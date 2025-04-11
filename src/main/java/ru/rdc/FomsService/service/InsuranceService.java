package ru.rdc.FomsService.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import ru.rdc.FomsService.dto.InsuranceRequest;
import ru.rdc.FomsService.storage.TokenStorage;

@Service
public class InsuranceService {
    private final WebClient webClient;
    private final TokenStorage tokenStorage;
    private final TokenRefreshService tokenRefreshService;
    private final AuthService authService;
    private final ObjectMapper objectMapper;

    @Value("${auth.email}")
    private String email;

    @Value("${auth.password}")
    private String password;

    public InsuranceService(WebClient.Builder webClientBuilder, TokenStorage tokenStorage,
                            TokenRefreshService tokenRefreshService, AuthService authService,
                            ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.baseUrl("https://81.24.84.134:5545").build();
        this.tokenStorage = tokenStorage;
        this.tokenRefreshService = tokenRefreshService;
        this.authService = authService;
        this.objectMapper = objectMapper;
    }

    public Mono<String> getSingleInsurance(InsuranceRequest request) {
        return ensureAuthenticated()
                .then(Mono.defer(() -> makeRequestWithToken(request)))
                .onErrorResume(WebClientResponseException.Unauthorized.class, e ->
                        refreshAndRetry(request)
                )
                .onErrorResume(e -> {
                    System.out.println("Ошибка при запросе: " + e.getMessage());
                    return Mono.error(e);
                });
    }

    private Mono<Void> ensureAuthenticated() {
        if (tokenStorage.getAccessToken() == null) {
            return authService.authenticate(email, password)
                    .onErrorResume(e -> {
                        System.out.println("Ошибка аутентификации: " + e.getMessage());
                        return Mono.error(e);
                    });
        }
        return Mono.empty();
    }

    private Mono<String> refreshAndRetry(InsuranceRequest request) {
        return tokenRefreshService.refreshAccessToken()
                .doOnNext(token -> {
                    System.out.println("Обновленный токен: " + token);
                    System.out.println("Обновленный refresh-токен: " + tokenStorage.getRefreshToken());
                })
                .flatMap(newToken -> makeRequestWithToken(request))
                .onErrorResume(e -> {
                    System.out.println("Ошибка при обновлении токена: " + e.getMessage());
                    tokenStorage.clearTokens();
                    return ensureAuthenticated()
                            .then(Mono.defer(() -> makeRequestWithToken(request)));
                });
    }

    private Mono<String> makeRequestWithToken(InsuranceRequest request) {
        String token = tokenStorage.getAccessToken();
        if (token == null) {
            return Mono.error(new RuntimeException("Access token отсутствует"));
        }

        System.out.println("Отправляем запрос с токеном: " + token);

        return webClient.post()
                .uri("/Insurance/Single")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatus.UNAUTHORIZED::equals, response -> {
                    System.out.println("Сервер вернул 401: Unauthorized. Возможно, токен истек.");
                    return response.createException().flatMap(Mono::error);
                })
                .bodyToMono(String.class)
                .doOnNext(response -> System.out.println("Успешный ответ: " + response))
                .doOnError(e -> System.out.println("Ошибка запроса: " + e.getMessage()));
    }
}