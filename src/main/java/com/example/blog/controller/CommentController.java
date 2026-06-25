package com.example.blog.controller;

import com.example.blog.annotation.RateLimit;
import com.example.blog.common.Result;
import com.example.blog.dto.CommentRequest;
import com.example.blog.service.CommentService;
import com.example.blog.vo.CommentVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public Result<List<CommentVO>> list(@RequestParam Long articleId) {
        return Result.success(commentService.listByArticleId(articleId));
    }

    @PostMapping
    @RateLimit(seconds = 60, maxRequests = 1, keyPrefix = "comment")
    public Result<CommentVO> create(@Valid @RequestBody CommentRequest request, HttpServletRequest httpRequest) {
        return Result.success(commentService.create(request, httpRequest));
    }
}
