package com.finpilot.ai.service;

import com.finpilot.ai.model.FinancialGoal;
import com.finpilot.ai.model.User;
import com.finpilot.ai.repository.FinancialGoalRepository;
import com.finpilot.ai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoalService {

    private final FinancialGoalRepository goalRepository;
    private final UserRepository userRepository;

    public List<FinancialGoal> getGoalsByUserId(Long userId) {
        List<FinancialGoal> goals = goalRepository.findByUserId(userId);
        goals.forEach(this::calculateGoalMetrics);
        return goals;
    }

    @Transactional
    public FinancialGoal createGoal(Long userId, FinancialGoal goal) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        goal.setUser(user);
        if (goal.getCurrentAmount() == null) {
            goal.setCurrentAmount(BigDecimal.ZERO);
        }
        if (goal.getStatus() == null) {
            goal.setStatus("ACTIVE");
        }
        
        calculateGoalMetrics(goal);
        return goalRepository.save(goal);
    }

    @Transactional
    public FinancialGoal contributeToGoal(Long goalId, BigDecimal amount) {
        FinancialGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new IllegalArgumentException("Goal not found: " + goalId));
        goal.setCurrentAmount(goal.getCurrentAmount().add(amount));
        if (goal.getCurrentAmount().compareTo(goal.getTargetAmount()) >= 0) {
            goal.setStatus("COMPLETED");
        }
        calculateGoalMetrics(goal);
        return goalRepository.save(goal);
    }

    private void calculateGoalMetrics(FinancialGoal goal) {
        if (goal.getTargetAmount() == null || goal.getTargetAmount().compareTo(BigDecimal.ZERO) == 0) return;

        BigDecimal remaining = goal.getTargetAmount().subtract(goal.getCurrentAmount());
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            goal.setStatus("COMPLETED");
            goal.setPredictedCompletionDate(LocalDate.now());
            goal.setMonthlySavingTarget(BigDecimal.ZERO);
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate target = goal.getTargetDate();

        if (target != null && target.isAfter(today)) {
            long monthsBetween = ChronoUnit.MONTHS.between(today.withDayOfMonth(1), target.withDayOfMonth(1));
            if (monthsBetween <= 0) monthsBetween = 1;
            
            BigDecimal monthlyTarget = remaining.divide(new BigDecimal(monthsBetween), 2, RoundingMode.HALF_UP);
            goal.setMonthlySavingTarget(monthlyTarget);
        }

        // Project predicted completion date based on user savings velocity (monthlySavingTarget or default surplus)
        BigDecimal monthlySavingsCapacity = goal.getMonthlySavingTarget();
        if (monthlySavingsCapacity == null || monthlySavingsCapacity.compareTo(BigDecimal.ZERO) == 0) {
            monthlySavingsCapacity = new BigDecimal("5000"); // Baseline fallback savings rate ₹5000/mo
        }

        BigDecimal monthsToSave = remaining.divide(monthlySavingsCapacity, 2, RoundingMode.HALF_UP);
        long daysToSave = monthsToSave.multiply(new BigDecimal("30")).longValue();
        goal.setPredictedCompletionDate(today.plusDays(daysToSave));

        if (target != null && goal.getPredictedCompletionDate().isAfter(target)) {
            goal.setStatus("DELAYED");
        } else {
            goal.setStatus("ACTIVE");
        }
    }
}
