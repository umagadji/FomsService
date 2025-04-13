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

        try {
            // Получаем данные из базы по переданным датам
            List<Item> items = misService.getOracleModels(startDate, endDate);

            // Используем Set для хранения уникальных запросов
            Set<InsuranceRequest> uniqueRequests = new LinkedHashSet<>();

            // Создаём уникальные запросы, в зависимости от типа запроса (по полисам или по ФИО)
            for (Item item : items) {
                InsuranceRequest request = new InsuranceRequest();
                request.setRequestId(1);  // Временно присваиваем ID для запроса (в будущем это можно сделать уникальным)

                if ("enp".equals(type)) {
                    request.setEnp(item.getNpolis());  // Устанавливаем номер полиса
                    request.setStype(1);  // Устанавливаем тип запроса для полисов
                } else if ("fio".equals(type)) {
                    request.setFam(item.getFam());  // Устанавливаем фамилию
                    request.setIm(item.getIm());    // Устанавливаем имя
                    request.setOt(item.getOt());    // Устанавливаем отчество
                    request.setDr(convertDateFormat(item.getBirthDate()));  // Преобразуем дату рождения
                    request.setStype(2);  // Устанавливаем тип запроса для ФИО
                }

                // Добавляем уникальный запрос в Set
                uniqueRequests.add(request);
            }

            // Присваиваем уникальные ID для каждого запроса
            int requestId = 1;
            List<InsuranceRequest> insuranceRequestList = new ArrayList<>();
            for (InsuranceRequest req : uniqueRequests) {
                req.setRequestId(requestId++);
                insuranceRequestList.add(req);
            }

            // Создаём объект запроса для пакета
            InsurancePackageRequest packageRequest = new InsurancePackageRequest();
            packageRequest.setInsuranceRequestList(insuranceRequestList);  // Добавляем список запросов

            // Получаем ответ от внешнего сервиса
            InsurancePackageResponse response = insurancePackageService.getPackageInsurance(packageRequest)
                    .onErrorReturn(new InsurancePackageResponse())  // В случае ошибки возвращаем пустой ответ
                    .block();  // Блокируем выполнение, пока не получим ответ от сервиса

            // Если в ответе есть данные, сохраняем их в Excel-файл
            if (response.getResponses() != null && !response.getResponses().isEmpty()) {
                File file = new File("response.xlsx");
                excelExporter.saveToExcel(response.getResponses(), items, file);
            }

            // Формируем ответ для клиента (JSON)
            responseData.put("hasData", response.getResponses() != null && !response.getResponses().isEmpty());
            responseData.put("downloadFile", "/mis/downloadFile");  // Путь для скачивания файла

        } catch (Exception e) {
            // В случае ошибки добавляем информацию об ошибке в ответ
            responseData.put("error", "Ошибка запроса: " + e.getMessage());
        }

        return responseData;  // Возвращаем ответ в формате JSON
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