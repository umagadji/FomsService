package ru.rdc.FomsService.service;

import org.springframework.stereotype.Service;
import ru.rdc.FomsService.dto.Item;
import ru.rdc.FomsService.repository.MisRepository;

import java.util.List;

@Service
public class MisService {
    private final MisRepository misRepository;

    public MisService(MisRepository misRepository) {
        this.misRepository = misRepository;
    }

    public List<Item> getOracleModels(String startDate, String endDate) {
        return misRepository.findErrorsByDateRange(startDate, endDate);
    }
}
