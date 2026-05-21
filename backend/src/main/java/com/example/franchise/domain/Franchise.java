package com.example.franchise.domain;

import java.util.List;

public class Franchise {
    private String id;
    private String name;
    private String industry;
    private String region;
    private String address;
    
    // For nesting the monthly sales data
    private List<MonthlySales> monthlySales;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<MonthlySales> getMonthlySales() {
        return monthlySales;
    }

    public void setMonthlySales(List<MonthlySales> monthlySales) {
        this.monthlySales = monthlySales;
    }
}
