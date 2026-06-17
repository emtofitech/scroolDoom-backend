package com.scrolldoom.repository;

import com.scrolldoom.model.NotificationPreference;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationPreferenceRepository extends MongoRepository<NotificationPreference, ObjectId> {

    Optional<NotificationPreference> findByUserId(ObjectId userId);
}
