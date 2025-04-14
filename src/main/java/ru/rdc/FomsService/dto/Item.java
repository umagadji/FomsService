package ru.rdc.FomsService.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Objects;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
//Класс описывающий данные из файла МЭК или из МИС
public class Item {
    private int requestId;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return Objects.equals(s_com, item.s_com) &&
                Objects.equals(spolis, item.spolis) &&
                Objects.equals(npolis, item.npolis) &&
                Objects.equals(fam, item.fam) &&
                Objects.equals(im, item.im) &&
                Objects.equals(ot, item.ot) &&
                Objects.equals(birthDate, item.birthDate) &&
                Objects.equals(date_in, item.date_in) &&
                Objects.equals(date_out, item.date_out) &&
                Objects.equals(refreason, item.refreason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                s_com, spolis, npolis, fam, im, ot, birthDate,
                date_in, date_out, refreason
        );
    }

    @Override
    public String toString() {
        return "Item{" +
                "requestId='" + requestId + '\'' +
                ", s_com='" + s_com + '\'' +
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
