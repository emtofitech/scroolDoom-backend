package com.scrolldoom.repository;

import com.scrolldoom.model.AppLimit;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppLimitRepository extends MongoRepository<AppLimit, ObjectId> {

    List<AppLimit> findByUserId(ObjectId userId);

    Optional<AppLimit> findByUserIdAndPackageName(ObjectId userId, String packageName);

    void deleteByUserIdAndPackageName(ObjectId userId, String packageName);
}
