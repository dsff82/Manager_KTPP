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

    public static Part parseExcel(File file) throws IOException {

        FileInputStream fis = new FileInputStream(file);
        Workbook wb = new XSSFWorkbook(fis);
        Sheet sheet = wb.getSheetAt(0);

        // Корневой Part
        Part root = new Part("Производственная структура", false);

        // Для удобства восстановления иерархии
        Map<String, Part> levelMap = new HashMap<>();

        for (Row row : sheet) {

            if (row.getRowNum() <= 3) continue; // первые 4 строки – шапка

            // ---- Чтение ячеек ----
            String levelStr = getCell(row, 0);
            String name = getCell(row, 1);
            String deptKD = getCell(row, 2);

            String kdStr = getCell(row, 3);
            String tdStr = getCell(row, 4);

            String buyStr = getCell(row, 5);
            String prodStr = getCell(row, 6);

            if (name.isEmpty()) continue;
            if (levelStr.isEmpty()) continue;

            // ---- Парсим уровень вложенности ----
            int level = levelStr.split("\\.").length;

            boolean hasKTPP = (!kdStr.isEmpty() || !tdStr.isEmpty());
            Part part = new Part(name, hasKTPP);

            // ---- Включаем в иерархию ----
            if (level == 1) {
                root.addChild(part);
            } else {
                String parentKey = parentLevel(levelStr);
                Part parent = levelMap.get(parentKey);
                if (parent != null) parent.addChild(part);
            }

            levelMap.put(levelStr, part);

            // ---- ГЕНЕРАЦИЯ Укрупнённого маршрута ----
            if (!buyStr.isEmpty() || !prodStr.isEmpty()) {

                Process pr = new Process("УТ " + name, Process.ProcessType.UT);

                // Закупка
                if (!buyStr.isEmpty()) {
                    Operation op = new Operation(
                            Operation.OperationType.ZAKUPKA,
                            Double.parseDouble(buyStr),
                            Operation.Department.UZ
                    );
                    pr.addOperation(op);
                }

                // Производство
                if (!prodStr.isEmpty()) {
                    Operation op = new Operation(
                            Operation.OperationType.PROIZVODSTVO,
                            Double.parseDouble(prodStr),
                            Operation.Department.TSEH
                    );
                    pr.addOperation(op);

                    // Если есть КД/ТД → генерируем ТМЦ-проект
                    if (hasKTPP) {
                        TmcProjectPart tmc = new TmcProjectPart("ТМЦ-П " + name, Operation.OperationType.PROIZVODSTVO);

                        Process tmcProc = new Process("ТМЦ-П " + name, Process.ProcessType.TMC_P);

                        // КД
                        if (!kdStr.isEmpty()) {
                            Operation opKD = new Operation(
                                    Operation.OperationType.KD,
                                    Double.parseDouble(kdStr),
                                    parseDept(deptKD)
                            );
                            tmcProc.addOperation(opKD);
                        }

                        // ТД
                        if (!tdStr.isEmpty()) {
                            Operation opTD = new Operation(
                                    Operation.OperationType.TD,
                                    Double.parseDouble(tdStr),
                                    Operation.Department.OGT
                            );
                            tmcProc.addOperation(opTD);
                        }

                        tmc.addProcess(tmcProc);

                        // Добавить ТМЦ-проект как ребенка операции Производство
                        part.addChild(tmc);
                    }
                }

                part.addProcess(pr);
            }

            // ---- Если нет производства, но есть закупка → техпроцесс Зак ----
            if (!prodStr.isEmpty() == false && !buyStr.isEmpty()) {

                Process zak = new Process("Зак " + name, Process.ProcessType.ZAK);

                Operation op = new Operation(
                        Operation.OperationType.ZAKUPKA,
                        Double.parseDouble(buyStr),
                        Operation.Department.UZ
                );
                zak.addOperation(op);

                // ТМЦ-проект на закупку, если есть КД/ТД
                if (hasKTPP) {
                    TmcProjectPart tmc = new TmcProjectPart("ТМЦ-П " + name, Operation.OperationType.ZAKUPKA);
                    Process tmcProc = new Process("ТМЦ-П " + name, Process.ProcessType.TMC_P);

                    if (!kdStr.isEmpty()) {
                        tmcProc.addOperation(new Operation(
                                Operation.OperationType.KD,
                                Double.parseDouble(kdStr),
                                parseDept(deptKD)
                        ));
                    }
                    if (!tdStr.isEmpty()) {
                        tmcProc.addOperation(new Operation(
                                Operation.OperationType.TD,
                                Double.parseDouble(tdStr),
                                Operation.Department.OGT
                        ));
                    }

                    tmc.addProcess(tmcProc);
                    part.addChild(tmc);
                }

                part.addProcess(zak);
            }

        }

        wb.close();
        fis.close();
        return root;
    }

    // ---------------- UTIL ----------------------

    private static String getCell(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            default -> "";
        };
    }

    private static String parentLevel(String level) {
        int lastDot = level.lastIndexOf(".");
        return (lastDot == -1) ? "" : level.substring(0, lastDot);
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
