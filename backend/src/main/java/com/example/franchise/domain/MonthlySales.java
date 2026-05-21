package com.example.franchise.domain;

public class MonthlySales {
    private Long id;
    private String franchiseId;
    private String month;
    private Long sales;
    private Integer txCount;
    private Integer avgTicket;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFranchiseId() {
        return franchiseId;
    }

    public void setFranchiseId(String franchiseId) {
        this.franchiseId = franchiseId;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public Long getSales() {
        return sales;
    }

    public void setSales(Long sales) {
        this.sales = sales;
    }

    public Integer getTxCount() {
        return txCount;
    }

    public void setTxCount(Integer txCount) {
        this.txCount = txCount;
    }

    public Integer getAvgTicket() {
        return avgTicket;
    }

    public void setAvgTicket(Integer avgTicket) {
        this.avgTicket = avgTicket;
    }
}
