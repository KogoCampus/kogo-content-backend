package com.kogo.content.storage.repository;

import com.kogo.content.exception.DBAccessException;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import java.util.*;

public class DefaultMongoEntityRepository<T, ID> implements MongoEntityRepository<T, ID> {

    @Autowired
    private MongoTemplate mongoTemplate;

    private Class<T> type;

    @Override
    public Optional<T> findById(ID id) {
        try {
            System.out.println("testsetests");
            return Optional.of(mongoTemplate.findById(id, type));
        } catch (RuntimeException e) {
            throw new DBAccessException(e.getMessage());
        }
    }

    @Override
    public <S extends T> S save(S entity) {
        try {
            return mongoTemplate.save(entity);
        } catch (RuntimeException e) {
            throw new DBAccessException(e.getMessage());
        }
    }
}