package org.solo.manager_ktpp.model;

/**
 * ТМЦ-проект. Наследует Part, но хранит ссылку на операцию-потребителя.
 */
public class TmcProjectPart extends Part {

    // Ссылка на реальную операцию, которая потребляет эту ТМЦ
    private Operation consumerOperation;

    public TmcProjectPart(String name) {
        super(name, true);
    }

    public Operation getConsumerOperation() {
        return consumerOperation;
    }

    /**
     * Привязка к операции — устанавливает ссылку, но не добавляет в operation.consumedParts,
     * чтобы избежать рекурсивного дублирования; в коде парсера используем Operation.addConsumedPart(...)
     */
    public void setConsumerOperation(Operation consumerOperation) {
        this.consumerOperation = consumerOperation;
    }
}
