package com.example.blog.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.blog.common.BusinessException;
import com.example.blog.common.ErrorCode;
import com.example.blog.common.PageResult;
import com.example.blog.dto.ArticleListCacheDTO;
import com.example.blog.dto.ArticleRequest;
import com.example.blog.entity.Article;
import com.example.blog.entity.Tag;
import com.example.blog.mapper.ArticleMapper;
import com.example.blog.mapper.ArticleTagMapper;
import com.example.blog.mapper.CommentMapper;
import com.example.blog.mapper.TagMapper;
import com.example.blog.util.SlugUtil;
import com.example.blog.vo.ArticleListVO;
import com.example.blog.vo.ArticleStatsVO;
import com.example.blog.vo.ArticleVO;
import com.example.blog.vo.LikeResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArticleService {

    private static final int STATUS_PUBLISHED = 1;
    private static final String VIEW_KEY_PREFIX = "article:view:";

    private final ArticleMapper articleMapper;
    private final TagMapper tagMapper;
    private final ArticleTagMapper articleTagMapper;
    private final CommentMapper commentMapper;
    private final SlugUtil slugUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ArticleCacheService articleCacheService;

    public PageResult<ArticleListVO> listPublished(int page, int size, String tag) {
        String cacheKey = articleCacheService.listKey(page, size, tag);
        ArticleListCacheDTO cached = articleCacheService.getOrLoad(
                cacheKey, ArticleListCacheDTO.class, () -> {
                    PageResult<ArticleListVO> loaded = loadPublished(page, size, tag);
                    return new ArticleListCacheDTO(loaded.getRecords(), loaded.getTotal(), loaded.getPage(), loaded.getSize());
                });
        return new PageResult<>(cached.getRecords(), cached.getTotal(), cached.getPage(), cached.getSize());
    }

    private PageResult<ArticleListVO> loadPublished(int page, int size, String tag) {
        Page<Article> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, STATUS_PUBLISHED)
                .orderByDesc(Article::getCreateTime);
        if (StrUtil.isNotBlank(tag)) {
            Tag tagEntity = tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getName, tag));
            if (tagEntity == null) {
                return new PageResult<>(List.of(), 0, page, size);
            }
            List<Long> articleIds = articleMapper.selectList(wrapper).stream()
                    .filter(a -> tagMapper.findNamesByArticleId(a.getId()).contains(tag))
                    .map(Article::getId)
                    .toList();
            if (articleIds.isEmpty()) {
                return new PageResult<>(List.of(), 0, page, size);
            }
            wrapper.in(Article::getId, articleIds);
        }
        Page<Article> result = articleMapper.selectPage(pageParam, wrapper);
        List<ArticleListVO> records = result.getRecords().stream()
                .map(this::toListVO)
                .collect(Collectors.toList());
        return new PageResult<>(records, result.getTotal(), page, size);
    }

    public PageResult<ArticleListVO> listAll(int page, int size) {
        Page<Article> pageParam = new Page<>(page, size);
        Page<Article> result = articleMapper.selectPage(pageParam,
                new LambdaQueryWrapper<Article>().orderByDesc(Article::getCreateTime));
        List<ArticleListVO> records = result.getRecords().stream()
                .map(this::toListVO)
                .collect(Collectors.toList());
        return new PageResult<>(records, result.getTotal(), page, size);
    }

    public ArticleStatsVO getStats() {
        Long total = articleCacheService.getOrLoad(
                articleCacheService.countKey(),
                Long.class,
                () -> articleMapper.selectCount(new LambdaQueryWrapper<Article>()));
        return ArticleStatsVO.builder().total(total == null ? 0 : total).build();
    }

    public ArticleVO getBySlug(String slug) {
        ArticleVO vo = articleCacheService.getOrLoad(
                articleCacheService.detailKey(slug),
                ArticleVO.class,
                () -> loadPublishedDetail(slug));
        if (vo != null) {
            incrementViewCount(vo.getId());
        }
        return vo;
    }

    private ArticleVO loadPublishedDetail(String slug) {
        Article article = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                .eq(Article::getSlug, slug)
                .eq(Article::getStatus, STATUS_PUBLISHED));
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return toDetailVO(article);
    }

    public ArticleVO getById(Long id) {
        return articleCacheService.getOrLoad(
                articleCacheService.adminDetailKey(id),
                ArticleVO.class,
                () -> {
                    Article article = articleMapper.selectById(id);
                    if (article == null) {
                        throw new BusinessException(ErrorCode.NOT_FOUND);
                    }
                    return toDetailVO(article);
                });
    }

    @Transactional(rollbackFor = Exception.class)
    public ArticleVO create(ArticleRequest request, Long authorId) {
        Article article = buildArticle(new Article(), request, authorId);
        articleMapper.insert(article);
        saveTags(article.getId(), request.getTags());
        clearCachesAfterMutation(null, article.getId());
        return toDetailVO(article);
    }

    @Transactional(rollbackFor = Exception.class)
    public ArticleVO update(Long id, ArticleRequest request) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        String oldSlug = article.getSlug();
        buildArticle(article, request, article.getAuthorId());
        articleMapper.updateById(article);
        articleTagMapper.deleteByArticleId(id);
        saveTags(id, request.getTags());
        clearCachesAfterMutation(oldSlug, id);
        if (!oldSlug.equals(article.getSlug())) {
            articleCacheService.evictDetail(article.getSlug());
        }
        articleCacheService.put(articleCacheService.adminDetailKey(id), toDetailVO(article));
        return toDetailVO(article);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        String slug = article.getSlug();
        articleTagMapper.deleteByArticleId(id);
        commentMapper.physicalDeleteByArticleId(id);
        articleMapper.physicalDeleteById(id);
        redisTemplate.delete("article:like:" + id);
        redisTemplate.delete("article:view:" + id);
        clearCachesAfterMutation(slug, id);
    }

    @Transactional(rollbackFor = Exception.class)
    public LikeResultVO like(Long articleId, String clientId) {
        String key = "article:like:" + articleId;
        Long added = redisTemplate.opsForSet().add(key, clientId);
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (added != null && added > 0) {
            article.setLikeCount(article.getLikeCount() + 1);
            articleMapper.updateById(article);
            refreshLikeCountInCache(article);
            return LikeResultVO.builder()
                    .liked(true)
                    .likeCount(article.getLikeCount())
                    .message("点赞成功")
                    .build();
        }
        return LikeResultVO.builder()
                .liked(true)
                .likeCount(article.getLikeCount())
                .message("您已点赞")
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public LikeResultVO unlike(Long articleId, String clientId) {
        String key = "article:like:" + articleId;
        Long removed = redisTemplate.opsForSet().remove(key, clientId);
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        if (removed != null && removed > 0) {
            article.setLikeCount(Math.max(0, article.getLikeCount() - 1));
            articleMapper.updateById(article);
            refreshLikeCountInCache(article);
            return LikeResultVO.builder()
                    .liked(false)
                    .likeCount(article.getLikeCount())
                    .message("已取消点赞")
                    .build();
        }
        return LikeResultVO.builder()
                .liked(false)
                .likeCount(article.getLikeCount())
                .message("您尚未点赞")
                .build();
    }

    public boolean hasLiked(Long articleId, String clientId) {
        String key = "article:like:" + articleId;
        Boolean member = redisTemplate.opsForSet().isMember(key, clientId);
        return Boolean.TRUE.equals(member);
    }

    private void refreshLikeCountInCache(Article article) {
        articleCacheService.evictDetail(article.getSlug());
        articleCacheService.evictListCaches();
        ArticleVO vo = toDetailVO(article);
        articleCacheService.put(articleCacheService.adminDetailKey(article.getId()), vo);
    }

    private void clearCachesAfterMutation(String slug, Long id) {
        articleCacheService.evictAllArticleCaches(slug, id);
    }

    private void incrementViewCount(Long articleId) {
        redisTemplate.opsForValue().increment(VIEW_KEY_PREFIX + articleId);
    }

    private Article buildArticle(Article article, ArticleRequest request, Long authorId) {
        article.setTitle(request.getTitle());
        article.setContent(request.getContent());
        article.setExcerpt(StrUtil.blankToDefault(request.getExcerpt(),
                StrUtil.sub(request.getContent().replaceAll("<[^>]+>", ""), 0, 200)));
        article.setCover(request.getCover());
        article.setStatus(request.getStatus());
        article.setAuthorId(authorId);
        if (StrUtil.isBlank(request.getSlug())) {
            article.setSlug(slugUtil.generateSlug(request.getTitle(), article.getId()));
        } else {
            article.setSlug(slugUtil.generateSlug(request.getSlug(), article.getId()));
        }
        return article;
    }

    private void saveTags(Long articleId, List<String> tags) {
        if (tags == null) {
            return;
        }
        for (String tagName : tags) {
            if (StrUtil.isBlank(tagName)) {
                continue;
            }
            Tag tag = tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getName, tagName));
            if (tag == null) {
                tag = new Tag();
                tag.setName(tagName);
                tagMapper.insert(tag);
            }
            articleTagMapper.insert(articleId, tag.getId());
        }
    }

    private ArticleListVO toListVO(Article article) {
        return ArticleListVO.builder()
                .id(article.getId())
                .title(article.getTitle())
                .slug(article.getSlug())
                .excerpt(article.getExcerpt())
                .cover(article.getCover())
                .viewCount(article.getViewCount())
                .likeCount(article.getLikeCount())
                .tags(tagMapper.findNamesByArticleId(article.getId()))
                .createTime(article.getCreateTime())
                .build();
    }

    private ArticleVO toDetailVO(Article article) {
        return ArticleVO.builder()
                .id(article.getId())
                .title(article.getTitle())
                .slug(article.getSlug())
                .content(article.getContent())
                .excerpt(article.getExcerpt())
                .cover(article.getCover())
                .status(article.getStatus())
                .viewCount(article.getViewCount())
                .likeCount(article.getLikeCount())
                .tags(tagMapper.findNamesByArticleId(article.getId()))
                .createTime(article.getCreateTime())
                .updateTime(article.getUpdateTime())
                .build();
    }
}
