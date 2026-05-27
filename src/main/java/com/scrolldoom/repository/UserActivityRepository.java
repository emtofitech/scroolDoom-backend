package com.scrolldoom.repository;

import com.scrolldoom.model.UserActivity;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserActivityRepository extends MongoRepository<UserActivity, ObjectId> {

    Optional<UserActivity> findByFirebaseUid(String firebaseUid);

    List<UserActivity> findByOnlineTrue();

    long countByOnlineTrue();

    List<UserActivity> findByLastSeenAfter(Date date);

    long countByLastSeenAfter(Date date);
}
