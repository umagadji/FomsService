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
//DTO для ответа при пакетном запросе
public class InsurancePackageResponse {
    @JsonProperty("response")
    private List<InsuranceResponse> responses;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (responses != null) {
            for (InsuranceResponse response : responses) {
                sb.append(response.toString()).append("\n");
            }
        }
        return sb.toString();
    }
}