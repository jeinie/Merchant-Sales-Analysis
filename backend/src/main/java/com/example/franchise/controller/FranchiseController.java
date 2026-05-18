package com.example.franchise.controller;

import com.example.franchise.domain.Franchise;
import com.example.franchise.domain.MonthlySales;
import com.example.franchise.mapper.FranchiseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173") // Allow Vite frontend
@RequiredArgsConstructor
public class FranchiseController {

    private final FranchiseMapper franchiseMapper;

    @GetMapping("/franchises")
    public List<Franchise> getFranchises() {
        List<Franchise> franchises = franchiseMapper.findAllFranchises();
        
        for (Franchise f : franchises) {
            List<MonthlySales> sales = franchiseMapper.findSalesByFranchiseId(f.getId());
            f.setMonthlySales(sales);
        }
        
        return franchises;
    }

    @GetMapping("/averages")
    public Map<String, Object> getAverages() {
        Map<String, Object> response = new HashMap<>();
        
        // Process Industry Averages
        List<Map<String, Object>> indList = franchiseMapper.getIndustryAverages();
        Map<String, Map<String, Object>> industryAverages = new HashMap<>();
        for (Map<String, Object> row : indList) {
            String industry = (String) row.get("INDUSTRY");
            String month = (String) row.get("MONTH");
            Long avgSales = ((Number) row.get("AVG_SALES")).longValue();
            
            industryAverages.putIfAbsent(industry, new HashMap<>());
            Map<String, Object> indObj = industryAverages.get(industry);
            
            List<Map<String, Object>> monthlySales = (List<Map<String, Object>>) indObj.computeIfAbsent("monthlySales", k -> new ArrayList<>());
            Map<String, Object> saleNode = new HashMap<>();
            saleNode.put("month", month);
            saleNode.put("sales", avgSales);
            monthlySales.add(saleNode);
        }

        // Process Region Averages
        List<Map<String, Object>> regList = franchiseMapper.getRegionAverages();
        Map<String, Map<String, Object>> regionAverages = new HashMap<>();
        for (Map<String, Object> row : regList) {
            String region = (String) row.get("REGION");
            String month = (String) row.get("MONTH");
            Long avgSales = ((Number) row.get("AVG_SALES")).longValue();
            
            regionAverages.putIfAbsent(region, new HashMap<>());
            Map<String, Object> regObj = regionAverages.get(region);
            
            List<Map<String, Object>> monthlySales = (List<Map<String, Object>>) regObj.computeIfAbsent("monthlySales", k -> new ArrayList<>());
            Map<String, Object> saleNode = new HashMap<>();
            saleNode.put("month", month);
            saleNode.put("sales", avgSales);
            monthlySales.add(saleNode);
        }

        response.put("industryAverages", industryAverages);
        response.put("regionAverages", regionAverages);

        return response;
    }
}
