package com.scrolldoom.repository;

import com.scrolldoom.model.Streak;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StreakRepository extends MongoRepository<Streak, ObjectId> {

    Optional<Streak> findByUserId(ObjectId userId);
}
