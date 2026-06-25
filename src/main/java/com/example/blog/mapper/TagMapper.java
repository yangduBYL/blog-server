package com.example.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.blog.entity.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    @Select("SELECT t.* FROM tag t INNER JOIN article_tag at ON t.id = at.tag_id WHERE at.article_id = #{articleId}")
    List<Tag> findByArticleId(@Param("articleId") Long articleId);

    @Select("SELECT t.name FROM tag t INNER JOIN article_tag at ON t.id = at.tag_id WHERE at.article_id = #{articleId}")
    List<String> findNamesByArticleId(@Param("articleId") Long articleId);
}
