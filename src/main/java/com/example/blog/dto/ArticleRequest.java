package com.example.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题最多200字")
    private String title;

    private String slug;

    @NotBlank(message = "内容不能为空")
    private String content;

    @Size(max = 500, message = "摘要最多500字")
    private String excerpt;

    private String cover;

    private Integer status = 0;

    private List<String> tags;
}
