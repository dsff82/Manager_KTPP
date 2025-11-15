package org.solo.manager_ktpp.model;

public class Operation {

    public enum Department { UZ, OGT, TSEH, NONE }
    public enum OperationType { KD, TD, ZAKUPKA, PROIZVODSTVO }

    private OperationType type;
    private double normTime;
    private Department department;

    // Новое поле: для связки ТМЦ с операциями "что потребляется"
    private String consumes; // строковое значение из Excel

    public Operation(OperationType type, double normTime, Department department) {
        this.type = type;
        this.normTime = normTime;
        this.department = department;
    }

    public OperationType getType() { return type; }
    public double getNormTime() { return normTime; }
    public Department getDept() { return department; }

    public String getConsumes() { return consumes; }
    public void setConsumes(String consumes) {
        this.consumes = consumes;
    }

    @Override
    public String toString() {
        return type + " (" + normTime + ")";
    }
}
