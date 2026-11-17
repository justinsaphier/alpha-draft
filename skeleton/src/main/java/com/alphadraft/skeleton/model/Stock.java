package com.alphadraft.skeleton.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

// Mapped to the "stocks" table. The database assigns the id on first save; a stock that
// has not been stored yet has id 0. Tickers are unique at the database level too, so two
// requests racing past UniqueTickerRule still cannot store the same ticker twice.
@Entity
@Table(name = "stocks", uniqueConstraints = @UniqueConstraint(name = "uk_stocks_ticker", columnNames = "ticker"))
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String ticker;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String sector;

    // The player who drafted this stock, or null while it is still in the pool. Many stocks
    // can belong to one user; each stock has at most one owner. Not part of the JSON shape
    // the frontend reads, and loaded only when asked for.
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drafted_by_user_id")
    private User draftedBy;

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

    public User getDraftedBy() {
        return draftedBy;
    }

    public void draftTo(User user) {
        this.draftedBy = user;
    }

    public void returnToPool() {
        this.draftedBy = null;
    }
}