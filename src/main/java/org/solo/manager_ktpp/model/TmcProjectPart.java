package org.solo.manager_ktpp.model;

public class TmcProjectPart extends Part {

    // какое OperationType потребляет этот TMC (ZAKUPKA или PROIZVODSTVO)
    private Operation.OperationType consumedBy;

    public TmcProjectPart(String name, Operation.OperationType consumedBy) {
        super(name, true);
        this.consumedBy = consumedBy;
    }

    public Operation.OperationType getConsumedBy() {
        return consumedBy;
    }

    public void setConsumedBy(Operation.OperationType consumedBy) {
        this.consumedBy = consumedBy;
    }
}
