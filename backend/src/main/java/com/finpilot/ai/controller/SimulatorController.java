package com.finpilot.ai.controller;

import com.finpilot.ai.dto.SimulationResponse;
import com.finpilot.ai.model.FinancialGoal;
import com.finpilot.ai.model.User;
import com.finpilot.ai.repository.FinancialGoalRepository;
import com.finpilot.ai.repository.UserRepository;
import com.finpilot.ai.service.AIService;
import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/simulator")
@RequiredArgsConstructor
@CrossOrigin
public class SimulatorController {

    private final AIService aiService;
    private final UserRepository userRepository;
    private final FinancialGoalRepository goalRepository;

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));
    }

    @PostMapping("/purchase")
    public ResponseEntity<SimulationResponse> simulatePurchase(@RequestBody PurchaseSimulationRequest request) {
        User user = getAuthenticatedUser();
        List<FinancialGoal> goals = goalRepository.findByUserId(user.getId());

        // Determine monthly savings velocity. Default to ₹15,000 if not configured.
        BigDecimal monthlySavingsCapacity = user.getMonthlyIncomeBudget().multiply(new BigDecimal("0.20")); // Assume 20% savings capacity
        if (monthlySavingsCapacity.compareTo(BigDecimal.ZERO) == 0) {
            monthlySavingsCapacity = new BigDecimal("15000.00");
        }

        SimulationResponse response = aiService.simulatePurchase(
                request.getItemName(),
                request.getItemCost(),
                monthlySavingsCapacity,
                goals
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/what-if")
    public ResponseEntity<?> simulateWhatIf(@RequestBody WhatIfRequest request) {
        // Compute projected compound savings
        BigDecimal monthlyReduction = request.getAmountPerIncident()
                .multiply(request.getFrequencyPerWeek())
                .multiply(new BigDecimal("4.33")) // Weeks in a month average
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal savings6m = monthlyReduction.multiply(new BigDecimal("6")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal savings1y = monthlyReduction.multiply(new BigDecimal("12")).setScale(2, RoundingMode.HALF_UP);

        // Compute 5-year compounding returns at 8% annual return (compounded monthly)
        // FV = P * [((1 + r/n)^(n*t) - 1) / (r/n)]
        double P = monthlyReduction.doubleValue();
        double r = 0.08; // 8% p.a.
        double n = 12; // Monthly compound
        double t = 5; // 5 Years
        double compoundFactor = (Math.pow(1 + (r / n), n * t) - 1) / (r / n);
        BigDecimal savings5y = BigDecimal.valueOf(P * compoundFactor).setScale(2, RoundingMode.HALF_UP);

        Map<String, Object> response = new HashMap<>();
        response.put("target", request.getReductionTarget());
        response.put("monthlySavings", monthlyReduction);
        response.put("savings6m", savings6m);
        response.put("savings1y", savings1y);
        response.put("savings5y", savings5y);
        
        String explanation = String.format("By redirecting this small expense of ₹%s (%s times a week), you " +
                "save ₹%s monthly. Invested conservatively in an index fund at 8%% p.a., this accumulates " +
                "to an impressive ₹%s in 5 years! This is equivalent to funding %s of a major financial purchase.",
                request.getAmountPerIncident(), request.getFrequencyPerWeek(), monthlyReduction, savings5y,
                monthlyReduction.compareTo(new BigDecimal("1000")) > 0 ? "a scooter" : "a premium laptop");
        
        response.put("explanation", explanation);

        return ResponseEntity.ok(response);
    }

    @Getter
    @Setter
    public static class PurchaseSimulationRequest {
        private String itemName;
        private BigDecimal itemCost;
    }

    @Getter
    @Setter
    public static class WhatIfRequest {
        private String reductionTarget; // e.g. Coffee, Smoking, Dining Out
        private BigDecimal amountPerIncident;
        private BigDecimal frequencyPerWeek;
    }
}
