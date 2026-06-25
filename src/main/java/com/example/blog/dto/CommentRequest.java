package com.example.blog.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentRequest {

    @NotNull(message = "文章ID不能为空")
    private Long articleId;

    private Long parentId;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称最多50字")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论最多1000字")
    private String content;
}
