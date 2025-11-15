package org.solo.manager_ktpp.ui;

import javafx.scene.control.TreeItem;
import org.solo.manager_ktpp.model.*;
import org.solo.manager_ktpp.model.Process;

public class TreeBuilder {

    public static TreeItem<Object> buildTree(Part root) {
        return createPartNode(root);
    }

    private static TreeItem<Object> createPartNode(Part part) {

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

        return item;
    }

    private static TreeItem<Object> createProcessNode(Process process, Part parentPart) {

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

    private static TreeItem<Object> createOperationNode(Operation op, Part parentPart) {

        HierarchyNode node = new HierarchyNode(
                op.getType().name() + " (" + op.getNormTime() + " ч)",
                op,
                HierarchyNode.NodeType.OPERATION
        );

        TreeItem<Object> item = new TreeItem<>(node, node.getIcon());

        // === ВСТАВЛЯЕМ TMC ПОД ОПЕРАЦИЮ ===
        for (Part child : parentPart.getChildren()) {
            if (child instanceof TmcProjectPart) {

                TmcProjectPart tmc = (TmcProjectPart) child;

                // подходит ли этот TMC для текущей операции?
                if (tmc.getConsumedBy() == op.getType()) {

                    item.getChildren().add(createTmcNode(tmc));
                }
            }
        }

        return item;
    }

    private static TreeItem<Object> createTmcNode(TmcProjectPart tmc) {

        HierarchyNode node = new HierarchyNode(
                tmc.getName(),
                tmc,
                HierarchyNode.NodeType.TMC_PART
        );

        TreeItem<Object> item = new TreeItem<>(node, node.getIcon());

        // Добавить процессы TMC
        for (Process p : tmc.getProcesses()) {
            item.getChildren().add(createTmcProcessNode(p));
        }

        return item;
    }

    private static TreeItem<Object> createTmcProcessNode(Process process) {

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

    private static TreeItem<Object> createTmcOperationNode(Operation op) {

        HierarchyNode node = new HierarchyNode(
                op.getType().name() + " (" + op.getNormTime() + " ч)",
                op,
                HierarchyNode.NodeType.OPERATION
        );

        return new TreeItem<>(node, node.getIcon());
    }
}
