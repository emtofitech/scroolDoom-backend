package com.scrolldoom.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "sessions")
public class Session {

    @Id
    private ObjectId id;

    @Indexed
    private String firebaseUid;

    private String deviceInfo;

    private String ipAddress;

    private String fcmToken;

    private boolean rememberMe;

    @Indexed(expireAfterSeconds = 0)
    private Date expiresAt;

    private Date createdAt;

    private Date lastActiveAt;

    private boolean revoked;
}
