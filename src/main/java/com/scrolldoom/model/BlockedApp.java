package com.scrolldoom.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "blocked_apps")
@CompoundIndex(name = "userId_packageName", def = "{'userId': 1, 'packageName': 1}", unique = true)
public class BlockedApp {

    @Id
    private ObjectId id;

    @Indexed
    private ObjectId userId;

    private String packageName;

    private String appLabel;

    private Date blockedAt;

    private String blockedBy;

    private Date expiresAt;

    private int breachCount;

    private Date lastBreachAt;
}
