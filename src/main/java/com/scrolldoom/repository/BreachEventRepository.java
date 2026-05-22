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

    boolean existsByUserIdAndPackageNameAndBreachedAtBetween(
            ObjectId userId, String packageName, Date start, Date end);

    boolean existsByUserIdAndBreachedAtBetween(
            ObjectId userId, Date start, Date end);
}
