package com.finpilot.ai.controller;

import com.finpilot.ai.model.Expense;
import com.finpilot.ai.model.FinancialGoal;
import com.finpilot.ai.model.SpendingProfile;
import com.finpilot.ai.model.User;
import com.finpilot.ai.repository.ExpenseRepository;
import com.finpilot.ai.repository.FinancialGoalRepository;
import com.finpilot.ai.repository.SpendingProfileRepository;
import com.finpilot.ai.repository.UserRepository;
import com.finpilot.ai.service.AIService;
import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/coach")
@RequiredArgsConstructor
@CrossOrigin
public class CoachController {

    private final AIService aiService;
    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final FinancialGoalRepository goalRepository;
    private final SpendingProfileRepository spendingProfileRepository;

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));
    }

    @PostMapping("/chat")
    public ResponseEntity<?> askCoach(@RequestBody Map<String, String> body) {
        User user = getAuthenticatedUser();
        String message = body.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Message is required.");
        }

        // Fetch context
        List<Expense> history = expenseRepository.findTop20ByUserIdOrderByTransactionDateDescIdDesc(user.getId());
        List<FinancialGoal> goals = goalRepository.findByUserId(user.getId());
        SpendingProfile profile = spendingProfileRepository.findById(user.getId()).orElse(null);

        String reply = aiService.askCoach(message, history, goals, profile, user.getMonthlyIncomeBudget());
        return ResponseEntity.ok(Map.of("reply", reply));
    }
}
