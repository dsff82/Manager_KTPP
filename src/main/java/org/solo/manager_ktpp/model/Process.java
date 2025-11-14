package org.solo.manager_ktpp.model;

import javafx.scene.control.TreeItem;

import java.util.ArrayList;
import java.util.List;

public class Process {
    private String name; // УТ+имя, ТМЦ-П+имя, Зак+имя
    private String type; // "УТ", "ТМЦ-П", "Зак"
    private List<Operation> operations = new ArrayList<>();

    public Process(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() { return name; }
    public String getType() { return type; }
    public List<Operation> getOperations() { return operations; }

    public void addOperation(Operation o) { operations.add(o); }

    public TreeItem<String> toTreeItem() {
        TreeItem<String> proc = new TreeItem<>("⧉ " + name);
        proc.setExpanded(true);
        for (Operation op : operations) proc.getChildren().add(op.toTreeItem());
        return proc;
    }
}
