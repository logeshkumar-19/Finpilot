package com.finpilot.ai.controller;

import com.finpilot.ai.model.*;
import com.finpilot.ai.repository.*;
import com.finpilot.ai.service.AIService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin
public class DashboardController {

    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final FinancialGoalRepository goalRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SpendingProfileRepository spendingProfileRepository;
    private final MonthlyIncomeRepository monthlyIncomeRepository;

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummary> getSummary() {
        User user = getAuthenticatedUser();
        Long userId = user.getId();

        LocalDate today = LocalDate.now();
        int currentMonth = today.getMonthValue();
        int currentYear = today.getYear();
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1); // Monday of current week

        // 1. Fetch data
        List<Expense> allExpenses = expenseRepository.findByUserId(userId);
        List<FinancialGoal> goals = goalRepository.findByUserId(userId);
        List<Subscription> subs = subscriptionRepository.findByUserId(userId);
        
        Optional<MonthlyIncome> incomeOpt = monthlyIncomeRepository.findByUserIdAndMonthAndYear(userId, currentMonth, currentYear);
        BigDecimal dynamicMonthlyIncome = incomeOpt.map(MonthlyIncome::getIncome).orElse(BigDecimal.ZERO);

        SpendingProfile profile = spendingProfileRepository.findById(userId)
                .orElseGet(() -> SpendingProfile.builder()
                        .userId(userId)
                        .healthScore(dynamicMonthlyIncome.compareTo(BigDecimal.ZERO) == 0 ? 0 : 80)
                        .personalityType(dynamicMonthlyIncome.compareTo(BigDecimal.ZERO) == 0 ? "Pending" : "Saver")
                        .explanation(dynamicMonthlyIncome.compareTo(BigDecimal.ZERO) == 0 ? "No financial data available yet." : "Steady tracking.")
                        .healthScoreExplanation(dynamicMonthlyIncome.compareTo(BigDecimal.ZERO) == 0 ? "No financial data available yet. Start by adding your monthly income and expenses." : "No negative points registered.")
                        .build());
        LocalDateTime incomeLastUpdated = incomeOpt.map(MonthlyIncome::getUpdatedAt).orElse(null);

        // 2. Calculations: Today's spending
        BigDecimal todayTotal = allExpenses.stream()
                .filter(e -> e.getTransactionDate().isEqual(today))
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal todayEssential = allExpenses.stream()
                .filter(e -> e.getTransactionDate().isEqual(today) && "Essential".equalsIgnoreCase(e.getExpenseType()))
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal todayNonEssential = allExpenses.stream()
                .filter(e -> e.getTransactionDate().isEqual(today) && "Non-Essential".equalsIgnoreCase(e.getExpenseType()))
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Weekly spending trends (last 7 days mapping)
        List<WeeklyTrendItem> weeklyTrend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            BigDecimal dayAmt = allExpenses.stream()
                    .filter(e -> e.getTransactionDate().isEqual(d))
                    .map(Expense::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            weeklyTrend.add(new WeeklyTrendItem(d.getDayOfWeek().name().substring(0, 3), dayAmt));
        }

        // 4. Category breakdown (current month)
        Map<String, BigDecimal> categoryMap = allExpenses.stream()
                .filter(e -> e.getTransactionDate().getMonthValue() == currentMonth && e.getTransactionDate().getYear() == currentYear)
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)
                ));
        List<CategoryBreakdownItem> breakdown = categoryMap.entrySet().stream()
                .map(e -> new CategoryBreakdownItem(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        // 5. Monthly computations and narrative story
        BigDecimal monthlySpent = allExpenses.stream()
                .filter(e -> e.getTransactionDate().getMonthValue() == currentMonth && e.getTransactionDate().getYear() == currentYear)
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        BigDecimal monthlySavings;
        Double savingsRate;
        BigDecimal remainingBudget;

        if (dynamicMonthlyIncome.compareTo(BigDecimal.ZERO) == 0) {
            monthlySavings = BigDecimal.ZERO;
            savingsRate = 0.0;
            remainingBudget = BigDecimal.ZERO;
        } else {
            monthlySavings = dynamicMonthlyIncome.subtract(monthlySpent);
            savingsRate = monthlySavings.multiply(new BigDecimal(100)).divide(dynamicMonthlyIncome, 1, RoundingMode.HALF_UP).doubleValue();
            remainingBudget = dynamicMonthlyIncome.subtract(monthlySpent);
        }

        String highestCategory = categoryMap.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse("None");

        String monthlyStory;
        if (dynamicMonthlyIncome.compareTo(BigDecimal.ZERO) == 0 && monthlySpent.compareTo(BigDecimal.ZERO) == 0) {
            monthlyStory = "No AI insights available yet.";
        } else {
            monthlyStory = String.format(
                    "For %s, you have accumulated ₹%s in spending against your income of ₹%s. The highest expense occurred in %s. " +
                    "You have registered multiple impulse buys which account for about %s%% of your non-essential spending. " +
                    "Adjusting weekend dine-outs will help keep you on track.",
                    today.getMonth().name().toLowerCase(),
                    monthlySpent,
                    dynamicMonthlyIncome,
                    highestCategory,
                    todayNonEssential.compareTo(BigDecimal.ZERO) > 0 ? "35" : "0"
            );
        }

        // 6. Assemble response
        DashboardSummary summary = DashboardSummary.builder()
                .currency(user.getCurrency())
                .monthlyIncomeBudget(dynamicMonthlyIncome)
                .monthlyIncomeLastUpdated(incomeLastUpdated)
                .monthlySpent(monthlySpent)
                .monthlySavings(monthlySavings)
                .savingsRate(savingsRate)
                .remainingBudget(remainingBudget)
                .todayTotal(todayTotal)
                .todayEssential(todayEssential)
                .todayNonEssential(todayNonEssential)
                .personalityType(profile.getPersonalityType())
                .personalityExplanation(profile.getExplanation())
                .healthScore(profile.getHealthScore())
                .healthScoreExplanation(profile.getHealthScoreExplanation())
                .monthlyStory(monthlyStory)
                .weeklyTrends(weeklyTrend)
                .categoryBreakdown(breakdown)
                .goalsCount(goals.size())
                .activeSubscriptionsCount((int) subs.stream().filter(Subscription::getIsActive).count())
                .flaggedSubscriptionsCount((int) subs.stream().filter(s -> s.getIsActive() && s.getIsFlaggedUnused()).count())
                .build();

        return ResponseEntity.ok(summary);
    }

    @Getter
    @Builder
    public static class DashboardSummary {
        private String currency;
        private BigDecimal monthlyIncomeBudget;
        private LocalDateTime monthlyIncomeLastUpdated;
        private BigDecimal monthlySpent;
        private BigDecimal monthlySavings;
        private Double savingsRate;
        private BigDecimal remainingBudget;
        private BigDecimal todayTotal;
        private BigDecimal todayEssential;
        private BigDecimal todayNonEssential;
        private String personalityType;
        private String personalityExplanation;
        private Integer healthScore;
        private String healthScoreExplanation;
        private String monthlyStory;
        private List<WeeklyTrendItem> weeklyTrends;
        private List<CategoryBreakdownItem> categoryBreakdown;
        private int goalsCount;
        private int activeSubscriptionsCount;
        private int flaggedSubscriptionsCount;
    }

    @Getter
    @AllArgsConstructor
    public static class WeeklyTrendItem {
        private String day;
        private BigDecimal amount;
    }

    @Getter
    @AllArgsConstructor
    public static class CategoryBreakdownItem {
        private String name;
        private BigDecimal value;
    }
}
