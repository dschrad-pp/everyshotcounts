package com.lektralabs.thrones.pallbearer.security;

import io.quarkus.elytron.security.common.BcryptUtil;

import java.security.SecureRandom;
import java.util.Base64;

public class SecurityUtils {
    private static final SecureRandom random = new SecureRandom();

    private static int HASH_ITERATIONS = 10;

    public static String createSalt() {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hash(String credentials, String encodedSalt) {
        byte[] salt = Base64.getDecoder().decode(encodedSalt);
        return BcryptUtil.bcryptHash(credentials, HASH_ITERATIONS, salt);
    }

}