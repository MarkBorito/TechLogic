package com.appdev.techlogic;

public class CardItem {
    public String title;
    public boolean isAddButton;
    public byte[] image;

    //1. Standard constructor for cards with images (3 arguments)
    public CardItem(String title, boolean isAddButton, byte[] image) {
        this.title = title;
        this.isAddButton = isAddButton;
        this.image = image;
    }

    // 2. Constructor for cards WITHOUT an image (2 arguments)
    // This fixes errors in DatabaseHelper if imageBytes is null
    public CardItem(String title, boolean isAddButton) {
        this.title = title;
        this.isAddButton = isAddButton;
        this.image = null;
    }

    // 3. Constructor for the "Add New" button card (1 argument)
    public CardItem(boolean isAddButton) {
        this.isAddButton = isAddButton;
        this.image = null;
        this.title = "";
    }

    // 4. Empty constructor to prevent "no default constructor" errors
    public CardItem() {
    }
}