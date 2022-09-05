package com.shf.lock.lock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DistributedLockClient {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private String uuid;

    public DistributedLockClient() {
        this.uuid = UUID.randomUUID().toString();
    }

    public DistributedRedisLock getRedisLock(String lockName) {
        return new DistributedRedisLock(redisTemplate,lockName, uuid);
    }
}
