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
import java.util.stream.Collectors;

import static org.thymeleaf.util.StringUtils.trim;

@Component
public class ExcelExporter {

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ENGLISH);

    public static void saveToExcel(List<InsuranceResponse> responses, List<Item> items, File file) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Нет исходных данных!");
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Результаты");

            CellStyle headerStyle = getHeaderCellStyle(workbook);
            // Создаем стиль для строк с расхождениями
            CellStyle redStyle = getHighlightRowStyle(workbook);
            CellStyle greenStyle = getGreenRowStyle(workbook);
            CellStyle blueStyle = getBlueRowStyle(workbook);  // синяя заливка для активных полисов

            String[] headers = {
                    "Request ID", "Комментарий", "Причина","Исх. Полис",
                    "Исх. Фамилия", "Исх. Имя", "Исх. Отчество", "Исх. Дата рождения", "Исх. МО",
                    "Полис", "ЕНП", "Ответ: Фамилия", "Ответ: Имя", "Ответ: Отчество", "Ответ: Дата рождения", "Ответ: Пол",
                    "Тип полиса", "Серия", "Дата начала", "Дата окончания", "СМО", "Название СМО",
                    "Реестровый №", "Тип ДУЛ", "Серия ДУЛ", "Номер ДУЛ", "Организация", "Дата выдачи",
                    "СНИЛС", "Активный", "Адрес", "Дата П", "Территория", "Организация", "Корректность", "Источник"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            Map<Integer, InsuranceResponse> responseMap = responses.stream()
                    .collect(Collectors.toMap(InsuranceResponse::getRequestId, r -> r));

            int rowNum = 1;
            for (Item item : items) {
                Row row = sheet.createRow(rowNum++);
                int col = 0;
                int requestId = item.getRequestId();
                InsuranceResponse response = responseMap.get(requestId);

                boolean isFioDrEqual = false;
                boolean isPolicyEqual = false;
                boolean isActive = false;
                StringBuilder errorDetails = new StringBuilder();

                //CellStyle rowStyle = greenStyle;  // по умолчанию, если все нормально

                if (response != null) {
                    // Логика сравнения ФИО, ДР и полиса
                    isFioDrEqual = Objects.equals(toUpperTrim(item.getFam()), toUpperTrim(response.getFam()))
                            && Objects.equals(toUpperTrim(item.getIm()), toUpperTrim(response.getIm()))
                            && Objects.equals(toUpperTrim(item.getOt()), toUpperTrim(response.getOt()))
                            && areDatesEqual(item.getBirthDate(), response.getDr());

                    isPolicyEqual = Objects.equals(item.getNpolis(), response.getEnp());
                    isActive = response.isActive();

                    // Сравнение ФИО и ДР
                    if (!Objects.equals(toUpperTrim(item.getFam()), toUpperTrim(response.getFam()))) {
                        appendError(errorDetails, "Фамилия");
                    }
                    if (!Objects.equals(toUpperTrim(item.getIm()), toUpperTrim(response.getIm()))) {
                        appendError(errorDetails, "Имя");
                    }
                    if (!Objects.equals(toUpperTrim(item.getOt()), toUpperTrim(response.getOt()))) {
                        appendError(errorDetails, "Отчество");
                    }
                    if (!areDatesEqual(item.getBirthDate(), response.getDr())) {
                        appendError(errorDetails, "Дата рождения");
                    }

                    // Сравнение полисов
                    if (!Objects.equals(item.getNpolis(), response.getEnp())) {
                        appendError(errorDetails, isActive ? "Другой активный полис" : "Другой неактивный полис");
                    }
                    if (!isActive) {
                        appendError(errorDetails, "Полис неактивный");
                    }
                }

                // 🎯 Логика подсветки на основе условий
                CellStyle rowStyle;
                if (isFioDrEqual) {
                    if (isPolicyEqual) {
                        rowStyle = isActive ? greenStyle : redStyle;
                    } else {
                        rowStyle = isActive ? greenStyle : redStyle;
                    }
                } else {
                    rowStyle = redStyle;
                }

                // ✍️ Пишем данные в ячейки
                row.createCell(col++).setCellValue(requestId);
                row.createCell(col++).setCellValue(item.getS_com());
                //row.createCell(col++).setCellValue(errorDetails.toString());

                // Если response == null, то в поле "Причина" пишем "Сервер не вернул ответ"
                String reason = (response == null) ? "Сервер не вернул ответ" : errorDetails.toString();
                row.createCell(col++).setCellValue(reason);

                row.createCell(col++).setCellValue(item.getNpolis());

                row.createCell(col++).setCellValue(toUpperTrim(item.getFam()));
                row.createCell(col++).setCellValue(toUpperTrim(item.getIm()));
                row.createCell(col++).setCellValue(toUpperTrim(item.getOt()));
                row.createCell(col++).setCellValue(item.getBirthDate());
                row.createCell(col++).setCellValue(item.getNameMO());

                if (response != null) {
                    row.createCell(col++).setCellValue(response.getNpolis());
                    row.createCell(col++).setCellValue(response.getEnp());
                    row.createCell(col++).setCellValue(toUpperTrim(response.getFam()));
                    row.createCell(col++).setCellValue(toUpperTrim(response.getIm()));
                    row.createCell(col++).setCellValue(toUpperTrim(response.getOt()));
                    row.createCell(col++).setCellValue(formatDate(response.getDr()));
                    row.createCell(col++).setCellValue(response.getW() == 1 ? "Мужской" : "Женский");
                    row.createCell(col++).setCellValue(response.getVpolis());
                    row.createCell(col++).setCellValue(response.getSpolis());
                    row.createCell(col++).setCellValue(formatDate(response.getDatE_BEGIN()));
                    row.createCell(col++).setCellValue(formatDate(response.getDatE_END()));
                    row.createCell(col++).setCellValue(response.getSmo());
                    row.createCell(col++).setCellValue(response.getNamsmok());
                    row.createCell(col++).setCellValue(response.getReenom());
                    row.createCell(col++).setCellValue(response.getDoctype());
                    row.createCell(col++).setCellValue(response.getDocser());
                    row.createCell(col++).setCellValue(response.getDocnum());
                    row.createCell(col++).setCellValue(response.getDocorg());
                    row.createCell(col++).setCellValue(formatDate(response.getDocdate()));
                    row.createCell(col++).setCellValue(response.getSnils());
                    row.createCell(col++).setCellValue(response.isActive() ? "Да" : "Нет");
                    row.createCell(col++).setCellValue(response.getAdres());
                    row.createCell(col++).setCellValue(formatDate(response.getDatE_P()));
                    row.createCell(col++).setCellValue(response.getTerst());
                    row.createCell(col++).setCellValue(response.getName());
                    row.createCell(col++).setCellValue(response.getCorrect());
                    row.createCell(col++).setCellValue(response.getSource());
                } else {
                    for (int i = 0; i < 28; i++) {
                        row.createCell(col++).setCellValue("");
                    }
                }

                // 🎨 Подсветка строки
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = row.getCell(i);
                    if (cell == null) cell = row.createCell(i);
                    cell.setCellStyle(rowStyle);
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream out = new FileOutputStream(file)) {
                workbook.write(out);
            }

        } catch (IOException e) {
            throw new RuntimeException("Ошибка при записи Excel: " + e.getMessage(), e);
        }
    }

    private static String toUpperTrim(String s) {
        return s == null ? null : s.trim().toUpperCase();
    }

    private static boolean areDatesEqual(String itemDate, String responseDate) {
        if (itemDate == null || responseDate == null) return false;
        try {
            LocalDate d1 = LocalDate.parse(itemDate, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            LocalDate d2 = LocalDate.parse(responseDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return d1.equals(d2);
        } catch (DateTimeParseException e) {
            return false;
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

    private static CellStyle getBlueRowStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        XSSFColor color = new XSSFColor(new java.awt.Color(173, 216, 230), new DefaultIndexedColorMap()); // синий
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