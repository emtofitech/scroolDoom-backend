package com.scrolldoom.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {

    private String id;
    private String deviceInfo;
    private String ipAddress;
    private boolean rememberMe;
    private Date expiresAt;
    private Date createdAt;
    private Date lastActiveAt;
}
