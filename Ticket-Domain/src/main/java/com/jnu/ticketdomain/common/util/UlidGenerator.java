package com.jnu.ticketdomain.common.util;


import com.github.f4b6a3.ulid.UlidCreator;

public class UlidGenerator {
    public static String generateUlid() {
        return UlidCreator.getMonotonicUlid().toString();
    }
}
