package org.solo.manager_ktpp.parser;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.solo.manager_ktpp.model.Operation;
import org.solo.manager_ktpp.model.Part;
import org.solo.manager_ktpp.model.Process;

import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class ExcelParser {

    /**
     * Читает Excel (A-G), пропускает строки 0..3 (1..4 в Excel)
     * Возвращает map (code -> Part) в порядке чтения (LinkedHashMap)
     */
    public static Map<String, Part> parse(File file) throws Exception {
        Map<String, Part> nodeMap = new LinkedHashMap<>();
        try (FileInputStream fis = new FileInputStream(file);
             Workbook wb = new XSSFWorkbook(fis)) {

            Sheet sheet = wb.getSheetAt(0);
            for (Row row : sheet) {
                int rn = row.getRowNum();
                if (rn < 4) continue; // пропускаем шапку 1-4 строки

                String code = getCellString(row.getCell(0)).trim(); // A
                if (code.isEmpty()) continue;

                String name = getCellString(row.getCell(1)).trim(); // B
                String dept = getCellString(row.getCell(2)).trim(); // C
                String kd = getCellString(row.getCell(3)).trim();   // D
                String td = getCellString(row.getCell(4)).trim();   // E
                String buy = getCellString(row.getCell(5)).trim();  // F
                String prod = getCellString(row.getCell(6)).trim(); // G

                boolean hasKTPP = !kd.isEmpty() || !td.isEmpty();

                Part part = new Part(code, name, hasKTPP);

                // --- Укрупнённый маршрут (УТ) / Закупка (Зак) / Производство
                if (!buy.isEmpty() || !prod.isEmpty()) {
                    Process ut = new Process("УТ " + name, "УТ");
                    // Закупка
                    if (!buy.isEmpty()) {
                        Operation opBuy = new Operation("Закупка", buy, "УЗ");
                        ut.addOperation(opBuy);
                        // если есть КД/ТД создаём ТМЦ и привяжем к закупке (по вашему указанию)
                        if (hasKTPP) {
                            Process tmc = createTmcProcess(name);
                            // add tmc under opBuy by adding a pseudo-node: we will attach Process as child of opBuy via Tree building later
                            // but to keep model simple — attach tmc as a process inside the part and mark that it should be consumed by opBuy
                            // we'll add tmc as a process with type "TMC" and later, while building TreeItem, place under opBuy if necessary.
                            part.addProcess(tmc);
                            // to indicate consumption target, we create an operation placeholder? Simpler: we will attach tmc to opBuy at Tree generation time.
                        }
                    }
                    // Производство
                    if (!prod.isEmpty()) {
                        Operation opProd = new Operation("Производство", prod, "Цех");
                        ut.addOperation(opProd);
                        if (hasKTPP) {
                            Process tmc = createTmcProcess(name);
                            part.addProcess(tmc);
                        }
                    }
                    part.addProcess(ut);
                }
                // --- Нет UT, но есть КД/ТД => создаём ТМЦ-проект как отдельный процесс
                else if (hasKTPP) {
                    Process tmc = createTmcProcess(name);
                    part.addProcess(tmc);
                }

                // --- Закупка отдельная логика: если только buy и это закупаемая часть - создадим process type "Зак"
                if (!buy.isEmpty() && prod.isEmpty()) {
                    Process zak = new Process("Зак " + name, "Зак");
                    zak.addOperation(new Operation("Закупка", buy, "УЗ"));
                    part.addProcess(zak);
                }

                // сохранить в map
                nodeMap.put(code, part);
            }
        }
        return nodeMap;
    }

    private static Process createTmcProcess(String name) {
        Process tmc = new Process("ТМЦ-П " + name, "TMC");
        // NOTE: Нормы КД/ТД будут заполнены не здесь — нужно, чтобы parser сохранил kd/td в Process/Operation.
        // Для простоты — оставляем операции пустыми и в более точной реализации нужно передать kd/td при создании TMC.
        // Но ниже мы будем строить Tree и подставлять операции (в следующем классе).
        return tmc;
    }

    private static String getCellString(org.apache.poi.ss.usermodel.Cell c) {
        if (c == null) return "";
        if (c.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) return c.getStringCellValue();
        if (c.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC) {
            double v = c.getNumericCellValue();
            if (v == (long) v) return String.valueOf((long) v);
            return String.valueOf(v);
        }
        return "";
    }
}
