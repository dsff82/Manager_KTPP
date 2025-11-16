package org.solo.manager_ktpp;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.solo.manager_ktpp.service.ExcelParser;
import org.solo.manager_ktpp.model.Part;
import org.solo.manager_ktpp.model.TmcProjectPart;
import org.solo.manager_ktpp.model.Process;
import org.solo.manager_ktpp.model.Operation;
import org.solo.manager_ktpp.ui.RowModel;
import org.solo.manager_ktpp.ui.TreeBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ExcelTreeController {

    @FXML private Label welcomeText;
    @FXML private TreeView<Object> treeView;

    // --- таблица ---
    @FXML private TableView<RowModel> table;
    @FXML private TableColumn<RowModel, String> colType;
    @FXML private TableColumn<RowModel, String> colName;
    @FXML private TableColumn<RowModel, String> colNorm;
    @FXML private TableColumn<RowModel, String> colDept;
    @FXML private TableColumn<RowModel, String> colConsumes;

    private Part parsedRoot;

    @FXML
    public void initialize() {

        // Настраиваем колонки таблицы
        colType.setCellValueFactory(c -> c.getValue().typeProperty());
        colName.setCellValueFactory(c -> c.getValue().nameProperty());
        colNorm.setCellValueFactory(c -> c.getValue().normProperty());
        colDept.setCellValueFactory(c -> c.getValue().deptProperty());
        colConsumes.setCellValueFactory(c -> c.getValue().consumedByProperty());

        // Выбор строки таблицы → выбор узла в дереве
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldRow, newRow) -> {
            if (newRow == null) return;
            Object model = newRow.getModelObject();
            selectNodeInTree(model);
        });

        // Выбор дерева → выбор строки таблицы
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (newItem == null) return;
            Object model = ((org.solo.manager_ktpp.model.HierarchyNode)newItem.getValue()).getValue();
            selectRowInTable(model);
        });
    }

    @FXML
    private void onOpenExcel() {

        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel files", "*.xlsx"));

        File file = fc.showOpenDialog(null);
        if (file == null) return;

        welcomeText.setText("Загружается: " + file.getName());

        try {
            parsedRoot = ExcelParser.parseExcel(file);

            TreeItem<Object> rootNode = TreeBuilder.buildTree(parsedRoot);
            treeView.setRoot(rootNode);
            treeView.setShowRoot(true);
            expandAll(rootNode);

            rebuildTable(parsedRoot);

            welcomeText.setText("Загружено: " + file.getName());

        } catch (Exception e) {
            e.printStackTrace();
            welcomeText.setText("Ошибка загрузки");
        }
    }

    private void rebuildTable(Part root) {
        List<RowModel> list = new ArrayList<>();
        scanPart(root, list);
        table.getItems().setAll(list);
    }

    private void scanPart(Part p, List<RowModel> out) {

        out.add(RowModel.of(p));

        // Процессы
        for (var pr : p.getProcesses()) {
            out.add(RowModel.of(pr));

            for (var op : pr.getOperations()) {
                out.add(RowModel.of(op));

                // добавить потребляемые TMC (чтобы они были в таблице под операцией)
                for (TmcProjectPart tmc : op.getConsumedParts()) {
                    out.add(RowModel.of(tmc));

                    // внутри ТМЦ показываем её процессы/операции (КД/ТД)
                    for (var tpr : tmc.getProcesses()) {
                        out.add(RowModel.of(tpr));
                        for (var top : tpr.getOperations()) {
                            out.add(RowModel.of(top));
                        }
                    }
                }
            }
        }

        // Дети
        for (Part c : p.getChildren()) {
            // дочерняя деталь как отдельный блок
            out.add(RowModel.of(c));

            for (var pr : c.getProcesses()) {
                out.add(RowModel.of(pr));
                for (var op : pr.getOperations())
                    out.add(RowModel.of(op));
            }

            scanPart(c, out);
        }
    }

    private void selectNodeInTree(Object target) {
        expandAll(treeView.getRoot());
        TreeItem<Object> found = findNode(treeView.getRoot(), target);
        if (found != null) {
            treeView.getSelectionModel().select(found);
            treeView.scrollTo(treeView.getSelectionModel().getSelectedIndex());
        }
    }

    private TreeItem<Object> findNode(TreeItem<?> item, Object target) {
        if (item == null) return null;

        Object val = item.getValue();
        if (val instanceof org.solo.manager_ktpp.model.HierarchyNode node) {
            if (node.getValue() == target)
                return (TreeItem<Object>) item;
        }

        for (TreeItem<?> child : item.getChildren()) {
            TreeItem<Object> r = findNode(child, target);
            if (r != null) return r;
        }
        return null;
    }

    private void selectRowInTable(Object model) {
        for (RowModel r : table.getItems()) {
            if (r.getModelObject() == model) {
                table.getSelectionModel().select(r);
                table.scrollTo(r);
                break;
            }
        }
    }

    @FXML
    private void onExpandAll() {
        expandAll(treeView.getRoot());
    }

    @FXML
    private void onExpandToLeaves() {
        expandToLeaves(treeView.getRoot());
    }

    private void expandAll(TreeItem<?> item) {
        if (item == null) return;
        item.setExpanded(true);
        for (TreeItem<?> c : item.getChildren())
            expandAll(c);
    }

    private void expandToLeaves(TreeItem<?> item) {
        if (item == null) return;

        if (!item.getChildren().isEmpty()) {
            item.setExpanded(true);
            for (TreeItem<?> c : item.getChildren())
                expandToLeaves(c);
        }
    }
}
