package com.example.swp391.aistudenthub.feature.admin.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminBusinessStatsResponse {
    private long totalRevenue;
    private long currentMonthRevenue;
    private long activePremiumUsers;
    private String mostPopularPackage;
}
