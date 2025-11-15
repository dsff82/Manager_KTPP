package org.solo.manager_ktpp.ui;

import javafx.beans.property.SimpleStringProperty;
import org.solo.manager_ktpp.model.*;
import org.solo.manager_ktpp.model.Process;

public class RowModel {

    private final SimpleStringProperty type;
    private final SimpleStringProperty name;
    private final SimpleStringProperty norm;
    private final SimpleStringProperty dept;
    private final SimpleStringProperty consumedBy;

    private final Object model;

    public RowModel(String type, String name, String norm, String dept, String consumedBy, Object model) {
        this.type = new SimpleStringProperty(type);
        this.name = new SimpleStringProperty(name);
        this.norm = new SimpleStringProperty(norm);
        this.dept = new SimpleStringProperty(dept);
        this.consumedBy = new SimpleStringProperty(consumedBy);
        this.model = model;
    }

    /**
     * Фабрика строк таблицы — адаптирована под новый ExcelParser.
     */
    public static RowModel of(Object obj) {

        if (obj instanceof Part p) {
            return new RowModel(
                    "Part",
                    p.getName(),
                    "",
                    "",
                    "",
                    obj
            );
        }

        if (obj instanceof Process pr) {
            return new RowModel(
                    "Process",
                    pr.getName(),
                    "",
                    "",
                    pr.getType().name(),   // UT / TMC_P / ZAK
                    obj
            );
        }

        if (obj instanceof Operation op) {
            return new RowModel(
                    "Operation",
                    op.getType().name(),
                    String.valueOf(op.getNormTime()),
                    op.getDept() == null ? "" : String.valueOf(op.getDept()),
                    op.getConsumes() == null ? "" : op.getConsumes(), // Новое поле
                    obj
            );
        }

        if (obj instanceof TmcProjectPart tmc) {
            return new RowModel(
                    "TMC",
                    tmc.getName(),
                    "",
                    "",
                    "",   // consumedBy больше нет – теперь в Operation
                    obj
            );
        }

        return new RowModel("?", "?", "", "", "", obj);
    }

    public Object getModelObject() { return model; }

    public SimpleStringProperty typeProperty() { return type; }
    public SimpleStringProperty nameProperty() { return name; }
    public SimpleStringProperty normProperty() { return norm; }
    public SimpleStringProperty deptProperty() { return dept; }
    public SimpleStringProperty consumedByProperty() { return consumedBy; }
}
