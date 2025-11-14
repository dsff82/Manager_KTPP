package org.solo.manager_ktpp.model;

import javafx.scene.control.TreeItem;

public class Operation {
    private String name;       // КД, ТД, Производство, Закупка
    private String norm;       // норма как строка (из ячейки)
    private String department; // подразделение

    public Operation(String name, String norm, String department) {
        this.name = name;
        this.norm = norm;
        this.department = department;
    }

    public String getName() { return name; }
    public String getNorm() { return norm; }
    public String getDepartment() { return department; }

    public TreeItem<String> toTreeItem() {
        StringBuilder sb = new StringBuilder();
        sb.append("[O] ").append(name);
        if (norm != null && !norm.isEmpty()) sb.append(" (").append(norm).append(" ч)");
        if (department != null && !department.isEmpty()) sb.append(", ").append(department);
        return new TreeItem<>(sb.toString());
    }
}
