package ru.rdc.FomsService.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
//DTO для запроса через refreshToken
public class RefreshTokenRequest {
    private String refreshToken;
}
