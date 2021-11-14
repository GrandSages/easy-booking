package com.easybooking.reservation.test;

import com.easybooking.reservation.util.redis.RedisValue;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class TestOptimisticLock implements ApplicationRunner {
    private final RedisValue redisValue;
    private final String PRODUCT_KEY = "test_optimistic_lock_product";
    private final int PRODUCT_NUM = 100;
    private final List<Runnable> CUSTOMERS = new ArrayList<>();
    private final int CUSTOMER_NUM = 2500;
    private final ExecutorService SHOPPING_MART = Executors.newCachedThreadPool();
    private final List<String> SHOPPING_SUCCESS_LIST = new ArrayList<>();

    public TestOptimisticLock(RedisValue redisValue) {
        this.redisValue = redisValue;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
//        prepareProducts();
//        LetCustomersIn();
//        beginShopping();
//        endShopping();
    }

    private void endShopping() {
        System.out.println("搶購結束");
        System.out.println("=============================================");
        System.out.printf("一共有 %d 個顧客搶購成功，成功搶購商品的顧客: %n", SHOPPING_SUCCESS_LIST.size());
//        SHOPPING_SUCCESS_LIST.forEach(System.out::println);
        SHOPPING_MART.shutdown();
    }

    private void beginShopping() throws InterruptedException {
        System.out.println("開始搶購");
        var allShopping = new ArrayList<Future>();
        for (var customer : CUSTOMERS) {
            Future shopping = SHOPPING_MART.submit(customer);
            allShopping.add(shopping);
        }
        var keepShopping = true;
        while (keepShopping) {
            int num = 0;
            var anyOneNotDone = false;
            for (var shopping : allShopping) {
                if (!shopping.isDone()) {
                    anyOneNotDone = true;
                    break;
                } else {
                    num++;
                }
            }
            System.out.println("num: " + num);
            keepShopping = anyOneNotDone;
            Thread.sleep(1000);
        }
    }

    private void LetCustomersIn() {
        System.out.println("客人準備進場");
        for (int id = 0; id < CUSTOMER_NUM; id++)
            CUSTOMERS.add(new Customer(id));
    }

    private void prepareProducts() {
        System.out.println("開始準備商品");
        if (redisValue.has(PRODUCT_KEY))
            redisValue.del(PRODUCT_KEY);
        redisValue.set(PRODUCT_KEY, String.valueOf(PRODUCT_NUM));
    }

    private class Customer implements Runnable {
        private final String name;

        Customer(int id) {
            this.name = "顧客[" + id + "]";
        }

        @Override
        public void run() {
            goShopping(this);
        }

        private final SessionCallback<Object> shoppingCallback = new SessionCallback<>() {
            @Override
            public Object execute(RedisOperations redisOperations) throws DataAccessException {
                try {
                    redisOperations.watch(PRODUCT_KEY);
                    redisOperations.multi();
                    var opsVal = redisOperations.opsForValue();
                    opsVal.decrement(PRODUCT_KEY);
                    return redisOperations.exec();
                } catch (Exception e) {
                    redisOperations.discard();
                    return null;
                } finally {
                    redisOperations.unwatch();
                }
            }
        };

    }

    private void goShopping(Customer customer) {
        var keepShopping = true;
        while (keepShopping) {
            verifyData();
            keepShopping = shopping(customer);
        }
    }

    /**
     * implements optimistic lock here.
     * not reliable in high concurrency, will cause over sell.
     */
    private boolean shopping(Customer customer) {
//        System.out.println(customer.name + "開始搶購商品");
        try {
            var productNum = Integer.parseInt(redisValue.get(PRODUCT_KEY));
            if (productNum <= 0) {
                return false;
            }

            var result = redisValue.execute(customer.shoppingCallback);
            if (result == null || result.isEmpty()) {
//                System.out.println(customer.name + " 搶購失敗，再接再勵");
                return true;
            } else {
//                System.out.println(customer.name + " 搶購商品成功");
                SHOPPING_SUCCESS_LIST.add(customer.name);
                return false;
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    private static void verifyData() {
        try {
            Thread.sleep(new Random().nextInt(1000) + 2000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
