package com.finpilot.ai.controller;

import com.finpilot.ai.model.User;
import com.finpilot.ai.model.UserSettings;
import com.finpilot.ai.repository.UserRepository;
import com.finpilot.ai.repository.UserSettingsRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@CrossOrigin
public class SettingsController {

    private final UserRepository userRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.finpilot.ai.repository.ExpenseRepository expenseRepository;
    private final com.finpilot.ai.repository.MonthlyIncomeRepository monthlyIncomeRepository;

    private User getAuthenticatedUser(Principal principal) {
        if (principal == null) return null;
        return userRepository.findByEmail(principal.getName()).orElse(null);
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Principal principal) {
        User user = getAuthenticatedUser(principal);
        if (user == null) return ResponseEntity.status(401).build();

        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate lastMonthDate = today.minusMonths(1);
        int prevMonth = lastMonthDate.getMonthValue();
        int prevYear = lastMonthDate.getYear();

        java.util.Optional<com.finpilot.ai.model.MonthlyIncome> prevIncomeOpt = monthlyIncomeRepository.findByUserIdAndMonthAndYear(user.getId(), prevMonth, prevYear);
        java.math.BigDecimal prevIncome = prevIncomeOpt.map(com.finpilot.ai.model.MonthlyIncome::getIncome).orElse(java.math.BigDecimal.ZERO);

        java.util.List<com.finpilot.ai.model.Expense> expenses = expenseRepository.findByUserId(user.getId());
        java.math.BigDecimal prevExpense = expenses.stream()
            .filter(e -> e.getTransactionDate().getMonthValue() == prevMonth && e.getTransactionDate().getYear() == prevYear)
            .map(com.finpilot.ai.model.Expense::getAmount)
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        java.math.BigDecimal prevSavings;
        double prevSavingsRate;
        if (prevIncome.compareTo(java.math.BigDecimal.ZERO) == 0) {
            prevSavings = java.math.BigDecimal.ZERO;
            prevSavingsRate = 0.0;
        } else {
            prevSavings = prevIncome.subtract(prevExpense);
            prevSavingsRate = prevSavings.multiply(new java.math.BigDecimal(100)).divide(prevIncome, 1, java.math.RoundingMode.HALF_UP).doubleValue();
        }

        // Include basic financial summary fields for Profile overview
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("email", user.getEmail());
        response.put("firstName", user.getFirstName() != null ? user.getFirstName() : "");
        response.put("lastName", user.getLastName() != null ? user.getLastName() : "");
        response.put("phoneNumber", user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
        response.put("profilePicture", user.getProfilePicture() != null ? user.getProfilePicture() : "");
        response.put("memberSince", user.getCreatedAt());
        response.put("accountType", user.getAccountType());
        
        // Calculated previous month financial summary (all from real data)
        response.put("previousMonthIncome", prevIncome);
        response.put("previousMonthExpense", prevExpense);
        response.put("previousMonthSavings", prevSavings);
        response.put("savingsPercentage", prevIncome.compareTo(java.math.BigDecimal.ZERO) == 0 ? null : prevSavingsRate);
        response.put("financialHealthScore", null); // Computed by spending profile engine, not hardcoded

        // Real expense breakdown from previous month data
        java.util.Map<String, java.math.BigDecimal> realBreakdown = expenses.stream()
            .filter(e -> e.getTransactionDate().getMonthValue() == prevMonth && e.getTransactionDate().getYear() == prevYear)
            .collect(java.util.stream.Collectors.groupingBy(
                e -> e.getCategory() != null ? e.getCategory() : "Others",
                java.util.stream.Collectors.reducing(java.math.BigDecimal.ZERO, com.finpilot.ai.model.Expense::getAmount, java.math.BigDecimal::add)
            ));
        response.put("expenseBreakdown", realBreakdown.isEmpty() ? null : realBreakdown);

        // AI insight from real savings data only
        if (prevIncome.compareTo(java.math.BigDecimal.ZERO) == 0) {
            response.put("aiInsight", null);
        } else if (prevSavings.compareTo(java.math.BigDecimal.ZERO) > 0) {
            response.put("aiInsight", String.format("Last month you saved ₹%s (%.1f%% of your income). Great discipline!",
                prevSavings.setScale(0, java.math.RoundingMode.HALF_UP), prevSavingsRate));
        } else {
            response.put("aiInsight", String.format("Last month your expenses exceeded your income by ₹%s. Consider reviewing your spending.",
                prevSavings.negate().setScale(0, java.math.RoundingMode.HALF_UP)));
        }

        // Journey card: computed from real data only
        String highestSpendingCat = realBreakdown.entrySet().stream()
            .max(java.util.Map.Entry.comparingByValue())
            .map(java.util.Map.Entry::getKey).orElse(null);
        long totalExpenseCount = expenses.size();
        response.put("journeyCard", java.util.Map.of(
            "totalExpenses", totalExpenseCount,
            "highestSpendingCategory", highestSpendingCat != null ? highestSpendingCat : "N/A",
            "hasData", totalExpenseCount > 0
        ));

        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> payload, Principal principal) {
        User user = getAuthenticatedUser(principal);
        if (user == null) return ResponseEntity.status(401).build();

        if (payload.containsKey("firstName")) user.setFirstName(payload.get("firstName"));
        if (payload.containsKey("lastName")) user.setLastName(payload.get("lastName"));
        if (payload.containsKey("profilePicture")) user.setProfilePicture(payload.get("profilePicture"));
        // email and phone are not editable as per requirements

        userRepository.save(user);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request, Principal principal) {
        User user = getAuthenticatedUser(principal);
        if (user == null) return ResponseEntity.status(401).build();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "Incorrect current password"));
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "New passwords do not match"));
        }

        if (!AuthController.isValidPassword(request.getNewPassword())) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "Password must contain at least one uppercase letter, one lowercase letter, one number, one special character, and be at least 8 characters long."));
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(java.util.Map.of("message", "Password changed successfully"));
    }

    @GetMapping("/preferences")
    public ResponseEntity<?> getPreferences(Principal principal) {
        User user = getAuthenticatedUser(principal);
        if (user == null) return ResponseEntity.status(401).build();

        UserSettings settings = user.getUserSettings();
        if (settings == null) {
            settings = UserSettings.builder().user(user).build();
            user.setUserSettings(settings);
            userRepository.save(user);
        }

        return ResponseEntity.ok(settings);
    }

    @PutMapping("/preferences")
    public ResponseEntity<?> updatePreferences(@RequestBody UserSettings updatedSettings, Principal principal) {
        User user = getAuthenticatedUser(principal);
        if (user == null) return ResponseEntity.status(401).build();

        UserSettings settings = user.getUserSettings();
        if (settings == null) {
            settings = new UserSettings();
            settings.setUser(user);
        }

        // AI Preferences
        if (updatedSettings.getAiCoachEnabled() != null) settings.setAiCoachEnabled(updatedSettings.getAiCoachEnabled());
        if (updatedSettings.getAiDailySummary() != null) settings.setAiDailySummary(updatedSettings.getAiDailySummary());
        if (updatedSettings.getAiWeeklyReport() != null) settings.setAiWeeklyReport(updatedSettings.getAiWeeklyReport());
        if (updatedSettings.getAiMonthlyStory() != null) settings.setAiMonthlyStory(updatedSettings.getAiMonthlyStory());
        if (updatedSettings.getAiDecisionSimulator() != null) settings.setAiDecisionSimulator(updatedSettings.getAiDecisionSimulator());
        if (updatedSettings.getAiNotifications() != null) settings.setAiNotifications(updatedSettings.getAiNotifications());
        if (updatedSettings.getAiCoachingStyle() != null) settings.setAiCoachingStyle(updatedSettings.getAiCoachingStyle());

        // Financial Preferences
        if (updatedSettings.getSalaryDate() != null) settings.setSalaryDate(updatedSettings.getSalaryDate());
        if (updatedSettings.getDailyBudget() != null) settings.setDailyBudget(updatedSettings.getDailyBudget());
        if (updatedSettings.getMonthlyBudget() != null) settings.setMonthlyBudget(updatedSettings.getMonthlyBudget());
        if (updatedSettings.getSavingsGoal() != null) settings.setSavingsGoal(updatedSettings.getSavingsGoal());

        // Notifications
        if (updatedSettings.getDailyReminder() != null) settings.setDailyReminder(updatedSettings.getDailyReminder());
        if (updatedSettings.getBudgetAlert() != null) settings.setBudgetAlert(updatedSettings.getBudgetAlert());
        if (updatedSettings.getGoalReminder() != null) settings.setGoalReminder(updatedSettings.getGoalReminder());
        if (updatedSettings.getWeeklyReport() != null) settings.setWeeklyReport(updatedSettings.getWeeklyReport());
        if (updatedSettings.getMonthlyReport() != null) settings.setMonthlyReport(updatedSettings.getMonthlyReport());
        if (updatedSettings.getSubscriptionReminder() != null) settings.setSubscriptionReminder(updatedSettings.getSubscriptionReminder());
        if (updatedSettings.getAiFinancialTips() != null) settings.setAiFinancialTips(updatedSettings.getAiFinancialTips());

        // Appearance
        if (updatedSettings.getTheme() != null) settings.setTheme(updatedSettings.getTheme());
        if (updatedSettings.getAccentColor() != null) settings.setAccentColor(updatedSettings.getAccentColor());
        if (updatedSettings.getFontSize() != null) settings.setFontSize(updatedSettings.getFontSize());
        if (updatedSettings.getLanguage() != null) settings.setLanguage(updatedSettings.getLanguage());

        userSettingsRepository.save(settings);
        return ResponseEntity.ok(settings);
    }

    @GetMapping("/export")
    public ResponseEntity<?> exportData(Principal principal) {
        User user = getAuthenticatedUser(principal);
        if (user == null) return ResponseEntity.status(401).build();

        // Stubbed export data response. Returns a mock URL or success message.
        return ResponseEntity.ok(Map.of("message", "Export request initiated successfully.", "url", "/download/mock-export"));
    }

    @DeleteMapping("/account")
    public ResponseEntity<?> deleteAccount(Principal principal) {
        User user = getAuthenticatedUser(principal);
        if (user == null) return ResponseEntity.status(401).build();
        userRepository.delete(user);
        return ResponseEntity.ok("Account deleted successfully.");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // Handled entirely by client-side token destruction
        return ResponseEntity.ok("Logout successful");
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;
        private String confirmPassword;
    }
}
