package com.example.blog.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.blog.common.Result;
import com.example.blog.entity.Tag;
import com.example.blog.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagMapper tagMapper;

    @GetMapping
    public Result<List<Tag>> list() {
        return Result.success(tagMapper.selectList(new LambdaQueryWrapper<Tag>().orderByAsc(Tag::getName)));
    }
}
