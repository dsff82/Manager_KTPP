package org.solo.manager_ktpp.model;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.InputStream;

public class HierarchyNode {

    public enum NodeType {
        PART, PROCESS, OPERATION, TMC_PART
    }

    private final String title;
    private final Object value;
    private final NodeType type;

    public HierarchyNode(String title, Object value, NodeType type) {
        this.title = title;
        this.value = value;
        this.type = type;
    }

    public String getTitle() { return title; }
    public Object getValue() { return value; }
    public NodeType getType() { return type; }

    /**
     * Надёжная загрузка иконок через ClassLoader.
     * Работает в IDEA, Maven, JAR.
     */
    private Node loadIcon(String fileName) {
        String path = "/icons/" + fileName;

        InputStream is = HierarchyNode.class.getResourceAsStream(path);
        if (is == null) {
            System.err.println("⚠ ICON NOT FOUND: " + path);
            return null;
        }

        Image img = new Image(is);
        ImageView view = new ImageView(img);
        view.setFitWidth(18);
        view.setFitHeight(18);
        return view;
    }

    public Node getIcon() {
        return switch (type) {
            case PART       -> loadIcon("part.png");
            case PROCESS    -> loadIcon("process.png");
            case OPERATION  -> loadIcon("op.png");
            case TMC_PART   -> loadIcon("tmc.png");
        };
    }

    @Override
    public String toString() {
        return title;
    }
}
