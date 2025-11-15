package org.solo.manager_ktpp.model;

import java.util.ArrayList;
import java.util.List;

public class Process {

    public enum ProcessType { UT, TMC_P, ZAK }

    private String name;
    private ProcessType type;
    private List<Operation> operations = new ArrayList<>();

    public Process(String name, ProcessType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() { return name; }
    public ProcessType getType() { return type; }
    public List<Operation> getOperations() { return operations; }

    public void addOperation(Operation op) {
        operations.add(op);
    }

    @Override
    public String toString() {
        return name;
    }
}
