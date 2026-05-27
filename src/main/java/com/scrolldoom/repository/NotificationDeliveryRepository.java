package com.scrolldoom.repository;

import com.scrolldoom.model.NotificationDelivery;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface NotificationDeliveryRepository extends MongoRepository<NotificationDelivery, ObjectId> {

    List<NotificationDelivery> findByFirebaseUidOrderBySentAtDesc(String firebaseUid);

    long countByDelivered(boolean delivered);

    long countByOpened(boolean opened);

    long countByDeliveredAndOpened(boolean delivered, boolean opened);

    long countBySentAtBetween(Date start, Date end);

    long countByNotificationType(String type);
}
