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
@Document(collection = "breachEvents")
public class BreachEvent {

    @Id
    private ObjectId id;

    @Indexed
    private ObjectId userId;

    private ObjectId partnershipId;

    private String packageName;

    private String appLabel;

    private int limitMinutes;

    private int actualMinutes;

    private boolean partnerNotified;

    @Indexed
    private Date breachedAt;
}
