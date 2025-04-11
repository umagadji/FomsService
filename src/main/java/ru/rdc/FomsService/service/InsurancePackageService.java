package ru.rdc.FomsService.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import ru.rdc.FomsService.dto.InsurancePackageRequest;
import ru.rdc.FomsService.dto.InsurancePackageResponse;
import ru.rdc.FomsService.storage.TokenStorage;

@Service
public class InsurancePackageService {
    private final WebClient webClient;
    private final TokenStorage tokenStorage;
    private final TokenRefreshService tokenRefreshService;
    private final AuthService authService;

    @Value("${auth.email}")
    private String email;

    @Value("${auth.password}")
    private String password;

    public InsurancePackageService(WebClient.Builder webClientBuilder,
                                   TokenStorage tokenStorage,
                                   TokenRefreshService tokenRefreshService,
                                   AuthService authService) {
        this.webClient = webClientBuilder.baseUrl("https://81.24.84.134:5545").build();
        this.tokenStorage = tokenStorage;
        this.tokenRefreshService = tokenRefreshService;
        this.authService = authService;
    }

    public Mono<InsurancePackageResponse> getPackageInsurance(InsurancePackageRequest request) {
        return ensureAuthenticated()
                .then(Mono.defer(() -> makePackageRequestWithToken(request)))
                .onErrorResume(WebClientResponseException.Unauthorized.class, e ->
                        refreshAndRetry(request)
                )
                .onErrorResume(e -> {
                    System.out.println("Ошибка при запросе: " + e.getMessage());
                    return Mono.just(new InsurancePackageResponse());
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

    private Mono<InsurancePackageResponse> refreshAndRetry(InsurancePackageRequest request) {
        return tokenRefreshService.refreshAccessToken()
                .flatMap(newToken -> makePackageRequestWithToken(request))
                .onErrorResume(e -> {
                    System.out.println("Ошибка при обновлении токена: " + e.getMessage());
                    tokenStorage.clearTokens();
                    return ensureAuthenticated()
                            .then(Mono.defer(() -> makePackageRequestWithToken(request)));
                });
    }

    private Mono<InsurancePackageResponse> makePackageRequestWithToken(InsurancePackageRequest request) {
        String token = tokenStorage.getAccessToken();
        if (token == null) {
            return Mono.error(new RuntimeException("Access token отсутствует"));
        }

        try {
            // Сериализация объекта в JSON для проверки
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonRequest = objectMapper.writeValueAsString(request);
            System.out.println("Отправляемый JSON: " + jsonRequest);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return webClient.post()
                .uri("/Insurance/Package")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatus.UNAUTHORIZED::equals, response -> {
                    System.out.println("Сервер вернул 401 Unauthorized");
                    return response.createException().flatMap(Mono::error);
                })
                .bodyToMono(InsurancePackageResponse.class)
                .defaultIfEmpty(new InsurancePackageResponse())
                .doOnError(e -> System.out.println("Ошибка запроса: " + e.getMessage()));
    }
}