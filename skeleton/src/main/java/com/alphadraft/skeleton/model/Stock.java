package com.alphadraft.skeleton.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Mapped to the "stocks" table. The database assigns the id on first save; a stock that
// has not been stored yet has id 0.
@Entity
@Table(name = "stocks")
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String ticker;
    private String name;
    private String sector;

    // Required by JPA. Application code uses the constructor below.
    protected Stock() {
    }

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
