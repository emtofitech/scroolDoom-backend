package com.scrolldoom.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response envelope")
public class ApiEnvelope<T> {

    @Schema(description = "Whether the request was successful", example = "true")
    private boolean success;

    @Schema(description = "Response data (present on success)")
    private T data;

    @Schema(description = "Error details (present on failure)")
    private ErrorDetail error;

    public static <T> ApiEnvelope<T> ok(T data) {
        return ApiEnvelope.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    public static <T> ApiEnvelope<T> error(int code, String message) {
        return ApiEnvelope.<T>builder()
                .success(false)
                .error(ErrorDetail.builder()
                        .code(code)
                        .message(message)
                        .build())
                .build();
    }
}
