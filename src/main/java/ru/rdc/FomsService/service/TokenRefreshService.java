package ru.rdc.FomsService.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.rdc.FomsService.dto.AuthResponse;
import ru.rdc.FomsService.dto.RefreshTokenRequest;
import ru.rdc.FomsService.storage.TokenStorage;

import java.util.Collections;

@Service
public class TokenRefreshService {
    private final WebClient webClient;
    private final TokenStorage tokenStorage;
    private final ObjectMapper objectMapper;

    public TokenRefreshService(WebClient.Builder webClientBuilder, TokenStorage tokenStorage,
                               ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.baseUrl("https://81.24.84.134:5545").build();
        this.tokenStorage = tokenStorage;
        this.objectMapper = objectMapper;
    }

    public Mono<String> refreshAccessToken() {
        String refreshToken = tokenStorage.getRefreshToken();
        System.out.println("Обновляем токен с refreshToken: " + refreshToken);

        if (refreshToken == null || refreshToken.isEmpty()) {
            return Mono.error(new RuntimeException("Refresh token отсутствует или пуст"));
        }

        return webClient.post()
                .uri("/Account/RefreshToken")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Collections.singletonMap("refreshToken", refreshToken))
                .retrieve()
                .onStatus(HttpStatus.UNAUTHORIZED::equals, response -> {
                    System.out.println("Сервер вернул 401 при обновлении токена. Refresh-токен, возможно, истек.");
                    return response.createException().flatMap(Mono::error);
                })
                .bodyToMono(AuthResponse.class)
                .flatMap(response -> {
                    System.out.println("Новый accessToken: " + response.getAuthenticationToken());
                    System.out.println("Новый refreshToken: " + response.getRefreshToken());

                    if (response.getAuthenticationToken() == null || response.getAuthenticationToken().isEmpty()) {
                        return Mono.error(new RuntimeException("Неверный ответ сервера: токен отсутствует"));
                    }
                    tokenStorage.setAccessToken(response.getAuthenticationToken());
                    if (response.getRefreshToken() != null && !response.getRefreshToken().isEmpty()) {
                        tokenStorage.setRefreshToken(response.getRefreshToken());
                    }
                    return Mono.just(response.getAuthenticationToken());
                });
    }
}