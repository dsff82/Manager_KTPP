package org.solo.manager_ktpp.model;

public class TmcProjectPart extends Part {

    private Operation.OperationType consumedByOperation;

    public TmcProjectPart(String name, Operation.OperationType consumedByOp) {
        super(name, true);
        this.consumedByOperation = consumedByOp;
    }

    public Operation.OperationType getConsumedByOperation() {
        return consumedByOperation;
    }
}
