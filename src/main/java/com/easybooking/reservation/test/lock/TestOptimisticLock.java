package com.easybooking.reservation.test.lock;

import com.easybooking.reservation.util.redis.RedisValue;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

@Service
public class TestOptimisticLock extends TestLockBase implements ApplicationRunner {

    public TestOptimisticLock(RedisValue redisValue) {
        super(redisValue);
        this.CUSTOMER_NUM = 1000;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("開始測試樂觀所");
        super.run();
    }

    @Override
    public void goShopping(Customer customer) {
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
            if (noMoreProducts(PRODUCT_KEY)) {
                return false;
            }

            var result = redisValue.execute(ShoppingCallback);
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

    private final SessionCallback<Object> ShoppingCallback = new SessionCallback<>() {
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
