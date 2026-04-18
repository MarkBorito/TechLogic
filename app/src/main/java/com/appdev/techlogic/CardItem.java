package com.appdev.techlogic;

public class CardItem {
    public String title;
    public boolean isAddButton;
    public byte[] image;


    public CardItem(String title, boolean isAddButton, byte[] image) {
        this.title = title;
        this.isAddButton = isAddButton;
        this.image = image;
    }

    public CardItem(String title, boolean isAddButton) {
        this.title = title;
        this.isAddButton = isAddButton;
        this.image = null;
    }

    public CardItem(boolean isAddButton) {
        this.isAddButton = isAddButton;
        this.image = null;
        this.title = "";
    }

    public CardItem() {
    }
}