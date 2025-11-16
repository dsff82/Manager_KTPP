package org.solo.manager_ktpp.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Operation {

    public enum Department { UZ, OGT, TSEH, NONE }
    public enum OperationType { KD, TD, ZAKUPKA, PROIZVODSTVO }

    private OperationType type;
    private double normTime;
    private Department department;

    // Список потребляемых TMC (двусторонняя связь)
    private final List<TmcProjectPart> consumedParts = new ArrayList<>();

    public Operation(OperationType type, double normTime, Department department) {
        this.type = type;
        this.normTime = normTime;
        this.department = department;
    }

    public OperationType getType() { return type; }
    public double getNormTime() { return normTime; }
    public Department getDept() { return department; }

    // Работа с потребляемыми частями
    public void addConsumedPart(TmcProjectPart p) {
        if (p == null) return;
        if (!consumedParts.contains(p)) {
            consumedParts.add(p);
            p.setConsumerOperation(this); // двусторонняя синхронизация
        }
    }

    public List<TmcProjectPart> getConsumedParts() {
        return Collections.unmodifiableList(consumedParts);
    }

    @Override
    public String toString() {
        return type + " (" + normTime + ")";
    }
}
