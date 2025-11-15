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
            item.getChildren().add(createProcessNode(pr));
        }

        // Добавить детей-частей
        for (Part child : part.getChildren()) {
            item.getChildren().add(createPartNode(child));
        }

        return item;
    }

    private static TreeItem<Object> createProcessNode(Process process) {

        HierarchyNode node = new HierarchyNode(
                process.getName(),
                process,
                HierarchyNode.NodeType.PROCESS
        );

        TreeItem<Object> item = new TreeItem<>(node, node.getIcon());

        for (Operation op : process.getOperations()) {
            item.getChildren().add(createOperationNode(op));
        }

        return item;
    }

    private static TreeItem<Object> createOperationNode(Operation op) {

        HierarchyNode node = new HierarchyNode(
                op.getType().name() + " (" + op.getNormTime() + " ч)",
                op,
                HierarchyNode.NodeType.OPERATION
        );

        return new TreeItem<>(node, node.getIcon());
    }
}
