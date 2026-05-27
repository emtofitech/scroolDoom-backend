package com.scrolldoom.repository;

import com.scrolldoom.model.Session;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends MongoRepository<Session, ObjectId> {

    List<Session> findByFirebaseUidAndRevokedFalseOrderByLastActiveAtDesc(String firebaseUid);

    Optional<Session> findByFirebaseUidAndIdAndRevokedFalse(String firebaseUid, ObjectId id);

    int countByFirebaseUidAndRevokedFalse(String firebaseUid);

    void deleteByExpiresAtBefore(Date now);
}
