package com.project.chatApp.repository;

import com.project.chatApp.entity.UserEntity;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<UserEntity, ObjectId>  {
    Optional<UserEntity> findByUsername(String username);
    Optional<UserEntity> deleteByUsername(String username);
}
