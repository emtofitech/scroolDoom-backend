package com.scrolldoom.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "streaks")
public class Streak {

    @Id
    private ObjectId id;

    @Indexed(unique = true)
    private ObjectId userId;

    private int currentStreak;

    private int longestStreak;

    private LocalDate lastSuccessDate;

    private Date updatedAt;
}
