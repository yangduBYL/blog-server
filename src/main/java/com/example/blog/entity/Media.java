package com.example.blog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@TableName("media")
@NoArgsConstructor
@AllArgsConstructor
public class Media {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String url;
    private String originalName;
    private Long size;
    private LocalDateTime createTime;
}
