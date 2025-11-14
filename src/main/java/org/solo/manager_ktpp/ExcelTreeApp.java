package org.solo.manager_ktpp;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ExcelTreeApp extends Application {

    @Override
    public void start(Stage stage) {
        TreeItem<String> rootItem = new TreeItem<>("Производственная структура");
        TreeView<String> tree = new TreeView<>(rootItem);

        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel files (*.xlsx)", "*.xlsx")
        );

        stage.setTitle("Импорт структуры из Excel");
        stage.setWidth(900);
        stage.setHeight(700);

        stage.setScene(new Scene(new BorderPane(tree), 900, 700));
        stage.show();

        // Открываем Excel
        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            buildTreeFromExcel(file, rootItem);
        }
    }

    private void buildTreeFromExcel(File file, TreeItem<String> rootItem) {
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // пропустить заголовок

                String partName = getCellValue(row.getCell(0));
                String dept = getCellValue(row.getCell(1));
                String prodTime = getCellValue(row.getCell(2));
                String buyTime = getCellValue(row.getCell(3));
                String kdTime = getCellValue(row.getCell(4));
                String tdTime = getCellValue(row.getCell(5));
                String comment = getCellValue(row.getCell(6));

                // Узел детали
                TreeItem<String> partNode = new TreeItem<>("★ " + partName);

                // Укрупненный техпроцесс
                TreeItem<String> processNode = new TreeItem<>("⧉ Техпроцесс: Укрупнённый маршрут");
                processNode.getChildren().add(new TreeItem<>("⚙ Операция: Закупка (" + buyTime + " ч)"));
                processNode.getChildren().add(new TreeItem<>("⚙ Операция: Производство (" + prodTime + " ч)"));

                // ТМЦ-проект
                TreeItem<String> tmcNode = new TreeItem<>("📦 ТМЦ-проект: " + partName);
                TreeItem<String> kdNode = new TreeItem<>("⚙ Разработка КД (" + kdTime + " ч)");
                TreeItem<String> tdNode = new TreeItem<>("⚙ Разработка ТД (" + tdTime + " ч)");
                tmcNode.getChildren().addAll(kdNode, tdNode);

                processNode.getChildren().add(tmcNode);
                partNode.getChildren().add(processNode);
                rootItem.getChildren().add(partNode);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    public static void main(String[] args) {
        launch();
    }
}
