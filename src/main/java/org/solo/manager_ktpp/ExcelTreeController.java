package org.solo.manager_ktpp;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.solo.manager_ktpp.model.Operation;
import org.solo.manager_ktpp.model.Part;
import org.solo.manager_ktpp.model.Process;
import org.solo.manager_ktpp.parser.ExcelParser;
import org.apache.poi.ss.usermodel.*;

import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class ExcelTreeController {

    @FXML private Label welcomeText;
    @FXML private TreeView<String> treeView;

    @FXML
    protected void onOpenExcel(ActionEvent event) {
        Stage stage = (Stage)((javafx.scene.control.Button)event.getSource()).getScene().getWindow();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Выберите Excel-файл");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel files", "*.xlsx"));
        File file = chooser.showOpenDialog(stage);
        if (file == null) return;

        try {
            // Для полной информации о KD/TD и привязках нам нужно прочитать все колонки в памяти
            Map<String, Part> partMap = parseDetailed(file);

            // Строим иерархию: вставляем детей в родителей по коду
            TreeItem<String> root = new TreeItem<>("🏭 Производственная структура");
            root.setExpanded(true);

            for (Map.Entry<String, Part> e : partMap.entrySet()) {
                String code = e.getKey();
                Part p = e.getValue();
                if (!code.contains(".")) {
                    root.getChildren().add(buildPartNode(p, partMap));
                } else {
                    String parentCode = code.substring(0, code.lastIndexOf('.'));
                    Part parent = partMap.get(parentCode);
                    if (parent != null) {
                        // добавление произойдёт при рекурсивном построении — здесь пропускаем
                    } else {
                        // если родителя нет — на верхний уровень
                        root.getChildren().add(buildPartNode(p, partMap));
                    }
                }
            }

            // Более корректно — пройти по map и добавлять детей к родителю:
            for (Map.Entry<String, Part> e : partMap.entrySet()) {
                String code = e.getKey();
                Part node = e.getValue();
                if (code.contains(".")) {
                    String parentCode = code.substring(0, code.lastIndexOf('.'));
                    Part parent = partMap.get(parentCode);
                    if (parent != null) parent.addChild(node);
                }
            }

            // После установки детей — добавляем корневые
            root.getChildren().clear();
            for (Map.Entry<String, Part> e : partMap.entrySet()) {
                String code = e.getKey();
                Part p = e.getValue();
                if (!code.contains(".")) root.getChildren().add(buildPartNode(p, partMap));
            }

            treeView.setRoot(root);
            welcomeText.setText("Загружен: " + file.getName());

        } catch (Exception ex) {
            ex.printStackTrace();
            welcomeText.setText("Ошибка: " + ex.getMessage());
        }
    }

    /**
     * Более детальный парсинг: читает kd/td и сохраняет операции внутри TMC процессов
     */
    private Map<String, Part> parseDetailed(File file) throws Exception {
        Map<String, Part> map = new LinkedHashMap<>();
        try (FileInputStream fis = new FileInputStream(file);
             Workbook wb = new XSSFWorkbook(fis)) {
            Sheet sheet = wb.getSheetAt(0);
            for (Row row : sheet) {
                int rn = row.getRowNum();
                if (rn < 4) continue;

                String code = cellToString(row.getCell(0)).trim();
                if (code.isEmpty()) continue;
                String name = cellToString(row.getCell(1)).trim();
                String dept = cellToString(row.getCell(2)).trim();
                String kd = cellToString(row.getCell(3)).trim();
                String td = cellToString(row.getCell(4)).trim();
                String buy = cellToString(row.getCell(5)).trim();
                String prod = cellToString(row.getCell(6)).trim();

                boolean hasKTPP = !kd.isEmpty() || !td.isEmpty();
                Part part = new Part(code, name, hasKTPP);

                // если есть покупка/производство — создаём УТ
                if (!buy.isEmpty() || !prod.isEmpty()) {
                    Process ut = new Process("УТ " + name, "УТ");
                    if (!buy.isEmpty()) {
                        ut.addOperation(new Operation("Закупка", buy, "УЗ"));
                    }
                    if (!prod.isEmpty()) {
                        ut.addOperation(new Operation("Производство", prod, "Цех"));
                    }
                    part.addProcess(ut);
                } else if (!kd.isEmpty() || !td.isEmpty()) {
                    // нет УТ, но есть КД/ТД — отдельный ТМЦ процесс
                    Process tmc = new Process("ТМЦ-П " + name, "TMC");
                    if (!kd.isEmpty()) tmc.addOperation(new Operation("КД", kd, dept));
                    if (!td.isEmpty()) tmc.addOperation(new Operation("ТД", td, "ОГТ"));
                    part.addProcess(tmc);
                }

                // если есть КД/ТД и в любом случае нужно тмц процесс (если kd/td есть)
                if (hasKTPP) {
                    // ensure TMC exists (if already added above for no UT, skip)
                    boolean hasTmc = part.getProcesses().stream().anyMatch(pr -> "TMC".equals(pr.getType()));
                    if (!hasTmc) {
                        Process tmc = new Process("ТМЦ-П " + name, "TMC");
                        if (!kd.isEmpty()) tmc.addOperation(new Operation("КД", kd, dept));
                        if (!td.isEmpty()) tmc.addOperation(new Operation("ТД", td, "ОГТ"));
                        part.addProcess(tmc);
                    }
                }

                // if only buy and no prod => also create Zak process
                if (!buy.isEmpty() && prod.isEmpty()) {
                    Process zak = new Process("Зак " + name, "Зак");
                    zak.addOperation(new Operation("Закупка", buy, "УЗ"));
                    part.addProcess(zak);
                }

                map.put(code, part);
            }
        }
        return map;
    }

    private TreeItem<String> buildPartNode(Part p, Map<String, Part> map) {
        TreeItem<String> node = new TreeItem<>("★ " + p.getName());
        node.setExpanded(true);

        // добавляем процессы в определённом порядке: сначала УТ (если есть), затем ТМЦ, затем Зак
        for (Process pr : p.getProcesses()) {
            if ("УТ".equals(pr.getType())) {
                // Укрупнённый маршрут — в нём операции; при операции Производство — добавляем ТМЦ (если есть)
                TreeItem<String> utItem = new TreeItem<>("⧉ " + pr.getName());
                utItem.setExpanded(true);
                for (Operation op : pr.getOperations()) {
                    TreeItem<String> opItem = op.toTreeItem();
                    // если операция — Производство или Закупка и есть TMC processes — прикрепляем их как дети
                    if ("Производство".equals(op.getName()) || "Закупка".equals(op.getName())) {
                        // attach TMC if any
                        for (Process tmc : p.getProcesses()) {
                            if ("TMC".equals(tmc.getType())) {
                                // attach tmc under operation
                                TreeItem<String> tmcItem = new TreeItem<>("📦 " + tmc.getName());
                                tmcItem.setExpanded(true);
                                for (Operation tmcOp : tmc.getOperations()) tmcItem.getChildren().add(tmcOp.toTreeItem());
                                opItem.getChildren().add(tmcItem);
                            }
                        }
                    }
                    utItem.getChildren().add(opItem);
                }
                node.getChildren().add(utItem);
            }
        }

        // добавить TMC как отдельный процесс, если были только KD/TD (и не были добавлены в UT above)
        for (Process pr : p.getProcesses()) {
            if ("TMC".equals(pr.getType())) {
                // если уже прикрепили к операциям UT — возможно дублирование; но чтобы избежать дублирования,
                // проверим что TMC не были добавлены ранее: если in UT we already added, skip standalone
                boolean addedInUT = p.getProcesses().stream().anyMatch(x -> "УТ".equals(x.getType()) && !x.getOperations().isEmpty());
                if (!addedInUT) {
                    TreeItem<String> tmcItem = new TreeItem<>("📦 " + pr.getName());
                    tmcItem.setExpanded(true);
                    for (Operation op : pr.getOperations()) tmcItem.getChildren().add(op.toTreeItem());
                    node.getChildren().add(tmcItem);
                } else {
                    // если UT есть, we already attached TMC under ops, but to avoid double attaching, we skip
                }
            }
        }

        // Зак процессы (если отдельно)
        for (Process pr : p.getProcesses()) {
            if ("Зак".equals(pr.getType())) node.getChildren().add(pr.toTreeItem());
        }

        // добавить реальные дочерние части (с рекурсией)
        for (Part child : p.getChildren()) {
            node.getChildren().add(buildPartNode(child, map));
        }
        return node;
    }

    private String cellToString(Cell c) {
        if (c == null) return "";
        if (c.getCellType() == CellType.STRING) return c.getStringCellValue();
        if (c.getCellType() == CellType.NUMERIC) {
            double v = c.getNumericCellValue();
            if (v == (long) v) return String.valueOf((long) v);
            return String.valueOf(v);
        }
        return "";
    }
}
