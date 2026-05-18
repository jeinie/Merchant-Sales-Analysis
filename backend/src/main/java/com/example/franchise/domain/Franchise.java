package com.example.franchise.domain;

import lombok.Data;
import java.util.List;

@Data
public class Franchise {
    private String id;
    private String name;
    private String industry;
    private String region;
    private String address;
    
    // For nesting the monthly sales data
    private List<MonthlySales> monthlySales;
}
