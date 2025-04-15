package ru.rdc.FomsService.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
//Ответ от сервера с данными о пациенте
public class InsuranceResponse {
    private int id;
    private int requestId;
    private String fam; // Фамилия
    private String im;  // Имя
    private String ot;  // Отчество
    private String dr;  // Дата рождения
    private int w; // Пол (1 - муж, 2 - жен)
    private int doctype; // Тип документа
    private String docser; // Серия документа
    private String docnum; // Номер документа
    private String docorg; // Кем выдан документ
    private String docdate; // Дата выдачи документа
    private String snils;
    private String datE_BEGIN;
    private String datE_END;
    private boolean active;
    private int vpolis;
    private String spolis;
    private String npolis;
    private String enp;
    private String smo;
    private String namsmok;
    private String reenom;
    private String adres;
    @JsonProperty("sS_VR")
    private String sS_VR;
    private String datE_P;
    private String terst;
    private String name;
    private int correct;
    private String source;

    // Форматтер для вывода дат
    private static final DateTimeFormatter OUTPUT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @Override
    public String toString() {
        return String.format(
                "Фамилия: %s\n" +
                        "Имя: %s\n" +
                        "Отчество: %s\n" +
                        "Дата рождения: %s\n" +
                        "Пол: %s\n" +
                        "Тип документа: %d\n" +
                        "Документ: %s №%s\n" +
                        "Кем выдан: %s\n" +
                        "Дата выдачи: %s\n" +
                        "СНИЛС: %s\n" +
                        "Дата начала: %s\n" +
                        "Дата окончания: %s\n" +
                        "Активен: %b\n" +
                        "Тип полиса: %d\n" +
                        "Серия полиса: %s\n" +
                        "Номер полиса: %s\n" +
                        "ЕНП: %s\n" +
                        "Код СМО: %s\n" +
                        "Страховая компания: %s\n" +
                        "Реестровый номер: %s\n" +
                        "Адрес: %s\n" +
                        "Дата П: %s\n" +
                        "Территория: %s\n" +
                        "Организация: %s\n" +
                        "Корректность: %d\n" +
                        "Источник: %s\n",
                fam, im, ot, formatDate(dr), (w == 1 ? "Мужской" : "Женский"),
                doctype, docser, docnum, docorg, formatDate(docdate),
                snils, formatDate(datE_BEGIN), formatDate(datE_END), active, vpolis,
                spolis, npolis, enp, smo, namsmok,
                reenom, adres, formatDate(datE_P), terst, name, correct, source
        );
    }

    // Метод форматирования дат
    private String formatDate(String dateStr) {
        if (dateStr == null || /*dateStr.equals("0001-01-01") || */dateStr.trim().isEmpty()) {
            return "Не указано"; // Заменяем пустые и невалидные даты
        }
        try {
            LocalDate date = LocalDate.parse(dateStr);
            return date.format(OUTPUT_DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return dateStr; // Если дата в неожиданном формате, оставляем как есть
        }
    }

}
