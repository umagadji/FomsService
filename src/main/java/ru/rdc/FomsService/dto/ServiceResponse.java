package ru.rdc.FomsService.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
//Ответ от сервера, который содержит одну или несколько записей о пациентах
public class ServiceResponse {
    @JsonProperty("response")
    private List<InsuranceResponse> response;
}
