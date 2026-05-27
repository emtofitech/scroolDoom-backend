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
@Document(collection = "refresh_tokens")
public class RefreshToken {

    @Id
    private ObjectId id;

    @Indexed
    private String firebaseUid;

    @Indexed(unique = true)
    private String token;

    @Indexed(expireAfterSeconds = 0)
    private Date expiresAt;

    private Date createdAt;

    private boolean revoked;

    private String deviceInfo;

    private String ipAddress;
}
