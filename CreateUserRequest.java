package com.grid07.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateUserRequest {
    @NotBlank(message = "username is required")
    private String username;

    private Boolean premium = false;
}