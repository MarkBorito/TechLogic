package com.appdev.techlogic;

public class ShapeItem {
    private String name;
    private int imageResId;
    private boolean isCategory;

    public ShapeItem(String name, int imageResId, boolean isCategory) {
        this.name = name;
        this.imageResId = imageResId;
        this.isCategory = isCategory;
    }

    public String getName() { return name; }
    public int getImageResId() { return imageResId; }
    public boolean isCategory() { return isCategory; }
}

