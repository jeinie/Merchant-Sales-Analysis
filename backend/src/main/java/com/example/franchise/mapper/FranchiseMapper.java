package com.example.franchise.mapper;

import com.example.franchise.domain.Franchise;
import com.example.franchise.domain.MonthlySales;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import java.util.Map;

@Mapper
public interface FranchiseMapper {
    List<Franchise> findAllFranchises();
    List<MonthlySales> findSalesByFranchiseId(String franchiseId);
    
    // For Averages
    List<Map<String, Object>> getIndustryAverages();
    List<Map<String, Object>> getRegionAverages();
}
