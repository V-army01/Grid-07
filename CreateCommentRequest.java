package com.grid07.dto;

import com.grid07.entity.Comment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCommentRequest {
    @NotNull(message = "authorId is required")
    private Long authorId;

    @NotNull(message = "authorType is required (USER or BOT)")
    private Comment.AuthorType authorType;

    @NotBlank(message = "content cannot be blank")
    private String content;
    private Long parentCommentId;
}