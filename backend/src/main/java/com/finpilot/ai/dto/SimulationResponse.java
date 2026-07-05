package com.finpilot.ai.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SimulationResponse {
    
    private String recommendation;
    private ScenarioDetails scenarioA;
    private ScenarioDetails scenarioB;
    private ScenarioDetails scenarioC;
    private ScenarioDetails scenarioD;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ScenarioDetails {
        private String name;
        private String description;
        private BigDecimal monthlyCost;
        private Integer goalDelayMonths;
        private BigDecimal totalCost;
        private String impactAnalysis;
    }
}
