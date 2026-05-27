package com.scrolldoom.repository;

import com.scrolldoom.model.BreachEvent;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface BreachEventRepository extends MongoRepository<BreachEvent, ObjectId> {

    List<BreachEvent> findByUserIdOrderByBreachedAtDesc(ObjectId userId);

    List<BreachEvent> findByUserIdAndBreachTypeOrderByBreachedAtDesc(ObjectId userId, String breachType);

    boolean existsByUserIdAndPackageNameAndBreachedAtBetween(
            ObjectId userId, String packageName, Date start, Date end);

    boolean existsByUserIdAndBreachedAtBetween(
            ObjectId userId, Date start, Date end);

    boolean existsByUserIdAndStreakNameAndBreachedAtBetween(
            ObjectId userId, String streakName, Date start, Date end);

    long countByUserIdAndBreachType(ObjectId userId, String breachType);

    long countByBreachType(String breachType);

    long countByAcknowledged(boolean acknowledged);

    long countByBreachedAtBetween(Date start, Date end);
}
