package com.stackroute.repository;

import com.stackroute.domain.DataModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.Map;

@Repository
public class DataRepositoryImpl implements DataRepository {

    private RedisTemplate<String, DataModel> redisTemplate;

    private HashOperations hashOperations;


    @Autowired
    public DataRepositoryImpl(RedisTemplate<String, DataModel> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    private void init(){
        hashOperations = redisTemplate.opsForHash();
    }

    @Override
    public void save(DataModel dataModel) {
        hashOperations.put("DataModel", dataModel.getKey(), dataModel);
    }

    @Override
    public Map<String, DataModel> findAll() {
        return hashOperations.entries("DataModel");
    }

//    @Override
//    public DataModel findByName(String name) {
//        return (DataModel)hashOperations.get("DataModel", name);
//    }
//
//    @Override
//    public void update(DataModel dataModel) {
//        save(dataModel);
//    }
//
//    @Override
//    public void delete(String name) {
//
//        hashOperations.delete("DataModel", name);
//    }
}
