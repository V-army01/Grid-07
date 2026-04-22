package com.grid07.dto;

import com.grid07.entity.Post;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreatePostRequest {
    @NotNull(message = "authorId is required")
    private Long authorId;

    @NotNull(message = "authorType is required (USER or BOT)")
    private Post.AuthorType authorType;

    @NotBlank(message = "content cannot be blank")
    private String content;
}