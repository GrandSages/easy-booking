package com.easybooking.reservation.test.lock;

import com.easybooking.reservation.util.redis.RedisValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TestLockBase {

    protected RedisValue redisValue;
    protected String PRODUCT_KEY = "test_optimistic_lock_product";
    protected int PRODUCT_NUM = 100;
    protected List<Runnable> CUSTOMERS = new ArrayList<>();
    protected int CUSTOMER_NUM = 2500;
    protected ExecutorService SHOPPING_MART = Executors.newCachedThreadPool();
    protected List<String> SHOPPING_SUCCESS_LIST = new ArrayList<>();

    public TestLockBase(RedisValue redisValue) {
        this.redisValue = redisValue;
    }

    protected void run() throws InterruptedException {
        prepareProducts();
        LetCustomersIn();
        beginShopping();
        endShopping();
    }

    protected void endShopping() {
        System.out.println("搶購結束");
        System.out.println("=============================================");
        System.out.printf("一共有 %d 個顧客搶購成功，成功搶購商品的顧客: %n", SHOPPING_SUCCESS_LIST.size());
        SHOPPING_MART.shutdown();
    }

    protected void beginShopping() throws InterruptedException {
        System.out.println("開始搶購");
        var allShopping = new ArrayList<Future>();
        for (var customer : CUSTOMERS) {
            Future shopping = SHOPPING_MART.submit(customer);
            allShopping.add(shopping);
        }
        var keepShopping = true;
        var waitingCount = 0;
        while (keepShopping) {
            var num = 0;
            var anyOneNotDone = false;
            for (var shopping : allShopping) {
                if (!shopping.isDone()) {
                    anyOneNotDone = true;
                    break;
                } else {
                    num++;
                }
            }
            keepShopping = anyOneNotDone;
            if (num != CUSTOMER_NUM)
                System.out.printf("num: %d\r", num);
            else
                System.out.printf("num: %d\n", num);
            Thread.sleep(100);
            waitingCount++;
        }
        System.out.println("Waiting count: " + waitingCount);
    }

    protected void LetCustomersIn() {
        System.out.println("客人準備進場");
        for (int id = 0; id < CUSTOMER_NUM; id++)
            CUSTOMERS.add(new Customer(id));
    }

    protected void prepareProducts() {
        System.out.println("開始準備商品");
        if (redisValue.has(PRODUCT_KEY))
            redisValue.del(PRODUCT_KEY);
        redisValue.set(PRODUCT_KEY, String.valueOf(PRODUCT_NUM));
    }

    protected class Customer implements Runnable {
        protected final String name;

        Customer(int id) {
            this.name = "顧客[" + id + "]";
        }

        @Override
        public void run() {
            goShopping(this);
        }

    }

    protected void goShopping(Customer customer) {
        throw new RuntimeException("Not Implemented Method");
    }

    protected static void verifyData() {
        try {
            Thread.sleep(new Random().nextInt(1000) + 2000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected boolean noMoreProducts(String productKey) {
        var productNum = Integer.parseInt(redisValue.get(productKey));
        return productNum <= 0;
    }

}
