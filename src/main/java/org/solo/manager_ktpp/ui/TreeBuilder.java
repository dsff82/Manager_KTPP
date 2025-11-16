package org.solo.manager_ktpp.ui;

import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;
import org.solo.manager_ktpp.model.*;
import org.solo.manager_ktpp.model.Process;

public class TreeBuilder {

    public static TreeItem<Object> buildTree(Part root) {
        return buildPart(root);
    }

    private static TreeItem<Object> buildPart(Part part) {

        HierarchyNode node = new HierarchyNode(part.getName(), part, HierarchyNode.NodeType.PART);
        TreeItem<Object> partItem = treeItem(node);

        // Процессы
        for (Process pr : part.getProcesses()) {
            TreeItem<Object> prItem = buildProcess(pr);
            partItem.getChildren().add(prItem);
        }

        // Дочерние детали
        for (Part c : part.getChildren()) {
            partItem.getChildren().add(buildPart(c));
        }

        return partItem;
    }

    private static TreeItem<Object> buildProcess(Process pr) {

        HierarchyNode node = new HierarchyNode(
                pr.getName() + " (" + pr.getType() + ")",
                pr,
                HierarchyNode.NodeType.PROCESS);

        TreeItem<Object> item = treeItem(node);

        // Операции
        for (Operation op : pr.getOperations()) {

            HierarchyNode opNode = new HierarchyNode(
                    op.getType().name(),
                    op,
                    HierarchyNode.NodeType.OPERATION);

            TreeItem<Object> opItem = treeItem(opNode);

            // Добавляем в дерево потребляемые TMC как дочерние узлы операции
            for (TmcProjectPart tmc : op.getConsumedParts()) {
                TreeItem<Object> tmcItem = buildTmcPart(tmc);
                opItem.getChildren().add(tmcItem);
            }

            item.getChildren().add(opItem);
        }

        return item;
    }

    private static TreeItem<Object> buildTmcPart(TmcProjectPart tmc) {
        HierarchyNode node = new HierarchyNode(tmc.getName(), tmc, HierarchyNode.NodeType.TMC_PART);
        TreeItem<Object> item = treeItem(node);

        // внутри ТМЦ показываем её процессы (КД/ТД)
        for (Process pr : tmc.getProcesses()) {
            TreeItem<Object> prItem = buildProcess(pr);
            item.getChildren().add(prItem);
        }

        return item;
    }

    private static TreeItem<Object> treeItem(HierarchyNode node) {
        TreeItem<Object> item = new TreeItem<>(node);

        ImageView icon = (ImageView) node.getIcon();
        if (icon != null)
            item.setGraphic(icon);

        return item;
    }
}
