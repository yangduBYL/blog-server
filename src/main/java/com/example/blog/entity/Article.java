package com.example.blog.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@TableName("article")
@NoArgsConstructor
@AllArgsConstructor
public class Article {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String slug;
    private String content;
    private String excerpt;
    private String cover;
    private Integer status;
    private Integer viewCount;
    private Integer likeCount;
    private Long authorId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
