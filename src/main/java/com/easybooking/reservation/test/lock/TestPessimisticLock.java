package com.easybooking.reservation.test.lock;

import com.easybooking.reservation.util.redis.RedisValue;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TestPessimisticLock extends TestLockBase implements ApplicationRunner {
    private final String PRODUCT_LOCK;

    public TestPessimisticLock(RedisValue redisValue) {
        super(redisValue);
        this.PRODUCT_KEY = "test_pessimistic_lock_product";
        PRODUCT_LOCK = PRODUCT_KEY + "_lock";
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("開始測試悲觀所");
        super.run();
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
     * implements pessimistic lock here.
     * reliable in high concurrency.
     */
    private boolean shopping(Customer customer) {
//        System.out.println(customer.name + "開始搶購商品");
        String lockId = null;
        try {
            if (noMoreProducts(PRODUCT_KEY)) {
                return false;
            }

            lockId = redisValue.lock(PRODUCT_LOCK, 500);
            if (lockId == null) return true;

            redisValue.incr(PRODUCT_KEY, -1);
            SHOPPING_SUCCESS_LIST.add(customer.name);
            return false;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            redisValue.releaseLock(PRODUCT_LOCK, lockId);
            return false;
        } finally {
            redisValue.releaseLock(PRODUCT_LOCK, lockId);
        }
    }

}
