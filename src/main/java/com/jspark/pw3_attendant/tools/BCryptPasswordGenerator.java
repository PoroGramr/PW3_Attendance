package com.jspark.pw3_attendant.tools;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class BCryptPasswordGenerator {

    private BCryptPasswordGenerator() {
    }

    public static void main(String[] args) {
        String password = System.getenv("ADMIN_PASSWORD");
        if (password == null || password.length() < 8 || password.length() > 72) {
            throw new IllegalArgumentException("ADMIN_PASSWORD must contain between 8 and 72 characters");
        }
        System.out.println(new BCryptPasswordEncoder().encode(password));
    }
}
