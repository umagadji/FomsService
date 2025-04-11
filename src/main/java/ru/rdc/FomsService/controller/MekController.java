package ru.rdc.FomsService.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.rdc.FomsService.dto.InsurancePackageRequest;
import ru.rdc.FomsService.dto.InsurancePackageResponse;
import ru.rdc.FomsService.dto.InsuranceRequest;
import ru.rdc.FomsService.dto.Item;
import ru.rdc.FomsService.reports.ExcelExporter;
import ru.rdc.FomsService.service.InsurancePackageService;
import ru.rdc.FomsService.service.MekXmlService;
import ru.rdc.FomsService.service.MisService;
import ru.rdc.FomsService.utils.ReadMekErrForXML;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
@RequestMapping("/mek")
public class MekController {

    private final MekXmlService mekXmlService;
    private final InsurancePackageService insurancePackageService;
    private final ExcelExporter excelExporter;

    public MekController(MekXmlService mekXmlService,
                         InsurancePackageService insurancePackageService,
                         ExcelExporter excelExporter) {
        this.mekXmlService = mekXmlService;
        this.insurancePackageService = insurancePackageService;
        this.excelExporter = excelExporter;
    }

    @GetMapping
    public String showMekPage(@RequestParam(required = false) String xmlPath,
                              Model model,
                              HttpServletRequest request) {
        List<Item> items = new ArrayList<>();

        if (xmlPath != null && !xmlPath.isEmpty()) {
            items = mekXmlService.parseXmlFile(xmlPath);

            // 🔄 Форматируем даты сразу после парсинга
            formatItemDates(items);
        }

        model.addAttribute("items", items);
        model.addAttribute("xmlPath", xmlPath);
        model.addAttribute("requestURI", request.getRequestURI());

        return "mek-page";
    }

    @PostMapping("/package-query")
    @ResponseBody
    public Map<String, Object> handlePackageQuery(@RequestParam String type,
                                                  @RequestParam String xmlPath) {

        List<Item> items = mekXmlService.parseXmlFile(xmlPath);

        // 🔄 Форматируем даты сразу после парсинга
        formatItemDates(items);

        Set<InsuranceRequest> uniqueRequests = new LinkedHashSet<>();

        for (Item item : items) {
            InsuranceRequest request = new InsuranceRequest();
            request.setRequestId(1);

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

            uniqueRequests.add(request);
        }

        int requestId = 1;
        List<InsuranceRequest> insuranceRequestList = new ArrayList<>();
        for (InsuranceRequest req : uniqueRequests) {
            req.setRequestId(requestId++);
            insuranceRequestList.add(req);
        }

        InsurancePackageRequest packageRequest = new InsurancePackageRequest();
        packageRequest.setInsuranceRequestList(insuranceRequestList);

        InsurancePackageResponse response = insurancePackageService.getPackageInsurance(packageRequest)
                .onErrorReturn(new InsurancePackageResponse())
                .block();

        if (!response.getResponses().isEmpty()) {
            File file = new File("response-mek.xlsx");
            excelExporter.saveToExcel(response.getResponses(), items, file);
        }

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("hasData", !response.getResponses().isEmpty());
        responseData.put("downloadFile", "/mek/downloadFile");

        return responseData;
    }

    @PostMapping("/mek-upload")
    @ResponseBody
    public Map<String, Object> uploadMekXml(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        if (file.isEmpty()) {
            response.put("success", false);
            response.put("message", "Файл пустой.");
            return response;
        }

        try {
            String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();
            Path uploadPath = Paths.get("uploads", fileName);
            Files.createDirectories(uploadPath.getParent());
            Files.copy(file.getInputStream(), uploadPath, StandardCopyOption.REPLACE_EXISTING);

            response.put("success", true);
            response.put("xmlPath", uploadPath.toString());
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "Ошибка при сохранении файла: " + e.getMessage());
        }

        return response;
    }

    @GetMapping("/downloadFile")
    public ResponseEntity<Resource> downloadFile() {
        File file = new File("response-mek.xlsx");

        if (!file.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        Path path = file.toPath();
        Resource resource = new FileSystemResource(path);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .body(resource);
    }

    private String convertDateFormat(String isoDate) {
        try {
            LocalDate date = LocalDate.parse(isoDate);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            return date.format(formatter);
        } catch (Exception e) {
            System.err.println("Ошибка преобразования даты: " + isoDate);
            return isoDate;
        }
    }

    private void formatItemDates(List<Item> items) {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        for (Item item : items) {
            if (item.getBirthDate() != null && !item.getBirthDate().isEmpty()) {
                try {
                    LocalDate date = LocalDate.parse(item.getBirthDate(), inputFormatter);
                    item.setBirthDate(date.format(outputFormatter));
                } catch (Exception e) {
                    System.err.println("Ошибка преобразования даты рождения: " + item.getBirthDate());
                }
            }
        }
    }
}