package com.scrolldoom.repository;

import com.scrolldoom.model.AppUsageEvent;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface AppUsageEventRepository extends MongoRepository<AppUsageEvent, ObjectId> {

    List<AppUsageEvent> findByFirebaseUidAndTimestampBetween(String firebaseUid, Date start, Date end);

    List<AppUsageEvent> findByFirebaseUidOrderByTimestampDesc(String firebaseUid);

    long countByEventType(String eventType);

    long countByFirebaseUidAndEventType(String firebaseUid, String eventType);

    long countByTimestampBetween(Date start, Date end);
}
