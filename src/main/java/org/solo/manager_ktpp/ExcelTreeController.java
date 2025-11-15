package org.solo.manager_ktpp;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.FileChooser;
import org.solo.manager_ktpp.model.Part;
import org.solo.manager_ktpp.service.ExcelParser;
import org.solo.manager_ktpp.ui.TreeBuilder;

import java.io.File;

public class ExcelTreeController {

    @FXML private Label welcomeText;
    @FXML private TreeView<Object> treeView;

    @FXML
    protected void onOpenExcel(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Выберите Excel-файл");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Excel files", "*.xlsx")
        );

        File file = chooser.showOpenDialog(treeView.getScene().getWindow());
        if (file == null) return;

        try {
            // ---- 1) Парсим Excel → получаем корневой Part ----
            Part rootPart = ExcelParser.parseExcel(file);

            // ---- 2) Построить UI-дерево по модели ----
            TreeItem<Object> rootNode = TreeBuilder.buildTree(rootPart);

            treeView.setRoot(rootNode);
            treeView.setShowRoot(true);
            treeView.getRoot().setExpanded(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
