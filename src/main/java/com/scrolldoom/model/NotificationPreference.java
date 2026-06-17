package com.scrolldoom.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notification_preferences")
public class NotificationPreference {

    @Id
    private ObjectId id;

    @Indexed(unique = true)
    private ObjectId userId;

    @Builder.Default
    private boolean breachAlerts = true;

    @Builder.Default
    private boolean streakBroken = true;

    @Builder.Default
    private boolean appLocked = true;
}
