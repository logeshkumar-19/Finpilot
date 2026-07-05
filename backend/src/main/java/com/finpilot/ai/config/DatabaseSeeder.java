package com.finpilot.ai.config;

import com.finpilot.ai.model.*;
import com.finpilot.ai.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ExpenseRepository expenseRepository;
    private final FinancialGoalRepository goalRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SpendingProfileRepository spendingProfileRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            log.info("Database already seeded with user profiles.");
            return;
        }

        log.info("Seeding default developer demo data...");

        // 1. Create Demo User
        User demoUser = User.builder()
                .email("demo@finpilot.ai")
                .passwordHash(passwordEncoder.encode("password123"))
                .firstName("Alex")
                .lastName("Pilot")
                .currency("INR")
                .monthlyIncomeBudget(new BigDecimal("100000.00"))
                .build();
        demoUser = userRepository.save(demoUser);
        // Keep a reference to the saved ID
        final Long userId = demoUser.getId();

        // 2. Create Dynamic Expenses
        LocalDate today = LocalDate.now();

        List<Expense> mockExpenses = List.of(
                Expense.builder().user(demoUser).merchant("Home Rent").amount(new BigDecimal("18000.00")).category("Utilities").subcategory("Rent").expenseType("Essential").transactionDate(today.minusDays(3)).transactionTime(LocalTime.of(10, 0)).aiContextSummary("Monthly rental payout. Marked as Essential.").source("MANUAL").build(),
                Expense.builder().user(demoUser).merchant("BESCOM Power").amount(new BigDecimal("1200.00")).category("Utilities").subcategory("Electricity").expenseType("Essential").transactionDate(today.minusDays(2)).transactionTime(LocalTime.of(15, 30)).aiContextSummary("Electric bill payment. Marked as Essential.").source("MANUAL").build(),
                Expense.builder().user(demoUser).merchant("A2B Veg Restaurant").amount(new BigDecimal("350.00")).category("Food & Dining").subcategory("Restaurant").mealType("Lunch").expenseType("Essential").transactionDate(today.minusDays(1)).transactionTime(LocalTime.of(13, 15)).aiContextSummary("Standard South Indian Lunch. Within healthy parameters.").source("MANUAL").build(),
                Expense.builder().user(demoUser).merchant("Swiggy Delivery").amount(new BigDecimal("850.00")).category("Food & Dining").subcategory("Restaurant").mealType("Late Night").expenseType("Non-Essential").transactionDate(today.minusDays(1)).transactionTime(LocalTime.of(23, 45)).isImpulse(true).aiContextSummary("Late-night food order detected. This is your third late-night order this week.").source("MANUAL").build(),
                Expense.builder().user(demoUser).merchant("Starbucks").amount(new BigDecimal("320.00")).category("Food & Dining").subcategory("Cafe").mealType("Morning Snack").expenseType("Non-Essential").transactionDate(today.minusDays(4)).transactionTime(LocalTime.of(10, 15)).isImpulse(true).aiContextSummary("Takeout coffee expense. Reducing one order per week could save 8000 INR annually.").source("MANUAL").build(),
                Expense.builder().user(demoUser).merchant("Starbucks").amount(new BigDecimal("320.00")).category("Food & Dining").subcategory("Cafe").mealType("Morning Snack").expenseType("Non-Essential").transactionDate(today.minusDays(6)).transactionTime(LocalTime.of(10, 30)).isImpulse(true).aiContextSummary("Repeated cafe expense detected. Consider brewing at home to speed up goals.").source("MANUAL").build(),
                Expense.builder().user(demoUser).merchant("Zomato Delivery").amount(new BigDecimal("620.00")).category("Food & Dining").subcategory("Restaurant").mealType("Dinner").expenseType("Non-Essential").transactionDate(today.minusDays(2)).transactionTime(LocalTime.of(20, 15)).source("MANUAL").build(),
                Expense.builder().user(demoUser).merchant("Uber Ride").amount(new BigDecimal("450.00")).category("Transportation").subcategory("Ride Sharing").expenseType("Essential").transactionDate(today.minusDays(2)).transactionTime(LocalTime.of(9, 30)).source("MANUAL").build(),
                Expense.builder().user(demoUser).merchant("Metro Recharge").amount(new BigDecimal("500.00")).category("Transportation").subcategory("Local Transport").expenseType("Essential").transactionDate(today.minusDays(5)).transactionTime(LocalTime.of(18, 0)).source("MANUAL").build(),
                Expense.builder().user(demoUser).merchant("Netflix").amount(new BigDecimal("649.00")).category("Entertainment").subcategory("Streaming").expenseType("Non-Essential").transactionDate(today.minusDays(10)).transactionTime(LocalTime.of(12, 0)).source("MANUAL").build(),
                Expense.builder().user(demoUser).merchant("Spotify Premium").amount(new BigDecimal("119.00")).category("Entertainment").subcategory("Streaming").expenseType("Non-Essential").transactionDate(today.minusDays(10)).transactionTime(LocalTime.of(12, 0)).source("MANUAL").build(),
                Expense.builder().user(demoUser).merchant("Amazon India").amount(new BigDecimal("4200.00")).category("Shopping").subcategory("Electronics").expenseType("Non-Essential").transactionDate(today.minusDays(4)).transactionTime(LocalTime.of(23, 10)).isImpulse(true).aiContextSummary("Late night electronics purchase. Flagged as non-essential impulse spend.").source("MANUAL").build()
        );
        expenseRepository.saveAll(mockExpenses);

        // 3. Create active goals
        List<FinancialGoal> mockGoals = List.of(
                FinancialGoal.builder().user(demoUser).name("MacBook Pro").targetAmount(new BigDecimal("120000.00")).currentAmount(new BigDecimal("40000.00")).targetDate(today.plusMonths(6)).monthlySavingTarget(new BigDecimal("13300.00")).status("ACTIVE").build(),
                FinancialGoal.builder().user(demoUser).name("Electric Scooter").targetAmount(new BigDecimal("150000.00")).currentAmount(new BigDecimal("25000.00")).targetDate(today.plusMonths(12)).monthlySavingTarget(new BigDecimal("10400.00")).status("ACTIVE").build(),
                FinancialGoal.builder().user(demoUser).name("Emergency Cushion").targetAmount(new BigDecimal("60000.00")).currentAmount(new BigDecimal("50000.00")).targetDate(today.plusMonths(2)).monthlySavingTarget(new BigDecimal("5000.00")).status("ACTIVE").build()
        );
        goalRepository.saveAll(mockGoals);

        // 4. Create Subscriptions
        List<Subscription> mockSubs = List.of(
                Subscription.builder().user(demoUser).merchant("Netflix").amount(new BigDecimal("649.00")).billingCycle("MONTHLY").nextBillingDate(today.plusDays(20)).isActive(true).isFlaggedUnused(false).build(),
                Subscription.builder().user(demoUser).merchant("Spotify").amount(new BigDecimal("119.00")).billingCycle("MONTHLY").nextBillingDate(today.plusDays(20)).isActive(true).isFlaggedUnused(false).build(),
                Subscription.builder().user(demoUser).merchant("Gold Gym").amount(new BigDecimal("2500.00")).billingCycle("MONTHLY").nextBillingDate(today.plusDays(5)).isActive(true).isFlaggedUnused(true).build()
        );
        subscriptionRepository.saveAll(mockSubs);

        // 5. Create Spending Profile
        SpendingProfile profile = new SpendingProfile();
        profile.setUser(demoUser);
        profile.setPersonalityType("Weekend Spender");
        profile.setExplanation("Most of your recreational and delivery purchases occur between Friday evening and Sunday night. You show excellent discipline on weekdays, but weekend outings account for 45% of your discretionary leaks.");
        profile.setHealthScore(82);
        profile.setHealthScoreExplanation("Your score is supported by a stable emergency cushion (83% funded) and low utility debt. However, frequent food delivery apps and impulse shopping late at night hold you back from a 90+ tier.");
        spendingProfileRepository.save(profile);

        log.info("Demo data seeding completed successfully! User: {}, Expenses: {}, Goals: {}, Profile saved.",
                demoUser.getEmail(), mockExpenses.size(), mockGoals.size());
    }
}
