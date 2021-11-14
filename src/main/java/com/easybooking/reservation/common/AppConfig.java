package com.easybooking.reservation.common;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.*;

@Configuration
@EnableScheduling
@EnableAsync
public class AppConfig {

    @Qualifier("threadPool")
    private ScheduledThreadPoolExecutor executorService;

    @Qualifier("taskScheduler")
    private ConcurrentTaskScheduler taskScheduler;

    public AppConfig() {
        System.out.println("init AppConfig");
    }

    @Bean(name = "threadPool", destroyMethod = "shutdown")
    public ScheduledThreadPoolExecutor getScheduledThreadPoolExecutor() {
        System.out.println("init threadPool");
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            1, 10,
            10, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(10), (runnable) -> {
                Thread thread = new Thread(runnable);
                thread.setDaemon(true);
                thread.setName("async-" + thread.getId());
                return thread;
            }
        );

        return (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(
            1, (runnable) -> {
                Thread thread = new Thread(runnable);
                thread.setDaemon(true);
                thread.setName("async-" + thread.getId());
                return thread;
            }
        );
    }

//    @Bean(name = "threadPool", destroyMethod = "shutdown")
//    public ScheduledThreadPoolExecutor getScheduledThreadPoolExecutor() {
//        System.out.println("init threadPool");
//        return (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(
//            1, (runnable) -> {
//                Thread thread = new Thread(runnable);
//                thread.setDaemon(true);
//                thread.setName("async-" + thread.getId());
//                return thread;
//            }
//        );
//    }

    @Bean("taskScheduler")
    public ConcurrentTaskScheduler getThreadPoolTaskScheduler() {
        System.out.println("init taskScheduler");
        if (executorService == null) {
            executorService = getScheduledThreadPoolExecutor();
        }
        System.out.println("executorService = " + executorService);
        ConcurrentTaskScheduler scheduler = new ConcurrentTaskScheduler(executorService);
        scheduler.setErrorHandler(Throwable::printStackTrace);
        return scheduler;
    }

//    @Bean("taskScheduler")
//    public ThreadPoolTaskScheduler getThreadPoolTaskScheduler() {
//        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
//        scheduler.setErrorHandler(Throwable::printStackTrace);
//        scheduler.setPoolSize(3);
//        scheduler.setWaitForTasksToCompleteOnShutdown(true);
//        scheduler.setAwaitTerminationSeconds(1);
//        scheduler.setThreadNamePrefix("schedule-");
//
//        ConcurrentTaskScheduler scheduler1 = new ConcurrentTaskScheduler(Executors.newScheduledThreadPool(10));
//        int i = ((ScheduledThreadPoolExecutor) scheduler1.getConcurrentExecutor()).getActiveCount();
//        System.out.println("INIT: the active count = " + i);
//        return scheduler;
//    }

}
