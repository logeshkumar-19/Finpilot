package com.finpilot.ai.controller;

import com.finpilot.ai.model.FinancialGoal;
import com.finpilot.ai.model.User;
import com.finpilot.ai.repository.UserRepository;
import com.finpilot.ai.service.GoalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/goals")
@RequiredArgsConstructor
@CrossOrigin
public class GoalController {

    private final GoalService goalService;
    private final UserRepository userRepository;

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));
    }

    @GetMapping
    public ResponseEntity<List<FinancialGoal>> getGoals() {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(goalService.getGoalsByUserId(user.getId()));
    }

    @PostMapping
    public ResponseEntity<FinancialGoal> createGoal(@RequestBody FinancialGoal goal) {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(goalService.createGoal(user.getId(), goal));
    }

    @PostMapping("/{id}/contribute")
    public ResponseEntity<FinancialGoal> contributeToGoal(@PathVariable Long id, @RequestBody Map<String, BigDecimal> body) {
        BigDecimal amount = body.get("amount");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(goalService.contributeToGoal(id, amount));
    }
}
