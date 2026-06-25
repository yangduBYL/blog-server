package com.example.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.blog.entity.Comment;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    /** 物理删除文章下所有评论 */
    @Delete("DELETE FROM comment WHERE article_id = #{articleId}")
    void physicalDeleteByArticleId(@Param("articleId") Long articleId);
}
