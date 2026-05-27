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

    public static final String BREACH_SCREEN_TIME = "SCREEN_TIME_EXCEEDED";
    public static final String BREACH_STREAK = "STREAK_BROKEN";
    public static final String BREACH_BLOCKED_APP = "BLOCKED_APP_OPENED";

    @Id
    private ObjectId id;

    @Indexed
    private ObjectId userId;

    private ObjectId partnershipId;

    @Indexed
    private String breachType;

    private String packageName;

    private String appLabel;

    private int limitMinutes;

    private int actualMinutes;

    private String streakName;

    private int missedDays;

    private String severity;

    private boolean partnerNotified;

    private boolean acknowledged;

    @Indexed
    private Date breachedAt;

    private Date acknowledgedAt;
}
