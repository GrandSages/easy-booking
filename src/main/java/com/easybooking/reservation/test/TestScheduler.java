package com.easybooking.reservation.test;

import com.easybooking.reservation.util.LogUtil;
import com.easybooking.reservation.util.redis.RedisValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.stream.IntStream;

//@Service
//@EnableAsync
public class TestScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestScheduler.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm:ss");

    private final ScheduledThreadPoolExecutor threadPool;
    private final ConcurrentTaskScheduler scheduler;
    private final RedisValue redisValue;

    public TestScheduler(ScheduledThreadPoolExecutor threadPool, ConcurrentTaskScheduler scheduler, RedisValue redisValue) {
        this.threadPool = threadPool;
        this.redisValue = redisValue;
        System.out.println("init test");
        if (!(scheduler.getConcurrentExecutor() instanceof ScheduledThreadPoolExecutor)) {
            scheduler.setScheduledExecutor(threadPool);
        }
        this.scheduler = scheduler;
    }

    @Async("threadPool")
    @Scheduled(cron = "0/2 * * * * ?")
    public void schedule1() {
        System.out.println(showThreadName("schedule1"));
        var key = "Hello";
        if (!redisValue.has(key)) {
            redisValue.set(key, "0");
        } else {
            var num = redisValue.incr(key);
            System.out.println("after incr: " + num);
        }
    }

    @Scheduled(cron = "0/4 * * * * ?")
    public void schedule2() throws InterruptedException {
        System.out.println(showThreadName("schedule2"));
        Thread.sleep(3000);
        IntStream.range(0, 1).forEach( i -> {
            try {
                new ObjectMapper().readValue("aa", getClass());
            } catch (Exception e) {
                LogUtil.warn(LOGGER, e);
            }
        });
    }

    private String showThreadName(String name) {
        String time, threadName;
        time = LocalDateTime.now().format(formatter);
        threadName = Thread.currentThread().getName();
        ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) scheduler.getConcurrentExecutor();
        return String.format(
            "%s [%s] %s, schedule active thread %d, schedule pool size %d, async active thread %d",
            time, threadName, name, executor.getActiveCount(), executor.getPoolSize(), threadPool.getActiveCount()
        );
    }
}
