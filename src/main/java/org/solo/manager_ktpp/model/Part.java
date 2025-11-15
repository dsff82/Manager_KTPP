package org.solo.manager_ktpp.model;

import java.util.ArrayList;
import java.util.List;

public class Part {

    private String name;
    private boolean hasKTPP; // столбцы D или E
    private List<Process> processes = new ArrayList<>();
    private List<Part> children = new ArrayList<>();

    public Part(String name, boolean hasKTPP) {
        this.name = name;
        this.hasKTPP = hasKTPP;
    }

    public String getName() { return name; }
    public boolean hasKTPP() { return hasKTPP; }

    public List<Process> getProcesses() { return processes; }
    public List<Part> getChildren() { return children; }

    public void addProcess(Process process) {
        processes.add(process);
    }

    public void addChild(Part child) {
        children.add(child);
    }

    @Override
    public String toString() {
        return name;
    }
}
