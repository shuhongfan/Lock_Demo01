package com.shf.lock;

import lombok.SneakyThrows;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.util.concurrent.CountDownLatch;

public class ZKTest {
    @SneakyThrows
    public static void main(String[] args) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        ZooKeeper zooKeeper = null;
        try {
            zooKeeper = new ZooKeeper("192.168.120.20:2181", 30000, new Watcher() {
                @Override
                public void process(WatchedEvent watchedEvent) {
                    System.out.println("获取链接");
                    countDownLatch.countDown();
                }
            });
        } finally {
            zooKeeper.close();
        }

    }
}
