package com.scrolldoom.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenResponse {

    private String token;
    private String refreshToken;
    private String firebaseUid;
    private String email;
}
