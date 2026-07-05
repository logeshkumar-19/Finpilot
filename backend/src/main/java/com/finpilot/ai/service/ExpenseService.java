package com.finpilot.ai.service;

import com.finpilot.ai.dto.ExpenseParseResult;
import com.finpilot.ai.model.Expense;
import com.finpilot.ai.model.SpendingProfile;
import com.finpilot.ai.model.User;
import com.finpilot.ai.repository.ExpenseRepository;
import com.finpilot.ai.repository.SpendingProfileRepository;
import com.finpilot.ai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final SpendingProfileRepository spendingProfileRepository;
    private final AIService aiService;

    public List<Expense> getExpensesByUserId(Long userId) {
        return expenseRepository.findTop20ByUserIdOrderByTransactionDateDescIdDesc(userId);
    }

    @Transactional
    public Expense createExpense(Long userId, Expense expense) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        expense.setUser(user);
        if (expense.getTransactionDate() == null) {
            expense.setTransactionDate(LocalDate.now());
        }
        if (expense.getTransactionTime() == null) {
            expense.setTransactionTime(LocalTime.now());
        }

        // Auto detect impulse based on simple rules or prompt if not specified
        if (expense.getIsImpulse() == null) {
            boolean lateNight = expense.getMealType() != null && expense.getMealType().equalsIgnoreCase("Late Night");
            boolean nonEssentialFood = "Food".equalsIgnoreCase(expense.getCategory()) && "Restaurant".equalsIgnoreCase(expense.getSubcategory());
            boolean shopping = "Shopping".equalsIgnoreCase(expense.getCategory());
            expense.setIsImpulse(lateNight || shopping || (nonEssentialFood && expense.getAmount().compareTo(new BigDecimal("500")) > 0));
        }

        Expense saved = expenseRepository.save(expense);
        recalculateSpendingProfile(user);
        return saved;
    }

    @Transactional
    public Expense quickParseAndCreate(Long userId, String rawText) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        String timeContext = "date: " + LocalDate.now() + ", time: " + LocalTime.now();
        ExpenseParseResult result = aiService.parseExpenseQuickInput(rawText, timeContext);

        Expense expense = Expense.builder()
                .user(user)
                .rawInput(rawText)
                .merchant(result.getMerchant())
                .amount(result.getAmount())
                .gst(result.getGst())
                .category(result.getCategory())
                .subcategory(result.getSubcategory())
                .mealType(result.getMealType())
                .expenseType(result.getExpenseType())
                .transactionDate(result.getTransactionDate())
                .transactionTime(result.getTransactionTime())
                .confidenceScore(result.getConfidenceScore())
                .aiContextSummary(result.getAiContextSummary())
                .build();

        return createExpense(userId, expense);
    }

    @Transactional
    public void recalculateSpendingProfile(User user) {
        List<Expense> expenses = expenseRepository.findByUserId(user.getId());
        
        // Sum current month spending
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        
        BigDecimal currentMonthSpent = expenseRepository.findByUserIdAndTransactionDateBetween(user.getId(), startOfMonth, endOfMonth)
                .stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal budget = user.getMonthlyIncomeBudget();
        BigDecimal utilization = budget.compareTo(BigDecimal.ZERO) > 0 ? 
                currentMonthSpent.divide(budget, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        AIService.UserSpendingStats stats = AIService.UserSpendingStats.builder()
                .totalSpent(currentMonthSpent)
                .budgetUtilization(utilization)
                .build();

        SpendingProfile calculated = aiService.analyzeSpendingPatterns(expenses, stats);
        calculated.setUser(user);
        calculated.setUserId(user.getId());

        spendingProfileRepository.save(calculated);
    }
}
