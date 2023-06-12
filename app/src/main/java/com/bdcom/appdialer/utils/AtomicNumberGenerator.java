package com.bdcom.appdialer.utils;

import java.util.concurrent.atomic.AtomicInteger;

/** Created by ramananda on 5/9/17. */
public class AtomicNumberGenerator {
    private static final AtomicInteger c = new AtomicInteger(1010);

    public static int getUniqueNumber() {
        return c.incrementAndGet();
    }
}
