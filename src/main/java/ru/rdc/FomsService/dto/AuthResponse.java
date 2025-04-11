package ru.rdc.FomsService.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
//Ответ возвращаемый с сервера после авторизации
public class AuthResponse {
    @JsonProperty("authenticationToken")
    private String authenticationToken;

    @JsonProperty("refreshToken")
    private String refreshToken;
}
