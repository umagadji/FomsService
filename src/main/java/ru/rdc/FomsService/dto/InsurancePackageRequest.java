package ru.rdc.FomsService.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
//DTO для пакетного запроса.
public class InsurancePackageRequest {
    @JsonProperty("package")
    private List<InsuranceRequest> insuranceRequestList;

    @Override
    public String toString() {
        return "InsurancePackageRequest{" +
                "insuranceRequestList=" + insuranceRequestList +
                '}';
    }
}