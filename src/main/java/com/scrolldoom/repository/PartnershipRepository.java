package com.scrolldoom.repository;

import com.scrolldoom.model.Partnership;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

@Repository
public interface PartnershipRepository extends MongoRepository<Partnership, ObjectId> {

    Optional<Partnership> findByInviteCode(String inviteCode);

    Optional<Partnership> findBySenderUserIdAndStatus(ObjectId userId, String status);

    Optional<Partnership> findByReceiverUserIdAndStatus(ObjectId userId, String status);

    @Query("{$or: [{'senderUserId': ?0}, {'receiverUserId': ?0}], 'status': 'active'}")
    Optional<Partnership> findActivePartnership(ObjectId userId);

    void deleteByStatusAndInviteExpiresAtBefore(String status, Date date);
}
