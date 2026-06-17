package com.scrolldoom.repository;

import com.scrolldoom.model.BlockedApp;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlockedAppRepository extends MongoRepository<BlockedApp, ObjectId> {

    List<BlockedApp> findByUserId(ObjectId userId);

    Optional<BlockedApp> findByUserIdAndPackageName(ObjectId userId, String packageName);

    void deleteByUserIdAndPackageName(ObjectId userId, String packageName);
}
