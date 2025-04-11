package ru.rdc.FomsService.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
//Класс описывающий данные из файла МЭК или из МИС
public class Item {
    private String s_com;
    private String spolis;
    private String npolis;
    private String fam;
    private String im;
    private String ot;
    private String birthDate;
    private LocalDate date_in;
    private LocalDate date_out;
    private String refreason;
    private String codeUsl;
    private double sum;
    private double sumv;
    private String ds1;
    private String nameMO;
    private String iddokt;
    private String nhistory;
    private String schet;
    private String idstrax;
    private String idserv;

    @Override
    public String toString() {
        return "Item{" +
                "s_com='" + s_com + '\'' +
                ", spolis='" + spolis + '\'' +
                ", npolis='" + npolis + '\'' +
                ", fam='" + fam + '\'' +
                ", im='" + im + '\'' +
                ", ot='" + ot + '\'' +
                ", birthDate='" + birthDate + '\'' +
                ", date_in=" + date_in +
                ", date_out=" + date_out +
                ", refreason='" + refreason + '\'' +
                ", codeUsl='" + codeUsl + '\'' +
                ", sum=" + sum +
                ", sumv=" + sumv +
                ", ds1='" + ds1 + '\'' +
                ", nameMO='" + nameMO + '\'' +
                ", iddokt='" + iddokt + '\'' +
                ", nhistory='" + nhistory + '\'' +
                ", schet='" + schet + '\'' +
                ", idstrax='" + idstrax + '\'' +
                ", idserv='" + idserv + '\'' +
                '}';
    }
}
