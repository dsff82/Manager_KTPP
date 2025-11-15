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

    private static final List<String> LOG = new ArrayList<>();

    public static List<String> getLog() {
        return LOG;
    }

    private static void log(String msg) {
        System.out.println(msg);
        LOG.add(msg);
    }

    public static Part parseExcel(File file) throws IOException {

        LOG.clear();
        log("=== Начинаем парсинг Excel ===");

        Workbook wb;
        try (FileInputStream fis = new FileInputStream(file)) {
            wb = new XSSFWorkbook(fis);
        }

        Sheet sheet = wb.getSheetAt(0);

        Part root = new Part("Производственная структура", false);

        // карта всех уровней
        Map<String, Part> levelMap = new HashMap<>();
        levelMap.put("ROOT", root);

        for (Row row : sheet) {

            if (row.getRowNum() <= 3) continue;

            String levelStr = getStringCell(row, 0);
            String name     = getStringCell(row, 1);
            String deptKD   = getStringCell(row, 2);

            double kdNorm   = getNumericCell(row, 3);
            double tdNorm   = getNumericCell(row, 4);
            double buyNorm  = getNumericCell(row, 5);
            double prodNorm = getNumericCell(row, 6);

            if (name.isEmpty() || levelStr.isEmpty()) continue;

            log("---- Строка " + row.getRowNum() + " -------------------");
            log("Уровень = " + levelStr + ", имя = " + name);

            boolean hasKD = kdNorm > 0;
            boolean hasTD = tdNorm > 0;
            boolean hasKTPP = hasKD || hasTD;

            Part part = new Part(name, hasKTPP);

            // === восстановление всех родителей ===
            String[] levels = levelStr.split("\\.");

            String path = "";
            for (int i = 0; i < levels.length; i++) {
                path = (i == 0) ? levels[0] : path + "." + levels[i];
                if (!levelMap.containsKey(path)) {

                    if (i == 0) {
                        // прямой сын root
                        root.addChild(part);
                        log("Добавлен в root: " + name);
                    } else {
                        String parentPath = path.substring(0, path.lastIndexOf("."));
                        Part parent = levelMap.get(parentPath);
                        if (parent != null) {
                            parent.addChild(part);
                            log("Добавлен в " + parent.getName() + ": " + name);
                        } else {
                            log("ОШИБКА: нет родителя " + parentPath + " для " + name);
                        }
                    }
                    levelMap.put(path, part);
                }
            }

            // ======================================================================
            // 1) УТ
            // ======================================================================
            if (buyNorm > 0 || prodNorm > 0) {

                log("Создаём процесс УТ");

                Process ut = new Process("УТ " + name, Process.ProcessType.UT);

                if (buyNorm > 0) {
                    log("  операция ZAKUPKA: " + buyNorm);
                    ut.addOperation(new Operation(
                            Operation.OperationType.ZAKUPKA,
                            buyNorm,
                            Operation.Department.UZ
                    ));
                }

                if (prodNorm > 0) {
                    log("  операция PROIZVODSTVO: " + prodNorm);
                    ut.addOperation(new Operation(
                            Operation.OperationType.PROIZVODSTVO,
                            prodNorm,
                            Operation.Department.TSEH
                    ));
                }

                part.addProcess(ut);
            }

            // ======================================================================
            // 2) Зак
            // ======================================================================
            if (buyNorm > 0 && prodNorm == 0) {
                log("Создаём процесс ЗАК");

                Process zak = new Process("Зак " + name, Process.ProcessType.ZAK);
                zak.addOperation(new Operation(
                        Operation.OperationType.ZAKUPKA,
                        buyNorm,
                        Operation.Department.UZ
                ));
                part.addProcess(zak);
            }

            // ======================================================================
            // 3) ТМЦ
            // ======================================================================
            if (hasKTPP) {

                log("Создаём ТМЦ-проект");

                TmcProjectPart tmc = new TmcProjectPart("ТМЦ-П " + name, null);
                Process tmcProc = new Process("ТМЦ-П " + name, Process.ProcessType.TMC_P);

                if (hasKD) {
                    log("  KD: " + kdNorm);
                    tmcProc.addOperation(new Operation(
                            Operation.OperationType.KD,
                            kdNorm,
                            parseDept(deptKD)
                    ));
                }

                if (hasTD) {
                    log("  TD: " + tdNorm);
                    tmcProc.addOperation(new Operation(
                            Operation.OperationType.TD,
                            tdNorm,
                            Operation.Department.OGT
                    ));
                }

                tmc.addProcess(tmcProc);

                if (prodNorm > 0) {
                    tmc.setConsumedBy(Operation.OperationType.PROIZVODSTVO);
                    part.addChild(tmc);

                    log("  ТМЦ прикреплён под ПРОИЗВОДСТВО");
                }
            }
        }

        wb.close();
        log("=== Парсинг завершён ===");

        return root;
    }

    // =============== UTILS ====================================================

    private static String getStringCell(Row row, int col) {
        Cell c = row.getCell(col);
        if (c == null) return "";
        return switch (c.getCellType()) {
            case STRING -> c.getStringCellValue().trim();
            case NUMERIC -> String.valueOf(c.getNumericCellValue());
            default -> "";
        };
    }

    private static double getNumericCell(Row row, int col) {
        Cell c = row.getCell(col);
        if (c == null) return 0;

        try {
            return switch (c.getCellType()) {
                case NUMERIC -> c.getNumericCellValue();
                case STRING -> parseDoubleSafe(c.getStringCellValue());
                default -> 0;
            };
        } catch (Exception e) {
            return 0;
        }
    }

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

    private static Operation.Department parseDept(String s) {
        if (s == null) return Operation.Department.NONE;
        return switch (s.trim().toUpperCase()) {
            case "ОГТ" -> Operation.Department.OGT;
            case "УЗ" -> Operation.Department.UZ;
            case "ЦЕХ" -> Operation.Department.TSEH;
            default -> Operation.Department.NONE;
        };
    }
}
