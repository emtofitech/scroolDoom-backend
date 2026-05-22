package com.scrolldoom.repository;

import com.scrolldoom.model.User;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, ObjectId> {

    Optional<User> findByFirebaseUid(String firebaseUid);

    Optional<User> findByEmail(String email);
}
