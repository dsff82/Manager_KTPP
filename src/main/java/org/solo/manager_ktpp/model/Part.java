package org.solo.manager_ktpp.model;

import javafx.scene.control.TreeItem;

import java.util.ArrayList;
import java.util.List;

public class Part {
    private String code;       // 1, 1.2, 1.2.3 ...
    private String name;       // столбец B
    private boolean hasKTPP;   // зависит от D или E
    private List<Process> processes = new ArrayList<>();
    private List<Part> children = new ArrayList<>();

    public Part(String code, String name, boolean hasKTPP) {
        this.code = code;
        this.name = name;
        this.hasKTPP = hasKTPP;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public boolean isHasKTPP() { return hasKTPP; }
    public List<Process> getProcesses() { return processes; }
    public List<Part> getChildren() { return children; }

    public void addProcess(Process p) { processes.add(p); }
    public void addChild(Part p) { children.add(p); }

    // создаёт TreeItem рекурсивно (с иконками)
    public TreeItem<String> toTreeItem() {
        TreeItem<String> partNode = new TreeItem<>("★ " + name);
        partNode.setExpanded(true);

        // добавить процессы этого узла
        for (Process proc : processes) {
            partNode.getChildren().add(proc.toTreeItem());
        }

        // дети (поддетали)
        for (Part ch : children) {
            partNode.getChildren().add(ch.toTreeItem());
        }
        return partNode;
    }
}
