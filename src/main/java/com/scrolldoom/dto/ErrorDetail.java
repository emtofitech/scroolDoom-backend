package com.scrolldoom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Error details included in failed API responses")
public class ErrorDetail {

    @Schema(description = "HTTP status code", example = "404")
    private int code;

    @Schema(description = "Error message", example = "User not found")
    private String message;

    @Schema(description = "Field-level validation errors (present on 400)")
    private Map<String, String> fields;
}
