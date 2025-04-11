package ru.rdc.FomsService.service;

import org.springframework.stereotype.Service;
import ru.rdc.FomsService.dto.Item;
import ru.rdc.FomsService.utils.ReadMekErrForXML;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class MekXmlService {

    public List<Item> parseXmlFile(String xmlPath) {
        List<Item> items = new ArrayList<>();

        try {
            //System.out.println("Пытаемся открыть файл по пути: " + xmlPath);
            File xmlFile = new File(xmlPath);
            if (!xmlFile.exists()) {
                //System.out.println("Файл не найден: " + xmlPath);
                return items;  // Возвращаем пустой список, если файл не найден
            }

            InputStream inputStream = new FileInputStream(xmlFile);

            // Парсим XML
            items = ReadMekErrForXML.getMek(inputStream);

            /*System.out.println("Размер списка items: " + items.size());
            for (Item item : items) {
                System.out.println("Полис: " + item.getNpolis() + ", Фамилия: " + item.getFam());
            }*/

        } catch (Exception e) {
            e.printStackTrace();
        }

        return items;
    }


    /*public List<Item> parseXmlFile(String filePath) {
        try (InputStream inputStream = new FileInputStream(filePath)) {
            return ReadMekErrForXML.getMek(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }*/
}