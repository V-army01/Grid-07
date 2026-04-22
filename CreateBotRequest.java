package com.grid07.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateBotRequest {
    @NotBlank(message = "name is required")
    private String name;
    private String personaDescription;
}