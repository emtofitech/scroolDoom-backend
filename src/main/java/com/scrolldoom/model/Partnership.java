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
@Document(collection = "partnerships")
public class Partnership {

    @Id
    private ObjectId id;

    private ObjectId senderUserId;

    private ObjectId receiverUserId;

    private String status;

    @Indexed(unique = true)
    private String inviteCode;

    private Date inviteExpiresAt;

    private Date createdAt;

    private Date acceptedAt;
}
