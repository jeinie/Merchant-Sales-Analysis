package com.example.franchise.domain;

import lombok.Data;

@Data
public class MonthlySales {
    private Long id;
    private String franchiseId;
    private String month;
    private Long sales;
    private Integer txCount;
    private Integer avgTicket;
}
