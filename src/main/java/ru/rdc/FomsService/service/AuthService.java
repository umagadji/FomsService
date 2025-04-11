package ru.rdc.FomsService.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.rdc.FomsService.dto.AuthResponse;
import ru.rdc.FomsService.storage.TokenStorage;

@Service
public class AuthService {
    private final WebClient webClient;
    private final TokenStorage tokenStorage;

    public AuthService(WebClient.Builder webClientBuilder, TokenStorage tokenStorage) {
        this.webClient = webClientBuilder.baseUrl("https://81.24.84.134:5545").build();
        this.tokenStorage = tokenStorage;
    }

    public Mono<Void> authenticate(String email, String password) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/Account/Login")
                        .queryParam("Email", email)
                        .queryParam("Password", password)
                        .build())
                .header(HttpHeaders.CONTENT_LENGTH, "0")
                .retrieve()
                .onStatus(httpStatus -> httpStatus.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(new RuntimeException(
                                        "Ошибка аутентификации: " + errorBody)))
                )
                .bodyToMono(AuthResponse.class)
                .doOnNext(response -> {
                    if (response.getAuthenticationToken() == null ||
                            response.getAuthenticationToken().isEmpty()) {
                        throw new RuntimeException("Неверный ответ сервера: токен отсутствует");
                    }
                    tokenStorage.setAccessToken(response.getAuthenticationToken());
                    tokenStorage.setRefreshToken(response.getRefreshToken());
                })
                .then();
    }
}