package com.example.blog.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.pinyin.PinyinUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.blog.entity.Article;
import com.example.blog.mapper.ArticleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class SlugUtil {

    private final ArticleMapper articleMapper;

    public String generateSlug(String title, Long excludeId) {
        String base = toSlug(title);
        if (StrUtil.isBlank(base)) {
            base = "article";
        }
        String slug = base;
        int counter = 1;
        while (exists(slug, excludeId)) {
            slug = base + "-" + counter++;
        }
        return slug;
    }

    private boolean exists(String slug, Long excludeId) {
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>()
                .eq(Article::getSlug, slug);
        if (excludeId != null) {
            wrapper.ne(Article::getId, excludeId);
        }
        return articleMapper.selectCount(wrapper) > 0;
    }

    private String toSlug(String title) {
        String pinyin = PinyinUtil.getPinyin(title, "-");
        return pinyin.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}
