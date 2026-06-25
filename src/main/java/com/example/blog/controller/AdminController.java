package com.example.blog.controller;

import com.example.blog.common.PageResult;
import com.example.blog.common.Result;
import com.example.blog.dto.ArticleRequest;
import com.example.blog.dto.LoginRequest;
import com.example.blog.dto.LoginResponse;
import com.example.blog.service.ArticleService;
import com.example.blog.service.AuthService;
import com.example.blog.service.UploadService;
import com.example.blog.vo.ArticleListVO;
import com.example.blog.vo.ArticleStatsVO;
import com.example.blog.vo.ArticleVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AuthService authService;
    private final ArticleService articleService;
    private final UploadService uploadService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @GetMapping("/stats")
    public Result<ArticleStatsVO> stats() {
        return Result.success(articleService.getStats());
    }

    @GetMapping("/articles")
    public Result<PageResult<ArticleListVO>> listArticles(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(articleService.listAll(page, size));
    }

    @GetMapping("/articles/{id}")
    public Result<ArticleVO> getArticle(@PathVariable Long id) {
        return Result.success(articleService.getById(id));
    }

    @PostMapping("/articles")
    public Result<ArticleVO> createArticle(@Valid @RequestBody ArticleRequest request, Authentication auth) {
        return Result.success(articleService.create(request, 1L));
    }

    @PutMapping("/articles/{id}")
    public Result<ArticleVO> updateArticle(@PathVariable Long id, @Valid @RequestBody ArticleRequest request) {
        return Result.success(articleService.update(id, request));
    }

    @DeleteMapping("/articles/{id}")
    public Result<Void> deleteArticle(@PathVariable Long id) {
        articleService.delete(id);
        return Result.success();
    }

    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        String url = uploadService.upload(file);
        return Result.success(Map.of("url", url));
    }
}
