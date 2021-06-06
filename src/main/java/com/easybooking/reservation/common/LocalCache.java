package com.easybooking.reservation.common;

import org.springframework.stereotype.Service;

@Service
public class LocalCache implements WorkOnInit {
    @Override
    public void init() {
        System.out.println("this is LocalCache");
    }

}
