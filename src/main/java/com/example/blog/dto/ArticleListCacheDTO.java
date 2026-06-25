package com.example.blog.dto;

import com.example.blog.vo.ArticleListVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleListCacheDTO {

    private List<ArticleListVO> records;
    private long total;
    private long page;
    private long size;
}
