package com.scrolldoom.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
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
@Document(collection = "notification_deliveries")
@CompoundIndex(def = "{'firebaseUid': 1, 'sentAt': -1}")
public class NotificationDelivery {

    @Id
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id;

    @Indexed
    private String firebaseUid;

    private String notificationType;

    private String title;

    private String body;

    private boolean delivered;

    private boolean opened;

    private String failureReason;

    @Indexed(expireAfterSeconds = 2592000)
    private Date sentAt;

    private Date deliveredAt;

    private Date openedAt;
}
