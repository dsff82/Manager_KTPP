package org.solo.manager_ktpp.model;

public class Operation {

    public enum Department { UZ, OGT, TSEH, NONE }
    public enum OperationType { KD, TD, ZAKUPKA, PROIZVODSTVO }

    private OperationType type;
    private double normTime;
    private Department department;

    // Для ТМЦ-проектов – какая операция потребляет
    private OperationType consumedBy;

    public Operation(OperationType type, double normTime, Department dept) {
        this.type = type;
        this.normTime = normTime;
        this.department = dept;
    }

    public OperationType getType() { return type; }
    public double getNormTime() { return normTime; }
    public Department getDepartment() { return department; }

    public OperationType getConsumedBy() { return consumedBy; }
    public void setConsumedBy(OperationType consumedBy) {
        this.consumedBy = consumedBy;
    }

    @Override
    public String toString() {
        return type + " (" + normTime + ")";
    }
}
