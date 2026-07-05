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
        
        // Calculated previous month financial summary
        response.put("previousMonthIncome", prevIncome);
        response.put("previousMonthExpense", prevExpense);
        response.put("previousMonthSavings", prevSavings);
        response.put("savingsPercentage", prevSavingsRate);
        response.put("financialHealthScore", prevIncome.compareTo(java.math.BigDecimal.ZERO) == 0 ? 0 : 82); 
        response.put("expenseBreakdown", Map.of(
            "Food", 1200,
            "Shopping", 800,
            "Bills", 600,
            "Transportation", 400,
            "Others", 520
        ));
        response.put("aiInsight", prevIncome.compareTo(java.math.BigDecimal.ZERO) == 0 ? "No financial data available yet." : String.format("Last month you saved %s%% of your income. Keep up the good work!", prevSavingsRate));
        response.put("journeyCard", Map.of(
            "currentStreak", "18 Days",
            "bestSavingMonth", "June",
            "highestSpendingCategory", "Food",
            "mostImprovedCategory", "Shopping",
            "currentGoalProgress", "Laptop Fund - 68%",
            "aiRecommendation", "Reducing one weekend food delivery each week can help you reach your savings goal approximately two weeks earlier."
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
            return ResponseEntity.badRequest().body("Incorrect current password");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest().body("New passwords do not match");
        }

        // Basic validation logic (8 chars, 1 upper, 1 lower, 1 number, 1 special)
        String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";
        if (!request.getNewPassword().matches(passwordPattern)) {
            return ResponseEntity.badRequest().body("Password does not meet security requirements.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return ResponseEntity.ok("Password changed successfully");
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
