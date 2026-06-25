package com.example.blog.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleVO {

    private Long id;
    private String title;
    private String slug;
    private String content;
    private String excerpt;
    private String cover;
    private Integer status;
    private Integer viewCount;
    private Integer likeCount;
    private List<String> tags;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
