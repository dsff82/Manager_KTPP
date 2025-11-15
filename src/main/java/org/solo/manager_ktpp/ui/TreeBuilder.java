package org.solo.manager_ktpp.ui;

import javafx.scene.control.TreeItem;
import org.solo.manager_ktpp.model.*;
import org.solo.manager_ktpp.model.Process;

public class TreeBuilder {

    public static TreeItem<Object> buildTree(Part root) {
        System.out.println("=== Построение дерева ===");
        return createPartNode(root);
    }

    // ======================================================================
    // PART NODE
    // ======================================================================
    private static TreeItem<Object> createPartNode(Part part) {

        System.out.println("Часть: " + part.getName());

        HierarchyNode node = new HierarchyNode(
                part.getName(),
                part,
                (part instanceof TmcProjectPart)
                        ? HierarchyNode.NodeType.TMC_PART
                        : HierarchyNode.NodeType.PART
        );

        TreeItem<Object> item = new TreeItem<>(node, node.getIcon());

        // Добавить процессы
        for (Process pr : part.getProcesses()) {
            item.getChildren().add(createProcessNode(pr, part));
        }

        // Добавить дочерние части (кроме TMC)
        for (Part child : part.getChildren()) {
            if (!(child instanceof TmcProjectPart)) {
                item.getChildren().add(createPartNode(child));
            }
        }

        // TMC добавляется через операции, а не напрямую
        return item;
    }

    // ======================================================================
    // PROCESS NODE
    // ======================================================================
    private static TreeItem<Object> createProcessNode(Process process, Part parentPart) {

        System.out.println("  Процесс: " + process.getName());

        HierarchyNode node = new HierarchyNode(
                process.getName(),
                process,
                HierarchyNode.NodeType.PROCESS
        );

        TreeItem<Object> item = new TreeItem<>(node, node.getIcon());

        for (Operation op : process.getOperations()) {
            item.getChildren().add(createOperationNode(op, parentPart));
        }

        return item;
    }

    // ======================================================================
    // OPERATION NODE
    // ======================================================================
    private static TreeItem<Object> createOperationNode(Operation op, Part parentPart) {

        System.out.println("    Операция: " + op.getType());

        HierarchyNode node = new HierarchyNode(
                op.getType().name() + " (" + op.getNormTime() + " ч)",
                op,
                HierarchyNode.NodeType.OPERATION
        );

        TreeItem<Object> item = new TreeItem<>(node, node.getIcon());

        // Вставляем TMC, которые привязаны к этой операции
        for (Part child : parentPart.getChildren()) {
            if (child instanceof TmcProjectPart tmc) {
                if (tmc.getConsumedBy() == op.getType()) {
                    System.out.println("      → Добавляем ТМЦ: " + tmc.getName());
                    item.getChildren().add(createTmcNode(tmc));
                }
            }
        }

        return item;
    }

    // ======================================================================
    // TMC PART NODE
    // ======================================================================
    private static TreeItem<Object> createTmcNode(TmcProjectPart tmc) {

        System.out.println("      TMC: " + tmc.getName());

        HierarchyNode node = new HierarchyNode(
                tmc.getName(),
                tmc,
                HierarchyNode.NodeType.TMC_PART
        );

        TreeItem<Object> item = new TreeItem<>(node, node.getIcon());

        for (Process p : tmc.getProcesses()) {
            item.getChildren().add(createTmcProcessNode(p));
        }

        return item;
    }

    // ======================================================================
    // TMC PROCESS NODE
    // ======================================================================
    private static TreeItem<Object> createTmcProcessNode(Process process) {

        System.out.println("        TMC-process: " + process.getName());

        HierarchyNode node = new HierarchyNode(
                process.getName(),
                process,
                HierarchyNode.NodeType.PROCESS
        );

        TreeItem<Object> item = new TreeItem<>(node, node.getIcon());

        for (Operation op : process.getOperations()) {
            item.getChildren().add(createTmcOperationNode(op));
        }

        return item;
    }

    // ======================================================================
    // TMC OPERATION NODE
    // ======================================================================
    private static TreeItem<Object> createTmcOperationNode(Operation op) {

        System.out.println("          TMC-op: " + op.getType());

        HierarchyNode node = new HierarchyNode(
                op.getType().name() + " (" + op.getNormTime() + " ч)",
                op,
                HierarchyNode.NodeType.OPERATION
        );

        return new TreeItem<>(node, node.getIcon());
    }
}
