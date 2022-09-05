package com.shf.lock.controller;

import com.shf.lock.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StockController {

    @Autowired
    private StockService stockService;

    @GetMapping("stock/deduct")
    public String deduct() {
        stockService.deduct();
        return "success";
    }

    @GetMapping("test/fair/lock/{id}")
    public String testFireLock(@PathVariable("id") Long id) {
        stockService.testFairLock(id);
        return "hello test fair lock";
    }

    @GetMapping("test/read/lock")
    public String testReadLock() {
        stockService.testReadLock();
        return "hello testReadLock";
    }

    @GetMapping("test/write/lock")
    public String testWriteLock() {
        stockService.testWriteLock();
        return "hello testWriteLock";
    }

    @GetMapping("test/semaphore")
    public String testSemaphore() {
        stockService.testSemaphore();
        return "hello testSemaphore";
    }

    @GetMapping("test/testLatch")
    public String testLatch() {
        stockService.testLatch();
        return "班长关门";
    }

    @GetMapping("test/testCountDown")
    public String testCountDown() {
        stockService.testCountDown();
        return "出来了一位同学";
    }
}
