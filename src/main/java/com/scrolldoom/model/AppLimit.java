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
@Document(collection = "appLimits")
@CompoundIndex(name = "userId_packageName", def = "{'userId': 1, 'packageName': 1}", unique = true)
public class AppLimit {

    @Id
    private ObjectId id;

    @Indexed
    private ObjectId userId;

    @Indexed
    private String packageName;

    private String appLabel;

    private int dailyLimitMinutes;

    private Date updatedAt;
}
