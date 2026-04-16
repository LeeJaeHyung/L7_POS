package com.l7pos.l7_pos.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", nullable = false, length = 50, unique = true)
    private String productCode;

    @Column(name = "price", nullable = false)
    private Integer price;

    public Product() {
    }

    public Product(String productCode, Integer price) {
        this.productCode = productCode;
        this.price = price;
    }

    public Long getId() {
        return id;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }
}