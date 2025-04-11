package ru.rdc.FomsService.utils;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import ru.rdc.FomsService.dto.Item;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

//Класс для чтения XML файла МЭК
public class ReadMekErrForXML {

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ENGLISH);
    private static DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static List<Item> getMek(InputStream inputStream) {
        List<Item> rowList = null;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);  // Используем InputStream
            document.getDocumentElement().normalize();
            NodeList nodeList = document.getElementsByTagName("ERR");

            //System.out.println("Найдено ERR элементов: " + nodeList.getLength());

            rowList = new ArrayList<>();

            for (int i = 0; i < nodeList.getLength(); i++) {
                Element element = (Element) nodeList.item(i);
                String refreason = getTagValue("refreason", element);
                String npolis = getTagValue("npolis", element); // извлекаем поле npolis

                // Логируем refreason для каждой записи
                //System.out.println("Refreason: " + refreason);

                if ("1.5.".equals(refreason) && npolis != null && npolis.length() == 16) {
                    rowList.add(getMek(nodeList.item(i)));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return rowList;
    }

    private static Item getMek(Node node) {
        Item item = new Item();

        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) node;

            item.setS_com(safeGetTagValue("s_com", element));
            item.setSpolis(""); // Нет такого поля в XML
            item.setNpolis(safeGetTagValue("npolis", element));
            item.setFam(safeGetTagValue("surname", element));
            item.setIm(safeGetTagValue("name", element));
            item.setOt(safeGetTagValue("patronymic", element));
            item.setBirthDate(safeGetTagValue("birthDate", element));

            String dateIn = safeGetTagValue("date_in", element);
            if (!dateIn.isEmpty()) {
                item.setDate_in(LocalDate.parse(dateIn, formatter1));
            }

            String dateOut = safeGetTagValue("date_out", element);
            if (!dateOut.isEmpty()) {
                item.setDate_out(LocalDate.parse(dateOut, formatter1));
            }

            item.setRefreason(safeGetTagValue("refreason", element));
            item.setCodeUsl(safeGetTagValue("codeUsl", element));

            String sankSum = safeGetTagValue("sankSum", element);
            item.setSum(!sankSum.isEmpty() ? Double.parseDouble(sankSum) : 0.0);

            item.setSumv(0.0); // Нет поля в XML
            item.setDs1(safeGetTagValue("diagnosis", element));
            item.setNameMO(safeGetTagValue("nameMO", element));
            item.setIddokt(safeGetTagValue("docCode", element));
            item.setNhistory(safeGetTagValue("nhistory", element));
            item.setSchet(""); // Нет такого поля
            item.setIdstrax(safeGetTagValue("idstrax", element));
            item.setIdserv(""); // Нет такого поля
        }

        return item;
    }

    /*private static Item getMek(Node node) {
        Item item = new Item();

        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element element = (Element) node;

            item.setS_com(safeGetTagValue("S_COM", element));
            item.setSpolis(safeGetTagValue("SPOLIS", element));
            item.setNpolis(safeGetTagValue("NPOLIS", element));
            item.setFam(safeGetTagValue("FAM", element));
            item.setIm(safeGetTagValue("IM", element));
            item.setOt(safeGetTagValue("OT", element));
            item.setBirthDate(safeGetTagValue("BIRTHDATE", element));  // Исправлено с "DR" на "BIRTHDATE"

            String dateIn = safeGetTagValue("DATE_IN", element);
            if (!dateIn.isEmpty()) {
                item.setDate_in(LocalDate.parse(dateIn));  // Убедитесь, что формат даты соответствует
            }

            String dateOut = safeGetTagValue("DATE_OUT", element);
            if (!dateOut.isEmpty()) {
                item.setDate_out(LocalDate.parse(dateOut));  // Убедитесь, что формат даты соответствует
            }

            item.setRefreason(safeGetTagValue("REFREASON", element));
            item.setCodeUsl(safeGetTagValue("CODE_USL", element));

            String sumValue = safeGetTagValue("SUM", element);
            item.setSum(!sumValue.isEmpty() ? Double.parseDouble(sumValue) : 0.0);

            String sumvValue = safeGetTagValue("SUMV", element);
            item.setSumv(!sumvValue.isEmpty() ? Double.parseDouble(sumvValue) : 0.0);

            item.setDs1(safeGetTagValue("DS1", element));
            item.setNameMO(safeGetTagValue("MO", element));
            item.setIddokt(safeGetTagValue("IDDOKT", element));
            item.setNhistory(safeGetTagValue("NHISTORY", element));
            item.setSchet(safeGetTagValue("SCHET", element));
            item.setIdstrax(safeGetTagValue("IDSTRAX", element));
            item.setIdserv(safeGetTagValue("IDSERV", element));
        }

        return item;
    }*/

    private static String getTagValue(String tag, Element element) {
        NodeList nodeList = element.getElementsByTagName(tag);
        if (nodeList.getLength() > 0) {
            Node node = nodeList.item(0).getFirstChild();
            if (node != null) {
                String value = node.getNodeValue();
                return (value != null && !value.trim().isEmpty()) ? value.trim() : null;
            }
        }
        return null;
    }

    private static String safeGetTagValue(String tag, Element element) {
        String value = getTagValue(tag, element);
        return (value != null) ? value : "";
    }
}
