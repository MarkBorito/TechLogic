package com.appdev.techlogic;

public class ShapeItem {
    private String name;
    private int imageResId; // The ID of the drawable (e.g., R.drawable.and)

    public ShapeItem(String name, int imageResId) {
        this.name = name;
        this.imageResId = imageResId;
    }

    public String getName() { return name; }
    public int getImageResId() { return imageResId; }


}

