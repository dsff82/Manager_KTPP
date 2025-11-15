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

        Map<String, Part> levelMap = new HashMap<>();
        levelMap.put("ROOT", root);

        for (Row row : sheet) {

            if (row.getRowNum() <= 3) continue; // заголовки

            String levelStr = getStringCell(row, 0);
            String name = getStringCell(row, 1);

            if (name.isEmpty() || levelStr.isEmpty()) {
                continue;
            }

            String deptKD = getStringCell(row, 2);

            double kdNorm = getNumericCell(row, 3);
            double tdNorm = getNumericCell(row, 4);
            double buyNorm = getNumericCell(row, 5);
            double prodNorm = getNumericCell(row, 6);

            // Бывают строки "Итого" — пропускаем
            if (name.toLowerCase().contains("итог")) continue;

            boolean hasKD = kdNorm > 0;
            boolean hasTD = tdNorm > 0;
            boolean hasKTPP = hasKD || hasTD;

            log("---- Строка " + row.getRowNum() + " --------------------");
            log("Уровень = " + levelStr + ", имя = " + name);

            Part part = new Part(name, hasKTPP);

            // ================= ИЕРАРХИЯ ======================================
            String[] levels = levelStr.split("\\.");
            String fullPath = String.join(".", levels);

            String parentPath = (levels.length == 1)
                    ? "ROOT"
                    : fullPath.substring(0, fullPath.lastIndexOf("."));

            Part parent = levelMap.getOrDefault(parentPath, root);
            parent.addChild(part);
            levelMap.put(fullPath, part);

            log("Добавлен в: " + parent.getName() + " → " + name);

            // =================== ПОКУПНАЯ ЧАСТЬ =============================
            boolean isPokupnaya = (prodNorm <= 0) && (buyNorm > 0);

            if (isPokupnaya) {
                createPurchaseLogic(part, name, kdNorm, tdNorm, buyNorm, deptKD, hasKD, hasTD);
                continue;
            }

            // =================== УТ ДЛЯ ПРОИЗВОДСТВА ========================
            if (buyNorm > 0 || prodNorm > 0) {
                createUTProcess(part, name, buyNorm, prodNorm);
            }

            // =================== ТМЦ-П ДЛЯ ОТДЕЛОВ ==========================
            if (hasKTPP) {
                createTmcLogic(part, name, kdNorm, tdNorm, deptKD, prodNorm > 0);
            }
        }

        wb.close();
        log("=== Парсинг завершён ===");
        return root;
    }

    // ======================================================================
    private static void createPurchaseLogic(Part part, String name,
                                            double kdNorm, double tdNorm,
                                            double buyNorm, String deptKD,
                                            boolean hasKD, boolean hasTD) {

        log("→ Покупная часть: " + name);

        Process zak = new Process("Зак " + name, Process.ProcessType.ZAK);
        zak.addOperation(new Operation(
                Operation.OperationType.ZAKUPKA,
                buyNorm,
                Operation.Department.UZ
        ));

        part.addProcess(zak);

        if (hasKD || hasTD) {
            createTmcLogic(part, name, kdNorm, tdNorm, deptKD, false);
            log("  ТМЦ-П привязан к закупке");
        }
    }

    private static void createUTProcess(Part part, String name,
                                        double buyNorm, double prodNorm) {
        log("Создаём процесс УТ");

        Process ut = new Process("УТ " + name, Process.ProcessType.UT);

        if (buyNorm > 0) {
            ut.addOperation(new Operation(
                    Operation.OperationType.ZAKUPKA,
                    buyNorm,
                    Operation.Department.UZ
            ));
        }

        if (prodNorm > 0) {
            ut.addOperation(new Operation(
                    Operation.OperationType.PROIZVODSTVO,
                    prodNorm,
                    Operation.Department.TSEH // TODO: возможно из Excel
            ));
        }

        part.addProcess(ut);
    }

    private static void createTmcLogic(Part part, String name,
                                       double kdNorm, double tdNorm,
                                       String deptKD,
                                       boolean linkToProd) {

        log("Создаём ТМЦ-П");

        TmcProjectPart tmc = new TmcProjectPart("ТМЦ-П " + name, null);
        Process tmcProc = new Process("ТМЦ-П " + name, Process.ProcessType.TMC_P);

        if (kdNorm > 0) {
            tmcProc.addOperation(new Operation(
                    Operation.OperationType.KD,
                    kdNorm,
                    parseDept(deptKD)
            ));
        }

        if (tdNorm > 0) {
            tmcProc.addOperation(new Operation(
                    Operation.OperationType.TD,
                    tdNorm,
                    Operation.Department.OGT
            ));
        }

        tmc.addProcess(tmcProc);

        if (linkToProd) {
            tmc.setConsumedBy(Operation.OperationType.PROIZVODSTVO);
        } else {
            tmc.setConsumedBy(Operation.OperationType.ZAKUPKA);
        }

        part.addChild(tmc);
    }

    // ======================= UTILS =======================================

    private static String getStringCell(Row row, int col) {
        Cell c = row.getCell(col);
        if (c == null) return "";
        return c.getCellType() == CellType.STRING
                ? c.getStringCellValue().trim()
                : String.valueOf(getNumericCell(row, col));
    }

    private static double getNumericCell(Row row, int col) {
        try {
            Cell c = row.getCell(col);
            if (c == null) return 0;
            return c.getCellType() == CellType.NUMERIC
                    ? c.getNumericCellValue()
                    : parseDoubleSafe(c.getStringCellValue());
        } catch (Exception e) {
            return 0;
        }
    }

    private static double parseDoubleSafe(String raw) {
        if (raw == null) return 0;
        String cleaned = raw.replaceAll("[^0-9.,]", "").replace(",", ".");
        try { return Double.parseDouble(cleaned); }
        catch (Exception e) { return 0; }
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
