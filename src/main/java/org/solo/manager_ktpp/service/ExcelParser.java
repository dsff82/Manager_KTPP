package org.solo.manager_ktpp.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.solo.manager_ktpp.model.*;
import org.solo.manager_ktpp.model.Process;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class ExcelParser {

    /**
     * Главный метод
     */
    public static Part parseExcel(File file) throws IOException {

        Workbook wb;
        try (FileInputStream fis = new FileInputStream(file)) {
            wb = new XSSFWorkbook(fis);
        }

        Sheet sheet = wb.getSheetAt(0);

        // Корень структуры
        Part root = new Part("Производственная структура", false);

        // Для восстановления дерева по уровням
        Map<String, Part> levelMap = new HashMap<>();

        for (Row row : sheet) {

            if (row.getRowNum() <= 3) continue; // 1–4 строки — шапка

            String levelStr = getStringCell(row, 0);     // Код уровня
            String name     = getStringCell(row, 1);     // Наименование
            String deptKD   = getStringCell(row, 2);     // Подразделение КД

            double kdNorm   = getNumericCell(row, 3);    // Норма КД
            double tdNorm   = getNumericCell(row, 4);    // Норма ТД

            double buyNorm  = getNumericCell(row, 5);    // Норма УТ — Закупка
            double prodNorm = getNumericCell(row, 6);    // Норма УТ — Производство

            if (name.isEmpty() || levelStr.isEmpty()) continue;

            boolean hasKD = kdNorm > 0;
            boolean hasTD = tdNorm > 0;
            boolean hasKTPP = hasKD || hasTD;

            // Создаём Part
            Part part = new Part(name, hasKTPP);

            // --- Встраиваем в иерархию по уровню ---
            int level = levelStr.split("\\.").length;

            if (level == 1) {
                root.addChild(part);
            } else {
                String parentKey = parentLevel(levelStr);
                Part parent = levelMap.get(parentKey);
                if (parent != null) parent.addChild(part);
            }

            levelMap.put(levelStr, part);


            // ======================================================================
            //  1) Укрупнённый маршрут УТ
            // ======================================================================
            if (buyNorm > 0 || prodNorm > 0) {

                Process ut = new Process("УТ " + name, Process.ProcessType.UT);

                if (buyNorm > 0) {
                    ut.addOperation(
                            new Operation(
                                    Operation.OperationType.ZAKUPKA,
                                    buyNorm,
                                    Operation.Department.UZ
                            )
                    );
                }

                if (prodNorm > 0) {
                    ut.addOperation(
                            new Operation(
                                    Operation.OperationType.PROIZVODSTVO,
                                    prodNorm,
                                    Operation.Department.TSEH
                            )
                    );
                }

                part.addProcess(ut);
            }


            // ======================================================================
            //  2) Процесс "Зак" если есть только закупка
            // ======================================================================
            if (buyNorm > 0 && prodNorm == 0) {

                Process zakPr = new Process("Зак " + name, Process.ProcessType.ZAK);

                zakPr.addOperation(
                        new Operation(
                                Operation.OperationType.ZAKUPKA,
                                buyNorm,
                                Operation.Department.UZ
                        )
                );

                part.addProcess(zakPr);
            }


            // ======================================================================
            //  3) Генерация ТМЦ-проектов (КД/ТД)
            // ======================================================================
            if (hasKTPP) {

                TmcProjectPart tmc = new TmcProjectPart("ТМЦ-П " + name, null);

                Process tmcProc = new Process("ТМЦ-П " + name, Process.ProcessType.TMC_P);

                if (hasKD) {
                    tmcProc.addOperation(
                            new Operation(
                                    Operation.OperationType.KD,
                                    kdNorm,
                                    parseDept(deptKD)
                            )
                    );
                }

                if (hasTD) {
                    tmcProc.addOperation(
                            new Operation(
                                    Operation.OperationType.TD,
                                    tdNorm,
                                    Operation.Department.OGT
                            )
                    );
                }

                tmc.addProcess(tmcProc);

                // ***** ВАЖНО *****
                // ТМЦ-проект навешиваем ТОЛЬКО на производство
                if (prodNorm > 0) {
                    tmc.setConsumedBy(Operation.OperationType.PROIZVODSTVO);
                    part.addChild(tmc);
                }
            }

        }

        wb.close();
        return root;
    }


    // =========================================================================
    // ========================  UTILS  ========================================
    // =========================================================================

    /** Безопасное получение строки */
    private static String getStringCell(Row row, int col) {
        Cell c = row.getCell(col);
        if (c == null) return "";

        switch (c.getCellType()) {
            case STRING:  return c.getStringCellValue().trim();
            case NUMERIC: return String.valueOf(c.getNumericCellValue());
            default:      return "";
        }
    }

    /** Чистый безопасный numeric: извлекает число даже если там мусор типа "400 мин" */
    private static double getNumericCell(Row row, int col) {
        Cell c = row.getCell(col);
        if (c == null) return 0;

        try {
            switch (c.getCellType()) {
                case NUMERIC:
                    return c.getNumericCellValue();

                case STRING:
                    return parseDoubleSafe(c.getStringCellValue());

                case FORMULA:
                    try {
                        return c.getNumericCellValue();
                    } catch (Exception ex) {
                        return parseDoubleSafe(c.getStringCellValue());
                    }

                default:
                    return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    /** Парсер удаляет всё, кроме цифр и точки */
    private static double parseDoubleSafe(String raw) {
        if (raw == null) return 0;
        String cleaned = raw.replaceAll("[^0-9.]", "");
        if (cleaned.isEmpty()) return 0;
        try {
            return Double.parseDouble(cleaned);
        } catch (Exception e) {
            return 0;
        }
    }

    private static String parentLevel(String level) {
        int idx = level.lastIndexOf(".");
        return (idx == -1) ? "" : level.substring(0, idx);
    }

    private static Operation.Department parseDept(String s) {
        if (s == null) return Operation.Department.NONE;
        return switch (s.trim().toUpperCase()) {
            case "ОГТ" -> Operation.Department.OGT;
            case "УЗ"  -> Operation.Department.UZ;
            case "ЦЕХ" -> Operation.Department.TSEH;
            default    -> Operation.Department.NONE;
        };
    }
}
