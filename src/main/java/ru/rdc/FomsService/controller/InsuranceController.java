package ru.rdc.FomsService.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.rdc.FomsService.dto.InsuranceRequest;
import ru.rdc.FomsService.service.InsuranceService;

@Controller
@RequestMapping("/")
public class InsuranceController {

    private final InsuranceService insuranceService;

    public InsuranceController(InsuranceService insuranceService) {
        this.insuranceService = insuranceService;
    }

    @GetMapping
    public String showInsuranceForm(Model model, HttpServletRequest request) {
        model.addAttribute("insuranceRequest", new InsuranceRequest());
        model.addAttribute("requestURI", request.getRequestURI());
        return "insurance-search";
    }

    @PostMapping("/submit")
    @ResponseBody
    public Mono<ResponseEntity<String>> submitInsurance(@RequestBody InsuranceRequest insuranceRequest) {
        System.out.println("Получен запрос: " + insuranceRequest);

        if (insuranceRequest.getStype() == null) {
            return Mono.just(ResponseEntity.badRequest().body("Ошибка: Не указан тип поиска."));
        }

        return insuranceService.getSingleInsurance(insuranceRequest)
                .map(response -> ResponseEntity.ok(response)) // Успешный ответ
                .onErrorResume(e -> {
                    if (e.getMessage() != null && e.getMessage().contains("Ошибка 400")) {
                        return Mono.just(ResponseEntity.badRequest().body("Ошибка 400: Неверные данные."));
                    }
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка сервера. Вероятно сервисы ТФОМС недоступны"));
                });
                /*.onErrorResume(RuntimeException.class, e -> {
                    if (e.getMessage().contains("Ошибка 400")) {
                        return Mono.just(ResponseEntity.badRequest().body("Ошибка 400: Неверные данные."));
                    }
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка сервера"));
                });*/
    }
}


