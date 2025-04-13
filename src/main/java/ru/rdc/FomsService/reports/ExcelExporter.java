package ru.rdc.FomsService.reports;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;
import ru.rdc.FomsService.dto.InsuranceResponse;
import ru.rdc.FomsService.dto.Item;

import java.io.IOException;

@Component
public class ExcelExporter {

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ENGLISH);

    public static void saveToExcel(List<InsuranceResponse> responses, List<Item> items, File file) {
        if (responses == null || responses.isEmpty()) {
            throw new IllegalArgumentException("Нет данных для сохранения!");
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Ответы сервера");

            // Обновленные заголовки
            String[] headers = {
                    "Комментарий", "Причина", "Исходная фамилия", "Исходное имя", "Исходное отчество",
                    "Исходная дата рождения", "Исходное МО", "Фамилия", "Имя", "Отчество",
                    "Дата рождения", "Пол", "Тип полиса", "Серия", "Полис", "ЕНП",
                    "Дата начала", "Дата окончания", "СМО", "Название СМО", "Реестровый номер",
                    "Тип ДУЛ", "Серия ДУЛ", "Номер ДУЛ", "Организация", "Дата выдачи", "СНИЛС",
                    "Активный", "Адрес", "Дата П", "Территория", "Организация", "Корректность", "Источник","Исходный полис"
            };

            // Создание заголовков
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(getHeaderCellStyle(workbook));
            }

            // Создаем Map для быстрого поиска Item по npolis
            Map<String, Item> itemMap = new HashMap<>();
            for (Item item : items) {
                itemMap.put(normalize(item.getNpolis()), item);
            }

            // Создаем стиль для строк с расхождениями
            CellStyle highlightStyle = getHighlightRowStyle(workbook);
            CellStyle greenStyle = getGreenRowStyle(workbook);

            int rowNum = 1;
            for (InsuranceResponse response : responses) {
                Row row = sheet.createRow(rowNum++);

                // Получение соответствующего Item
                String key = response.getSourceNpolis() != null ? normalize(response.getSourceNpolis()) : "";
                if (key.isEmpty() && response.getEnp() != null) {
                    key = normalize(response.getEnp());
                }
                Item item = itemMap.get(key);

                // Получение соответствующего Item
                //Item item = itemMap.get(normalize(response.getSourceNpolis()));
                if (item == null) {
                    System.out.println("⚠️ Не найден item по SourceNpolis: '" + response.getSourceNpolis() + "'");
                    System.out.println("🔍 Доступные ключи itemMap: " + itemMap.keySet());
                }

                row.createCell(0).setCellValue(item != null ? item.getS_com() : ""); // Ошибка

                // Проверка на расхождения
                boolean hasDifference = false;
                boolean isInactive = !response.isActive();
                StringBuilder errorDetails = new StringBuilder();

                // Если item не найден или полисы не совпадают
                if (item == null || !Objects.equals(item.getNpolis(), response.getEnp())) {
                    appendError(errorDetails, "Полисы в запросе и ответе не совпадают");
                    hasDifference = true;
                }

                if (isInactive) {
                    errorDetails.append("Полис неактивный");
                }

                if (item != null) {  // Проверяем, что оба объекта не null
                    // Фамилия
                    if (item.getFam() != null && response.getFam() != null) {
                        if (!Objects.equals(item.getFam().toUpperCase().trim(), response.getFam().toUpperCase().trim())) {
                            appendError(errorDetails, "Фамилия");
                            hasDifference = true;
                        }
                    } else if (item.getFam() != response.getFam()) {  // Если одно из значений null
                        appendError(errorDetails, "Фамилия");
                        hasDifference = true;
                    }

                    // Имя
                    if (item.getIm() != null && response.getIm() != null) {
                        if (!Objects.equals(item.getIm().toUpperCase().trim(), response.getIm().toUpperCase().trim())) {
                            appendError(errorDetails, "Имя");
                            hasDifference = true;
                        }
                    } else if (item.getIm() != response.getIm()) {  // Если одно из значений null
                        appendError(errorDetails, "Имя");
                        hasDifference = true;
                    }

                    // Отчество
                    if (item.getOt() != null && response.getOt() != null) {
                        if (!Objects.equals(item.getOt().toUpperCase().trim(), response.getOt().toUpperCase().trim())) {
                            appendError(errorDetails, "Отчество");
                            hasDifference = true;
                        }
                    } else if (item.getOt() != response.getOt()) {  // Если одно из значений null
                        appendError(errorDetails, "Отчество");
                        hasDifference = true;
                    }

                    // Дата рождения
                    if (item.getBirthDate() != null && response.getDr() != null) {
                        try {
                            LocalDate itemBirthDate = LocalDate.parse(item.getBirthDate(), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                            LocalDate responseBirthDate = LocalDate.parse(response.getDr(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                            if (!Objects.equals(itemBirthDate, responseBirthDate)) {
                                appendError(errorDetails, "Дата рождения");
                                hasDifference = true;
                            }
                        } catch (DateTimeParseException e) {
                            appendError(errorDetails, "Некорректный формат даты рождения");
                            hasDifference = true;
                        }
                    } else if (item.getBirthDate() != response.getDr()) {  // Если одно из значений null
                        appendError(errorDetails, "Дата рождения");
                        hasDifference = true;
                    }
                }

                // Заполнение данных в Excel
                row.createCell(1).setCellValue(errorDetails.toString()); // Ошибка своя
                row.createCell(2).setCellValue(item != null ? item.getFam().toUpperCase() : ""); // Исходная фамилия
                row.createCell(3).setCellValue(item != null ? item.getIm().toUpperCase() : ""); // Исходное имя
                row.createCell(4).setCellValue(item != null && item.getOt() != null ? item.getOt().toUpperCase() : ""); // Исходное отчество
                row.createCell(4).setCellValue(""); // Исходное отчество
                row.createCell(5).setCellValue(item != null ? item.getBirthDate() : ""); // Исходная дата рождения
                row.createCell(6).setCellValue(item != null ? item.getNameMO() : ""); // Исходное МО
                row.createCell(7).setCellValue(response.getFam()); // Фамилия
                row.createCell(8).setCellValue(response.getIm()); // Имя
                row.createCell(9).setCellValue(response.getOt()); // Отчество
                row.createCell(10).setCellValue(formatDate(response.getDr())); // Дата рождения
                row.createCell(11).setCellValue(response.getW() == 1 ? "Мужской" : "Женский"); // Пол
                row.createCell(12).setCellValue(response.getVpolis()); // Тип полиса
                row.createCell(13).setCellValue(response.getSpolis()); // Серия
                row.createCell(14).setCellValue(response.getNpolis()); // Полис
                row.createCell(15).setCellValue(response.getEnp()); // ЕНП
                row.createCell(16).setCellValue(formatDate(response.getDatE_BEGIN())); // Дата начала
                row.createCell(17).setCellValue(formatDate(response.getDatE_END())); // Дата окончания
                row.createCell(18).setCellValue(response.getSmo()); // СМО
                row.createCell(19).setCellValue(response.getNamsmok()); // Название СМО
                row.createCell(20).setCellValue(response.getReenom()); // Реестровый номер
                row.createCell(21).setCellValue(response.getDoctype()); // Тип ДУЛ
                row.createCell(22).setCellValue(response.getDocser()); // Серия ДУЛ
                row.createCell(23).setCellValue(response.getDocnum()); // Номер ДУЛ
                row.createCell(24).setCellValue(response.getDocorg()); // Организация
                row.createCell(25).setCellValue(formatDate(response.getDocdate())); // Дата выдачи
                row.createCell(26).setCellValue(response.getSnils()); // СНИЛС
                row.createCell(27).setCellValue(response.isActive() ? "Да" : "Нет"); // Активный
                row.createCell(28).setCellValue(response.getAdres()); // Адрес
                row.createCell(29).setCellValue(formatDate(response.getDatE_P())); // Дата П
                row.createCell(30).setCellValue(response.getTerst()); // Территория
                row.createCell(31).setCellValue(response.getName()); // Организация
                row.createCell(32).setCellValue(response.getCorrect()); // Корректность
                row.createCell(33).setCellValue(response.getSource()); // Источник
                row.createCell(34).setCellValue(response.getSourceNpolis()); // Исходный полис

                // Если есть расхождения или полис неактивный, закрашиваем строку
                if (hasDifference || isInactive) {
                    for (int i = 0; i < headers.length; i++) {
                        Cell cell = row.getCell(i);
                        if (cell == null) {
                            cell = row.createCell(i);
                        }
                        cell.setCellStyle(highlightStyle);
                    }
                } else {
                    for (int i = 0; i < headers.length; i++) {
                        row.getCell(i).setCellStyle(greenStyle);
                    }
                }
            }

            // Автоматически подгоняем ширину столбцов
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Записываем файл
            try (FileOutputStream fileOut = new FileOutputStream(file)) {
                workbook.write(fileOut);
            }

        } catch (IOException e) {
            throw new RuntimeException("Ошибка при сохранении файла: " + e.getMessage(), e);
        }
    }

    // Метод для создания стиля с цветом #ffd1d1
    private static CellStyle getHighlightRowStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        XSSFColor color = new XSSFColor(new java.awt.Color(255, 209, 209), new DefaultIndexedColorMap());
        ((XSSFCellStyle) style).setFillForegroundColor(color);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    // Метод для создания стиля с цветом #ffd1d1
    private static CellStyle getGreenRowStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        // Преобразование шестнадцатеричного цвета #deffc9 в RGB
        int red = 0xDE;   // 222
        int green = 0xFF; // 255
        int blue = 0xC9;  // 201

        // Создание цвета с использованием RGB
        XSSFColor color = new XSSFColor(new java.awt.Color(red, green, blue), new DefaultIndexedColorMap());
        ((XSSFCellStyle) style).setFillForegroundColor(color);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    // Метод для создания стиля заголовков
    private static CellStyle getHeaderCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontName("Arial");
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private static String formatDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return "";
        }
        return LocalDate.parse(dateStr).format(formatter);
    }

    // Метод для добавления ошибок в строку с разделителем
    private static void appendError(StringBuilder sb, String error) {
        if (sb.length() > 0) {
            sb.append(", ");
        }
        sb.append(error);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replace("\u00A0", "").replaceAll("\\s+", "");
    }
}