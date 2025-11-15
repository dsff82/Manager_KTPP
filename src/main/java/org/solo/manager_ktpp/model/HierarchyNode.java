package org.solo.manager_ktpp.model;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class HierarchyNode {

    public enum NodeType {
        PART, PROCESS, OPERATION, TMC_PART
    }

    private String title;
    private Object value;
    private NodeType type;

    public HierarchyNode(String title, Object value, NodeType type) {
        this.title = title;
        this.value = value;
        this.type = type;
    }

    public String getTitle() { return title; }
    public Object getValue() { return value; }
    public NodeType getType() { return type; }

    public ImageView getIcon() {
        switch (type) {
            case PART: return new ImageView(new Image("/icons/part.png"));
            case PROCESS: return new ImageView(new Image("/icons/process.png"));
            case OPERATION: return new ImageView(new Image("/icons/op.png"));
            case TMC_PART: return new ImageView(new Image("/icons/tmc.png"));
            default: return null;
        }
    }

    @Override
    public String toString() {
        return title;
    }
}
