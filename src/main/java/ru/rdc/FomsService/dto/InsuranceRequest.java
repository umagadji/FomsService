package ru.rdc.FomsService.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
//DTO для запроса информации по страховой принадлежности для одного полиса
@JsonInclude(JsonInclude.Include.NON_NULL) // Исключаем null-поля из JSON
public class InsuranceRequest {
    private Integer requestId;
    private String fam;
    private String im;
    private String ot;
    private String dr;
    private Integer doctype;
    private String docser;
    private String docnum;
    private String enp;
    private Integer stype;

    // Переопределяем equals() и hashCode()
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InsuranceRequest that = (InsuranceRequest) o;
        return Objects.equals(fam, that.fam) &&
                Objects.equals(im, that.im) &&
                Objects.equals(ot, that.ot) &&
                Objects.equals(dr, that.dr) &&
                Objects.equals(enp, that.enp) &&
                Objects.equals(stype, that.stype);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fam, im, ot, dr, enp, stype);
    }

    @Override
    public String toString() {
        return "InsuranceRequest{" +
                "requestId=" + requestId +
                ", fam='" + fam + '\'' +
                ", im='" + im + '\'' +
                ", ot='" + ot + '\'' +
                ", dr='" + dr + '\'' +
                ", doctype=" + doctype +
                ", docser='" + docser + '\'' +
                ", docnum='" + docnum + '\'' +
                ", enp='" + enp + '\'' +
                ", stype=" + stype +
                '}';
    }
}
