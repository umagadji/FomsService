package ru.rdc.FomsService.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.rdc.FomsService.dto.*;
import ru.rdc.FomsService.reports.ExcelExporter;
import ru.rdc.FomsService.service.InsurancePackageService;
import ru.rdc.FomsService.service.MisService;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/mis")
public class MisController {

    private final MisService misService;               // Сервис для получения данных из базы
    private final InsurancePackageService insurancePackageService;  // Сервис для работы с запросами на пакет
    private final ExcelExporter excelExporter;          // Сервис для экспорта данных в Excel

    // Конструктор контроллера, инициализируем сервисы, через которые будет работать контроллер
    public MisController(MisService misService, InsurancePackageService insurancePackageService, ExcelExporter excelExporter) {
        this.misService = misService;
        this.insurancePackageService = insurancePackageService;
        this.excelExporter = excelExporter;
    }

    @GetMapping
    public String showMisPage(Model model, HttpServletRequest request) {

        model.addAttribute("requestURI", request.getRequestURI());

        return "mis-page";  // Возвращаем имя страницы для отображения
    }

    @GetMapping("/items")
    @ResponseBody
    public List<Item> getItems(@RequestParam String startDate,
                               @RequestParam String endDate,
                               Model model,
                               HttpServletRequest request) {

        List<Item> items = new ArrayList<>();

        // Если переданы параметры startDate и endDate, то запрашиваем данные из базы
        if (startDate != null && endDate != null) {
            items = misService.getOracleModels(startDate, endDate);  // Получаем данные по датам
        }

        // Добавляем данные в модель для отображения на странице
        model.addAttribute("items", items);
        model.addAttribute("startDate", startDate);  // Дата начала
        model.addAttribute("endDate", endDate);      // Дата конца
        model.addAttribute("requestURI", request.getRequestURI());

        return misService.getOracleModels(startDate, endDate);
    }

    @PostMapping("/package-query")
    @ResponseBody
    public Map<String, Object> handlePackageQuery(@RequestParam String type,
                                                  @RequestParam String startDate,
                                                  @RequestParam String endDate) {
        Map<String, Object> responseData = new HashMap<>();
        System.out.println("Начало обработки package-query, тип: " + type); // ← Логируем начало

        try {
            // Получаем данные из базы по переданным датам
            List<Item> items = misService.getOracleModels(startDate, endDate);
            System.out.println("Получено items из базы: " + items.size()); // ← Логируем количество записей

            // Сохраняем связь между уникальными запросами и оригинальными Item
            Map<InsuranceRequest, Item> requestItemMap = new LinkedHashMap<>();

            for (Item item : items) {
                InsuranceRequest request = new InsuranceRequest();
                request.setRequestId(1); // временно

                if ("enp".equals(type)) {
                    request.setEnp(item.getNpolis());
                    request.setStype(1);
                } else if ("fio".equals(type)) {
                    request.setFam(item.getFam());
                    request.setIm(item.getIm());
                    request.setOt(item.getOt());
                    request.setDr(convertDateFormat(item.getBirthDate()));
                    request.setStype(2);
                }

                // Добавляем в мапу (дубликаты отфильтруются по equals/hashCode)
                requestItemMap.put(request, item);
            }

            System.out.println("Уникальных запросов: " + requestItemMap.size()); // ← Логируем количество уникальных запросов

            // Присваиваем уникальные requestId и связываем с Item
            int requestId = 1;
            List<InsuranceRequest> insuranceRequestList = new ArrayList<>();

            for (Map.Entry<InsuranceRequest, Item> entry : requestItemMap.entrySet()) {
                InsuranceRequest req = entry.getKey();
                Item item = entry.getValue();

                req.setRequestId(requestId);
                item.setRequestId(requestId);

                insuranceRequestList.add(req);
                requestId++;
            }

            // Создаём пакетный запрос
            InsurancePackageRequest packageRequest = new InsurancePackageRequest();
            packageRequest.setInsuranceRequestList(insuranceRequestList);

            System.out.println("Отправка запроса во внешний сервис..."); // ← Логируем перед вызовом сервиса

            // Запрос во внешний сервис с обработкой ошибок
            InsurancePackageResponse response = insurancePackageService.getPackageInsurance(packageRequest)
                    .onErrorResume(e -> {
                        String errorMsg = "Ошибка обращения к внешнему сервису: " + e.getMessage();
                        System.err.println(errorMsg);
                        InsurancePackageResponse errorResp = new InsurancePackageResponse();
                        errorResp.setErrorMessage(errorMsg);
                        return Mono.just(errorResp);
                    })
                    .block();

            // Проверка на ошибку
            if (response.getErrorMessage() != null) {
                responseData.put("error", response.getErrorMessage());
                return responseData;
            }

            // Проверка на наличие данных
            if (response.getResponses() == null || response.getResponses().isEmpty()) {
                responseData.put("error", "Ответ от внешнего сервиса пуст");
                return responseData;
            }

            System.out.println("Ответ от сервиса получен, количество ответов: " +
                    response.getResponses().size());

            // Сохраняем в Excel
            File file = new File("response.xlsx");
            System.out.println("Сохранение в Excel...");
            excelExporter.saveToExcel(response.getResponses(), new ArrayList<>(requestItemMap.values()), file);
            System.out.println("Файл Excel создан");

            responseData.put("hasData", true);
            responseData.put("downloadFile", "/mis/downloadFile");

            /*// Получаем ответ от внешнего сервиса
            InsurancePackageResponse response = insurancePackageService.getPackageInsurance(packageRequest)
                    .onErrorReturn(new InsurancePackageResponse())
                    .block();

            System.out.println("Ответ от сервиса получен, количество ответов: " +
                    (response.getResponses() != null ? response.getResponses().size() : 0)); // ← Логируем ответ

            // Если есть ответ — сохраняем в Excel
            if (response.getResponses() != null && !response.getResponses().isEmpty()) {
                File file = new File("response.xlsx");
                System.out.println("Сохранение в Excel..."); // ← Логируем перед сохранением
                excelExporter.saveToExcel(response.getResponses(), new ArrayList<>(requestItemMap.values()), file);
                System.out.println("Файл Excel создан"); // ← Логируем успешное сохранение
            }

            responseData.put("hasData", response.getResponses() != null && !response.getResponses().isEmpty());
            responseData.put("downloadFile", "/mis/downloadFile");*/

        } catch (Exception e) {
            String errorMsg = "Ошибка в handlePackageQuery: " + e.getMessage(); // ← Логируем ошибку
            System.err.println(errorMsg);
            e.printStackTrace();
            responseData.put("error", errorMsg);
        }

        System.out.println("Возвращаемые данные: " + responseData); // ← Логируем финальный responseData

        return responseData;
    }

    // Метод для скачивания файла
    @GetMapping("/downloadFile")
    public ResponseEntity<Resource> downloadFile() {
        // Путь к файлу Excel, который мы создали ранее
        File file = new File("response.xlsx");

        // Проверяем, существует ли файл
        if (!file.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);  // Если файла нет, возвращаем 404
        }

        // Загружаем файл как ресурс
        Path path = file.toPath();  // Преобразуем файл в путь
        Resource resource = new FileSystemResource(path);  // Создаём ресурс из файла

        // Возвращаем файл как response с указанием типа контента и имени файла
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)  // Устанавливаем тип контента для бинарных данных
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")  // Устанавливаем заголовок для скачивания
                .body(resource);  // Отправляем файл в теле ответа
    }

    private String convertDateFormat(String oldFormatDate) {
        try {
            DateTimeFormatter oldFormat = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            DateTimeFormatter newFormat = DateTimeFormatter.ISO_LOCAL_DATE; // "yyyy-MM-dd"
            LocalDate date = LocalDate.parse(oldFormatDate, oldFormat);
            return date.format(newFormat);
        } catch (Exception e) {
            // В случае ошибки — логируем и возвращаем оригинал
            System.err.println("Ошибка преобразования даты: " + oldFormatDate);
            return oldFormatDate;
        }
    }

}