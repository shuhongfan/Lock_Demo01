package com.shf.lock.lock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;


public class DistributedRedisLock implements Lock {

    private String lockName;

    private String uuid;

    private long expire = 30;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public void lock() {
        tryLock();
    }

    public DistributedRedisLock(StringRedisTemplate redisTemplate,String lockName,String uuid) {
        this.redisTemplate = redisTemplate;
        this.lockName = lockName;
        this.uuid = uuid + ":" + Thread.currentThread().getId();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {

    }

    @Override
    public boolean tryLock() {
        try {
            return this.tryLock(-1L,TimeUnit.SECONDS);
        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
        }
        return false;
    }

    /**
     * 加锁方法
     * @param time the maximum time to wait for the lock
     * @param unit the time unit of the {@code time} argument
     * @return
     * @throws InterruptedException
     */
    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        String script = "if redis.call('exists', KEYS[1]) == 0 or redis.call('hexists', KEYS[1], ARGV[1]) == 1 " +
                "then " +
                "   redis.call('hincrby', KEYS[1], ARGV[1], 1) " +
                "   redis.call('expire', KEYS[1], ARGV[2]) " +
                "   return 1 " +
                "else " +
                "   return 0 " +
                "end";
        while (!redisTemplate.execute(
                new DefaultRedisScript<>(script, Boolean.class),
                Arrays.asList(lockName),
                uuid,
                String.valueOf(expire))) {
            Thread.sleep(50);
        }

        return false;
    }

    /**
     * 解锁方法
     */
    @Override
    public void unlock() {
        String script = "if redis.call('hexists', KEYS[1], ARGV[1]) == 0 " +
                "then " +
                "   return nil " +
                "elseif redis.call('hincrby', KEYS[1], ARGV[1], -1) == 0 " +
                "then " +
                "   return redis.call('del', KEYS[1]) " +
                "else " +
                "   return 0 " +
                "end";
        Long flag = this.redisTemplate.execute(
                new DefaultRedisScript<>(script, Long.class),
                Arrays.asList(lockName),
                uuid);
        if (flag == null){
            throw new IllegalMonitorStateException("this lock doesn't belong to you!");
        }
    }

    @Override
    public Condition newCondition() {
        return null;
    }

    private void renewExpire() {
        String script = "if redis.call('hexists', KEYS[1], ARGV[1]) == 1 " +
                "then " +
                "   return redis.call('expire', KEYS[1], ARGV[2]) " +
                "else " +
                "   return 0 " +
                "end";
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (redisTemplate.execute(
                        new DefaultRedisScript<>(script, Boolean.class),
                        Arrays.asList(lockName),
                        uuid,
                        String.valueOf(expire))) {
                 renewExpire();
                }
            }
        },expire*1000/3,expire*1000/3);
    }
}
