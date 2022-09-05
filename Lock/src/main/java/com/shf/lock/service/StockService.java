package com.shf.lock.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.shf.lock.lock.DistributedLockClient;
import com.shf.lock.lock.DistributedRedisLock;
import com.shf.lock.mapper.StockMapper;
import com.shf.lock.pojo.Stock;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;


import java.sql.Time;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS) // 多例模式
public class StockService {
//    private Stock stock = new Stock();

    @Autowired
    private DistributedLockClient distributedLockClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private RedissonClient redissonClient;

//    private ReentrantLock lock = new ReentrantLock();

    public void test() {
        RLock lock = redissonClient.getLock("lock");
        lock.lock();
        System.out.println("测试可重入锁");
        lock.unlock();
    }

    @SneakyThrows
    public void deduct() {
        RLock lock = this.redissonClient.getLock("lock");
        lock.lock();

        try {
//        1.查询库存信息
            String stock = redisTemplate.opsForValue().get("stock").toString();

            if (stock != null && stock.length() != 0) {
                Integer st = Integer.valueOf(stock);
                if (st > 0) {
                    redisTemplate.opsForValue().set("stock", String.valueOf(--st));
                }
            }

            test();
        } finally {
            lock.unlock();
        }
    }

    @SneakyThrows
    public void deduct5() {
        DistributedRedisLock redisLock = distributedLockClient.getRedisLock("lock");
        redisLock.lock();

        try {
//        1. 查询库存信息
            String stock = redisTemplate.opsForValue().get("stock").toString();

//        2.判断库存是否充足
            if (stock != null && stock.length() != 0) {
                Integer st = Integer.valueOf(stock);
                if (st > 0) {
                    redisTemplate.opsForValue().set("stock", String.valueOf(--st));
                }
            }

            TimeUnit.SECONDS.sleep(1000);
        } finally {
            redisLock.unlock();
        }
    }

    public void deduct4() {
        String uuid = UUID.randomUUID().toString();

//        加锁setnx
        while (!redisTemplate.opsForValue().setIfAbsent("lock", uuid, 3, TimeUnit.SECONDS)) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        try {
//        1.查询库存
            String stock = redisTemplate.opsForValue().get("stock").toString();

//        2.判断库存是否充足
            if (stock != null && stock.length() != 0) {
                Integer st = Integer.valueOf(stock);
                if (st > 0) {
                    //                3.扣减库存
                    redisTemplate.opsForValue().set("stock", String.valueOf(--st));
                }
            }
        } finally {
//            首先判断是否是自己的锁，再解锁
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] " +
                    "then " +
                    "   return redis.call('del', KEYS[1]) " +
                    "else " +
                    "   return 0 " +
                    "end";
            redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList("lock"), uuid);

            if (StringUtils.equals(redisTemplate.opsForValue().get("lock"), uuid)) {
                redisTemplate.delete("lock");
            }

        }

    }

    public void deduct3() {
//        watch
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {

//        1. 查询库存信息
                String stock = operations.opsForValue().get("stock").toString();

//        2. 判断库存是否充足
                if (stock != null && stock.length() != 0) {
                    Integer st = Integer.valueOf(stock);
                    if (st > 0) {
//                multi
                        operations.multi();

                        operations.opsForValue().set("stock", String.valueOf(--st));

//                exec
                        List<Object> exec = operations.exec();
//                        如果执行事务的返回结果为空，则代表减库存失败，重试
                        if (exec == null || exec.size() == 0) {
                            deduct();
                        }
                        return exec;
                    }
                }
                return null;
            }
        });

    }

    @Transactional
    public void deduct2() {
//        1. 查询库存信息并锁定库存信息
        List<Stock> stocks = stockMapper.queryStock("1001");
//        这里取出一个库存
        Stock stock = stocks.get(0);

//        2.判断库存是否充足
        if (stock != null && stock.getCount() > 0) {
//        3.扣减库存
            stock.setCount(stock.getCount() - 1);
            stockMapper.updateById(stock);
        }


    }


    //    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void deduct1() {
//        lock.lock();
        try {
//            Stock stock = stockMapper.selectOne(
//                    new QueryWrapper<Stock>()
//                            .eq("product_code", "1001")
//                            .eq("warehouse", "北京仓"));
//            if (stock.getCount() > 0) {
//                stock.setCount(stock.getCount() - 1);
//                stockMapper.updateById(stock);
////                System.out.println("库存余量：" + stock.getCount());
//            } else {
//                System.out.println("库存不足");
//            }

            stockMapper.updateStock("1001", "北京仓", 1);
        } finally {
//            lock.unlock();
        }


    }

    /**
     * 测试公平锁
     * @param id
     */
    public void testFairLock(Long id) {
        RLock fairLock = redissonClient.getFairLock("fairLock");
        fairLock.lock();

        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            fairLock.unlock();
        }
    }

    public void testReadLock() {
        RReadWriteLock rwLock = redissonClient.getReadWriteLock("rwLock");
        rwLock.readLock().lock(10,TimeUnit.SECONDS);

//        rwLock.readLock().unlock();
    }

    public void testWriteLock() {
        RReadWriteLock rwLock = redissonClient.getReadWriteLock("rwLock");
        rwLock.writeLock().lock(10, TimeUnit.SECONDS);

//        rwLock.writeLock().unlock();
    }

    @SneakyThrows
    public static void main(String[] args) {
        Semaphore semaphore = new Semaphore(3);

        for (int i = 0; i < 6; i++) {
            new Thread(()->{
                try {
                    semaphore.acquire();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                System.out.println(Thread.currentThread().getName()+"抢到车位");
                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println(Thread.currentThread().getName()+"车停了一回儿开走了");

                semaphore.release();
            },i+"号车").start();
        }
    }


    @SneakyThrows
    public void testSemaphore() {
        RSemaphore semaphore = redissonClient.getSemaphore("semaphore");

        semaphore.acquire();
        System.out.println("10010获取了资源，开始处理业务逻辑");
        TimeUnit.SECONDS.sleep(10+new Random().nextInt(10));
        System.out.println("10010处理完成业务逻辑，释放资源----------");

        semaphore.release();
    }

    @SneakyThrows
    public void testLatch() {
        RCountDownLatch cdl = redissonClient.getCountDownLatch("cdl");
        cdl.trySetCount(6);
        cdl.await();
    }

    public void testCountDown() {
        RCountDownLatch cdl = redissonClient.getCountDownLatch("cdl");
        cdl.countDown();
    }
}
