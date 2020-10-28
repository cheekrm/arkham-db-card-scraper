package com.ryancheek.model;

public class Card {
    final String name;
    final String imgSrc;

    public Card(String name, String imgSrc) {
        this.name = name;
        this.imgSrc = imgSrc;
    }

    public String getName() {
        return name;
    }

    public String getImgSrc() {
        return imgSrc;
    }
}