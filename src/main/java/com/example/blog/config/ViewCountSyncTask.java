package com.example.blog.config;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.blog.entity.Article;
import com.example.blog.mapper.ArticleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ViewCountSyncTask {

    private static final String VIEW_KEY_PREFIX = "article:view:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ArticleMapper articleMapper;

    @Scheduled(fixedRate = 300000)
    public void syncViewCounts() {
        Set<String> keys = redisTemplate.keys(VIEW_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return;
        }
        for (String key : keys) {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                continue;
            }
            long increment = Long.parseLong(value.toString());
            if (increment <= 0) {
                continue;
            }
            String idStr = key.replace(VIEW_KEY_PREFIX, "");
            Long articleId = Long.parseLong(idStr);
            Article article = articleMapper.selectById(articleId);
            if (article != null) {
                article.setViewCount(article.getViewCount() + (int) increment);
                articleMapper.updateById(article);
                redisTemplate.delete(key);
                log.debug("Synced {} views for article {}", increment, articleId);
            }
        }
    }
}
