package com.easybooking.reservation.test.lock;

import com.easybooking.reservation.util.redis.RedisValue;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TestDistributedLock extends TestLockBase implements ApplicationRunner {
    private final int PRODUCT_CATEGORY_NUM = 5;
    private final String[] PRODUCT_KEYS = new String[PRODUCT_CATEGORY_NUM];
    private final String[] PRODUCT_LOCKS = new String[PRODUCT_CATEGORY_NUM];
    private final Map<Integer, List<String>> SHOPPING_SUCCESS_MAP = new ConcurrentHashMap<>();

    public TestDistributedLock(RedisValue redisValue) {
        super(redisValue);
        this.PRODUCT_KEY = "test_distributed_lock_product";
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("開始測試分散所");
        super.run();
    }

    @Override
    protected void endShopping() {
        System.out.println("搶購結束");
        System.out.println("=============================================");
        for (var eachMap : SHOPPING_SUCCESS_MAP.entrySet()) {
            var category = eachMap.getKey();
            var successList = eachMap.getValue();
            System.out.printf(
                "第 %d 商品一共有 %d 個顧客搶購成功: %n", category, successList.size()
            );
        }
        SHOPPING_MART.shutdown();
    }

    @Override
    protected void prepareProducts() {
        System.out.println("開始準備商品");
        // 準備多種商品
        for (int i = 0; i < PRODUCT_CATEGORY_NUM; i++) {
            String key = PRODUCT_KEY + "_" + i;
            PRODUCT_KEYS[i] = key;
            PRODUCT_LOCKS[i] = key + "_lock";
            if (redisValue.has(key))
                redisValue.del(key);
            redisValue.set(key, String.valueOf(PRODUCT_NUM));
        }
    }

    @Override
    protected void goShopping(Customer customer) {
        var keepShopping = true;
        while (keepShopping) {
            verifyData();
            keepShopping = shopping(customer);
        }
    }

    /**
     * implements distributed pessimistic lock here.
     * reliable in high concurrency.
     */
    private boolean shopping(Customer customer) {
//        System.out.println(customer.name + "開始搶購商品");
        var rand = (int) (Math.random() * PRODUCT_CATEGORY_NUM);
        var productLock = PRODUCT_LOCKS[rand];
        String lockId = null;
        try {

            var productKey = PRODUCT_KEYS[rand];
            if (noMoreProducts(productKey)) {
                return false;
            }

            lockId = redisValue.lock(productLock, 500);
            if (lockId == null) return true;

            redisValue.incr(productKey, -1);
            SHOPPING_SUCCESS_MAP.putIfAbsent(rand, new ArrayList<>());
            SHOPPING_SUCCESS_MAP.get(rand).add(customer.name);
            return false;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            redisValue.releaseLock(productLock, lockId);
            return false;
        } finally {
            redisValue.releaseLock(productLock, lockId);
        }
    }

}
