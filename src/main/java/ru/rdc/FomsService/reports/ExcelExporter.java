package ru.rdc.FomsService.reports;

import jakarta.annotation.PostConstruct;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
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

@Component
public class ExcelExporter {

    // Храним только параметры стилей, а не сами стили
    private FontStyle headerFontStyle;
    private CellColorStyle redStyleParams;
    private CellColorStyle greenStyleParams;
    private CellColorStyle blueStyleParams;

    // Вспомогательные record-классы для хранения параметров стилей
    private record FontStyle(boolean bold, String fontName, short fontSize) {}
    private record CellColorStyle(java.awt.Color color, HorizontalAlignment alignment) {}

    @PostConstruct
    public void initStyleParams() {
        this.headerFontStyle = new FontStyle(true, "Arial", (short)11);
        this.redStyleParams = new CellColorStyle(new java.awt.Color(255, 209, 209), HorizontalAlignment.LEFT);
        this.greenStyleParams = new CellColorStyle(new java.awt.Color(222, 255, 201), HorizontalAlignment.LEFT);
        this.blueStyleParams = new CellColorStyle(new java.awt.Color(173, 216, 230), HorizontalAlignment.LEFT);
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(headerFontStyle.bold());
        font.setFontName(headerFontStyle.fontName());
        font.setFontHeightInPoints(headerFontStyle.fontSize());
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createHighlightStyle(Workbook workbook, CellColorStyle params) {
        CellStyle style = workbook.createCellStyle();
        XSSFColor color = new XSSFColor(params.color(), new DefaultIndexedColorMap());
        ((XSSFCellStyle) style).setFillForegroundColor(color);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(params.alignment());
        return style;
    }

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ENGLISH);

    public void saveToExcel(List<InsuranceResponse> responses, List<Item> items, File file) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Нет исходных данных!");
        }

        // Предварительная обработка данных
        preprocessItems(items, responses);

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(1000)) {
            workbook.setCompressTempFiles(true);  // Сжимать временные файлы (экономит место)

            // Создаем стили для этого конкретного workbook
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle redStyle = createHighlightStyle(workbook, redStyleParams);
            CellStyle greenStyle = createHighlightStyle(workbook, greenStyleParams);
            CellStyle blueStyle = createHighlightStyle(workbook, blueStyleParams);

            SXSSFSheet sheet = workbook.createSheet("Результаты");

            // Отключаем ненужные функции для больших файлов
            sheet.setRandomAccessWindowSize(100); // Лимит строк в памяти для случайного доступа

            String[] headers = {
                    "Request ID", "Комментарий", "Причина", "Исх. Полис",
                    "Исх. Фамилия", "Исх. Имя", "Исх. Отчество", "Исх. Дата рождения", "Исх. МО",
                    "Полис", "ЕНП", "Ответ: Фамилия", "Ответ: Имя", "Ответ: Отчество", "Ответ: Дата рождения", "Ответ: Пол",
                    "Тип полиса", "Серия", "Дата начала", "Дата окончания", "СМО", "Название СМО",
                    "Реестровый №", "Тип ДУЛ", "Серия ДУЛ", "Номер ДУЛ", "Организация", "Дата выдачи",
                    "СНИЛС", "Активный", "Адрес", "Дата П", "Территория", "Организация", "Корректность", "Источник"
            };

            // Создаем заголовки таблицы
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Группируем ответы по requestId (теперь один requestId может иметь несколько ответов)
            Map<Integer, List<InsuranceResponse>> responseMap = responses.stream()
                    .collect(Collectors.groupingBy(InsuranceResponse::getRequestId));

            int rowNum = 1;

            // Проходим по всем исходным записям
            for (Item item : items) {
                int requestId = item.getRequestId();
                List<InsuranceResponse> responseList = responseMap.get(requestId);

                // Если нет ответов для этого requestId, создаем одну строку с пустыми данными
                if (responseList == null || responseList.isEmpty()) {
                    Row row = sheet.createRow(rowNum++);
                    fillRowWithItemData(row, item, null, redStyle);
                    continue;
                }

                // Для каждого ответа создаем отдельную строку
                for (InsuranceResponse response : responseList) {
                    Row row = sheet.createRow(rowNum++);

                    // Проверяем соответствие данных
                    boolean isFioDrEqual = isFioDrEqual(item, response);
                    boolean isPolicyEqual = Objects.equals(item.getNpolis(), response.getEnp());
                    boolean isActive = response.isActive();

                    // Определяем стиль строки на основе проверок
                    CellStyle rowStyle;
                    if (isFioDrEqual) {
                        rowStyle = isActive ? greenStyle : redStyle;
                    } else {
                        rowStyle = redStyle;
                    }

                    // Заполняем строку данными
                    fillRowWithItemData(row, item, response, rowStyle);

                    // Добавляем информацию о несоответствиях
                    if (response != null) {
                        String reason = buildReasonString(item, response, isFioDrEqual, isPolicyEqual, isActive);
                        row.getCell(2).setCellValue(reason); // Ячейка "Причина"
                    }
                }
            }

            // Сохраняем файл
            try (FileOutputStream out = new FileOutputStream(file)) {
                workbook.write(out);
            }

            // После записи данных можно очистить временные файлы
            workbook.dispose();

        } catch (IOException e) {
            throw new RuntimeException("Ошибка при записи Excel: " + e.getMessage(), e);
        }
    }

    private void preprocessItems(List<Item> items, List<InsuranceResponse> responses) {
        Optional.ofNullable(items).ifPresent(list -> list.forEach(this::normalizeItem));
        Optional.ofNullable(responses).ifPresent(list -> list.forEach(this::normalizeResponse));
    }

    private void normalizeItem(Item item) {
        item.setFam(normalize(item.getFam()));
        item.setIm(normalize(item.getIm()));
        item.setOt(normalize(item.getOt()));
    }

    private void normalizeResponse(InsuranceResponse response) {
        response.setFam(normalize(response.getFam()));
        response.setIm(normalize(response.getIm()));
        response.setOt(normalize(response.getOt()));
    }

    /*private void preprocessItems(List<Item> items, List<InsuranceResponse> responses) {
        if (items == null) return;

        items.forEach(item -> {
            item.setFam(normalize(item.getFam()));
            item.setIm(normalize(item.getIm()));
            item.setOt(normalize(item.getOt()));
        });

        responses.forEach(insuranceResponse -> {
            insuranceResponse.setFam(normalize(insuranceResponse.getFam()));
            insuranceResponse.setIm(normalize(insuranceResponse.getIm()));
            insuranceResponse.setOt(normalize(insuranceResponse.getOt()));
        });
    }*/

    // Метод для заполнения строки данными
    private static void fillRowWithItemData(Row row, Item item, InsuranceResponse response, CellStyle style) {
        int col = 0;

        // Request ID
        row.createCell(col++).setCellValue(item.getRequestId());

        // Комментарий
        row.createCell(col++).setCellValue(item.getS_com());

        // Причина (будет заполнена позже)
        row.createCell(col++).setCellValue("");

        // Исходные данные
        row.createCell(col++).setCellValue(item.getNpolis());
        row.createCell(col++).setCellValue(toUpperTrim(item.getFam()));
        row.createCell(col++).setCellValue(toUpperTrim(item.getIm()));
        row.createCell(col++).setCellValue(toUpperTrim(item.getOt()));
        row.createCell(col++).setCellValue(item.getBirthDate());
        row.createCell(col++).setCellValue(item.getNameMO());

        // Данные ответа
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
            // Заполняем пустые значения, если ответа нет
            for (int i = 0; i < 28; i++) {
                row.createCell(col++).setCellValue("");
            }
        }

        // Применяем стиль ко всем ячейкам строки
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell == null) cell = row.createCell(i);
            cell.setCellStyle(style);
        }
    }

    // Проверка соответствия ФИО и даты рождения
    private static boolean isFioDrEqual(Item item, InsuranceResponse response) {
        return normalize(item.getFam()).equals(normalize(response.getFam()))
                && normalize(item.getIm()).equals(normalize(response.getIm()))
                && normalize(item.getOt()).equals(normalize(response.getOt()))
                && areDatesEqual(item.getBirthDate(), response.getDr());
    }

    // Формирование строки с причинами несоответствий
    private static String buildReasonString(Item item, InsuranceResponse response,
                                            boolean isFioDrEqual, boolean isPolicyEqual,
                                            boolean isActive) {
        if (isFioDrEqual && isPolicyEqual && isActive) {
            return "Все данные совпадают";
        }

        StringBuilder reasons = new StringBuilder();

        if (!isFioDrEqual) {
            appendIfNotEqual(reasons, "Фамилия", item.getFam(), response.getFam());
            appendIfNotEqual(reasons, "Имя", item.getIm(), response.getIm());
            appendIfNotEqual(reasons, "Отчество", item.getOt(), response.getOt());
            if (!areDatesEqual(item.getBirthDate(), response.getDr())) {
                appendError(reasons, "Дата рождения");
            }

            System.out.println(item.getFam() + " : " + response.getFam());
            System.out.println(item.getIm() + " : " + response.getIm());
            System.out.println(item.getOt() + " : " + response.getOt());
            System.out.println(item.getBirthDate() + " : " + response.getDr());
        }

        if (!isPolicyEqual) {
            appendError(reasons, isActive ? "Другой активный полис" : "Другой неактивный полис");
        }

        if (!isActive) {
            appendError(reasons, "Полис неактивный");
        }

        return reasons.toString();
    }

    private static void appendIfNotEqual(StringBuilder sb, String fieldName, String value1, String value2) {
        String norm1 = normalize(value1);
        String norm2 = normalize(value2);
        if (!norm1.equals(norm2)) {
            System.out.printf("Difference in %s: '%s' != '%s'%n", fieldName, norm1, norm2);
            appendError(sb, fieldName);
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
        if (value == null) return "";
        // Удаляем все пробелы, неразрывные пробелы, приводим к верхнему регистру
        // И заменяем дефисы на пустую строку (или можно на пробел, если нужно сохранить разделение)
        return value.trim()
                .replace("\u00A0", "")
                .replaceAll("[\\s-]+", "")
                .toUpperCase();
    }
}