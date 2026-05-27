package com.scrolldoom.repository;

import com.scrolldoom.model.BreachEvent;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface BreachEventRepository extends MongoRepository<BreachEvent, ObjectId> {

    List<BreachEvent> findByUserIdOrderByBreachedAtDesc(ObjectId userId);

    List<BreachEvent> findByUserIdAndBreachTypeOrderByBreachedAtDesc(ObjectId userId, String breachType);

    Page<BreachEvent> findByUserIdOrderByBreachedAtDesc(ObjectId userId, Pageable pageable);

    Page<BreachEvent> findByUserIdAndBreachTypeOrderByBreachedAtDesc(ObjectId userId, String breachType, Pageable pageable);

    Page<BreachEvent> findByUserIdAndAcknowledgedOrderByBreachedAtDesc(ObjectId userId, boolean acknowledged, Pageable pageable);

    Page<BreachEvent> findByUserIdAndBreachTypeAndAcknowledgedOrderByBreachedAtDesc(ObjectId userId, String breachType, boolean acknowledged, Pageable pageable);

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
