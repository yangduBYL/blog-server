package com.example.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.blog.common.BusinessException;
import com.example.blog.common.ErrorCode;
import com.example.blog.dto.CommentRequest;
import com.example.blog.entity.Article;
import com.example.blog.entity.Comment;
import com.example.blog.mapper.ArticleMapper;
import com.example.blog.mapper.CommentMapper;
import com.example.blog.util.IpUtil;
import com.example.blog.vo.CommentVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;
    private final ArticleMapper articleMapper;
    private final IpUtil ipUtil;

    public List<CommentVO> listByArticleId(Long articleId) {
        List<Comment> comments = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getArticleId, articleId)
                .orderByAsc(Comment::getCreateTime));
        Map<Long, List<CommentVO>> repliesMap = comments.stream()
                .filter(c -> c.getParentId() != null)
                .map(this::toVO)
                .collect(Collectors.groupingBy(CommentVO::getParentId));
        List<CommentVO> roots = new ArrayList<>();
        for (Comment comment : comments) {
            if (comment.getParentId() == null) {
                CommentVO vo = toVO(comment);
                vo.setReplies(repliesMap.getOrDefault(comment.getId(), List.of()));
                roots.add(vo);
            }
        }
        return roots;
    }

    public CommentVO create(CommentRequest request, HttpServletRequest httpRequest) {
        Article article = articleMapper.selectById(request.getArticleId());
        if (article == null || article.getStatus() != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        Comment comment = new Comment();
        comment.setArticleId(request.getArticleId());
        comment.setParentId(request.getParentId());
        comment.setNickname(request.getNickname());
        comment.setEmail(request.getEmail());
        comment.setContent(request.getContent());
        comment.setIp(ipUtil.getClientIp(httpRequest));
        comment.setUserAgent(httpRequest.getHeader("User-Agent"));
        commentMapper.insert(comment);
        return toVO(comment);
    }

    private CommentVO toVO(Comment comment) {
        return CommentVO.builder()
                .id(comment.getId())
                .articleId(comment.getArticleId())
                .parentId(comment.getParentId())
                .nickname(comment.getNickname())
                .content(comment.getContent())
                .createTime(comment.getCreateTime())
                .build();
    }
}
