package com.lektralabs.thrones.pallbearer.api.auth;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory brute-force protection for the login endpoints.
 *
 * <p>Tracks failed attempts per key (typically the normalized account identifier and the client
 * IP). After {@code maxAttempts} failures inside a rolling {@link #WINDOW_MS} window the key is
 * locked for {@link #LOCKOUT_MS}; further attempts are rejected with the remaining wait time so the
 * caller can surface a 429 + {@code Retry-After}. A successful login {@link #reset(String) clears}
 * the key.
 *
 * <p>State is process-local. That is sufficient for the current single-instance deployment and
 * fails safe (a restart simply forgives counters). If the service is ever horizontally scaled,
 * back this with a shared store (e.g. Redis) so limits hold across instances.
 */
@ApplicationScoped
public class LoginRateLimiter {

    /** Max failed attempts per account before lockout. */
    public static final int MAX_ATTEMPTS_PER_USER = 5;
    /** Max failed attempts per source IP before lockout (looser, to tolerate shared NAT). */
    public static final int MAX_ATTEMPTS_PER_IP = 20;

    /** Rolling window over which failures accumulate. */
    static final long WINDOW_MS = 15 * 60 * 1000L;
    /** How long a key stays locked once the threshold is hit. */
    static final long LOCKOUT_MS = 15 * 60 * 1000L;

    /** Guard against unbounded growth from random/garbage keys. */
    private static final int MAX_TRACKED_KEYS = 50_000;

    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    private static final class Counter {
        int failures;
        long windowStartMs;
        long lockedUntilMs;
    }

    long now() {
        return System.currentTimeMillis();
    }

    /**
     * @return seconds the caller must wait before retrying, or 0 if not currently locked.
     */
    public long retryAfterSeconds(String key) {
        Counter c = counters.get(key);
        if (c == null) {
            return 0;
        }
        synchronized (c) {
            long remaining = c.lockedUntilMs - now();
            return remaining > 0 ? (remaining + 999) / 1000 : 0;
        }
    }

    /** True if any of the supplied keys is currently locked. */
    public long retryAfterSeconds(String... keys) {
        long max = 0;
        for (String key : keys) {
            max = Math.max(max, retryAfterSeconds(key));
        }
        return max;
    }

    /**
     * Record a failed attempt for {@code key} and return the resulting lockout wait in seconds
     * (0 if the threshold has not yet been reached).
     */
    public long recordFailure(String key, int maxAttempts) {
        if (counters.size() > MAX_TRACKED_KEYS) {
            evictExpired();
        }
        Counter c = counters.computeIfAbsent(key, k -> new Counter());
        synchronized (c) {
            long now = now();
            if (c.windowStartMs == 0 || now - c.windowStartMs > WINDOW_MS) {
                c.windowStartMs = now;
                c.failures = 0;
                c.lockedUntilMs = 0;
            }
            c.failures++;
            if (c.failures >= maxAttempts) {
                c.lockedUntilMs = now + LOCKOUT_MS;
            }
            long remaining = c.lockedUntilMs - now;
            return remaining > 0 ? (remaining + 999) / 1000 : 0;
        }
    }

    /** Clear all counters for the supplied keys (call on successful authentication). */
    public void reset(String... keys) {
        for (String key : keys) {
            counters.remove(key);
        }
    }

    public static String userKey(String username) {
        return "user:" + (username == null ? "" : username.trim().toLowerCase());
    }

    public static String ipKey(String ip) {
        return "ip:" + (ip == null ? "unknown" : ip);
    }

    /**
     * Separate IP bucket for Google sign-in.
     *
     * <p>Google sign-in has to throttle on IP alone — the request carries only an opaque ID
     * token, so the account is unknown until the CRM replies. If those failures fed
     * {@link #ipKey(String)}, which {@code crm-login} and {@code coach-login} share, one client
     * hammering Google sign-in would lock out <em>every</em> login method for everyone behind
     * that NAT — a school or club on one public IP.
     */
    public static String googleIpKey(String ip) {
        return "google-ip:" + (ip == null ? "unknown" : ip);
    }

    private void evictExpired() {
        long now = now();
        counters.entrySet().removeIf(e -> {
            Counter c = e.getValue();
            synchronized (c) {
                return c.lockedUntilMs < now && now - c.windowStartMs > WINDOW_MS;
            }
        });
    }
}
