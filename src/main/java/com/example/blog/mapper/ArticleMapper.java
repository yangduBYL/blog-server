package com.example.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.blog.entity.Article;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    /** 物理删除，绕过逻辑删除 */
    @Delete("DELETE FROM article WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);
}
