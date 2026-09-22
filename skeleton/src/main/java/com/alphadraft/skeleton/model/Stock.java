package com.alphadraft.skeleton.model;

public class Stock {

    private final int id;
    private final String ticker;
    private final String name;
    private final String sector;

    public Stock(int id, String ticker, String name, String sector) {
        this.id = id;
        this.ticker = ticker;
        this.name = name;
        this.sector = sector;
    }

    public int getId() {
        return id;
    }

    public String getTicker() {
        return ticker;
    }

    public String getName() {
        return name;
    }

    public String getSector() {
        return sector;
    }
}
