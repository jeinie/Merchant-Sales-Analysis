package com.example.merchant.domain;

public class MonthlySales {
    private Long id;
    private String merchantId;
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

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
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
