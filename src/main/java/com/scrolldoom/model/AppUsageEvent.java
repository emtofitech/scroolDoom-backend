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
@Document(collection = "app_usage_events")
@CompoundIndex(def = "{'firebaseUid': 1, 'timestamp': -1}")
public class AppUsageEvent {

    @Id
    private ObjectId id;

    @Indexed
    private String firebaseUid;

    private String eventType;

    private String screenName;

    private String featureName;

    private long durationMs;

    private String deviceInfo;

    private String appVersion;

    @Indexed(expireAfterSeconds = 7776000)
    private Date timestamp;
}
