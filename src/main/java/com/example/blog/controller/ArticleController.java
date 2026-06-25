package com.example.blog.controller;

import com.example.blog.common.PageResult;
import com.example.blog.common.Result;
import com.example.blog.service.ArticleService;
import com.example.blog.util.IpUtil;
import com.example.blog.vo.ArticleListVO;
import com.example.blog.vo.ArticleVO;
import com.example.blog.vo.LikeResultVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;
    private final IpUtil ipUtil;

    @GetMapping
    public Result<PageResult<ArticleListVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String tag) {
        return Result.success(articleService.listPublished(page, size, tag));
    }

    @GetMapping("/{slug}")
    public Result<ArticleVO> detail(@PathVariable String slug) {
        return Result.success(articleService.getBySlug(slug));
    }

    @GetMapping("/{id}/like/status")
    public Result<Boolean> likeStatus(@PathVariable Long id, HttpServletRequest request) {
        String clientId = ipUtil.getClientIp(request);
        return Result.success(articleService.hasLiked(id, clientId));
    }

    @PostMapping("/{id}/like")
    public Result<LikeResultVO> like(@PathVariable Long id, HttpServletRequest request) {
        String clientId = ipUtil.getClientIp(request);
        return Result.success(articleService.like(id, clientId));
    }

    @DeleteMapping("/{id}/like")
    public Result<LikeResultVO> unlike(@PathVariable Long id, HttpServletRequest request) {
        String clientId = ipUtil.getClientIp(request);
        return Result.success(articleService.unlike(id, clientId));
    }
}
