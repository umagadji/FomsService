package ru.rdc.FomsService.storage;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
//Storage для хранения accessToken b refreshToken
public class TokenStorage {
    private final AtomicReference<String> accessToken = new AtomicReference<>();
    private final AtomicReference<String> refreshToken = new AtomicReference<>();

    public synchronized String getAccessToken() {
        return accessToken.get();
    }

    public synchronized void setAccessToken(String token) {
        this.accessToken.set(token);
    }

    public synchronized String getRefreshToken() {
        return refreshToken.get();
    }

    public synchronized void setRefreshToken(String token) {
        this.refreshToken.set(token);
    }

    public synchronized void clearTokens() {
        this.accessToken.set(null);
        this.refreshToken.set(null);
    }
}