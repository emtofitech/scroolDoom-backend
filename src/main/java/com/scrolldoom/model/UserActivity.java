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
@Document(collection = "user_activity")
public class UserActivity {

    @Id
    private ObjectId id;

    @Indexed(unique = true)
    private String firebaseUid;

    private boolean online;

    private String lastScreen;

    private String lastFeature;

    @Indexed(expireAfterSeconds = 7776000)
    private Date lastSeen;

    private Date lastAppOpen;

    private Date lastAppClose;

    private long totalSessionMs;

    private int sessionCount;

    private Date createdAt;

    private Date updatedAt;
}
