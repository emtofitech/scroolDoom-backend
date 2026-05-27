package com.scrolldoom.repository;

import com.scrolldoom.model.RefreshToken;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends MongoRepository<RefreshToken, ObjectId> {

    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    Optional<RefreshToken> findByToken(String token);

    void deleteByExpiresAtBefore(Date now);

    void deleteByFirebaseUid(String firebaseUid);
}
