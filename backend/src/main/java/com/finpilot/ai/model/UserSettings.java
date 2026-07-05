package com.finpilot.ai.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "user_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // AI Preferences
    @Column(name = "ai_coach_enabled")
    @Builder.Default
    private Boolean aiCoachEnabled = true;

    @Column(name = "ai_daily_summary")
    @Builder.Default
    private Boolean aiDailySummary = true;

    @Column(name = "ai_weekly_report")
    @Builder.Default
    private Boolean aiWeeklyReport = true;

    @Column(name = "ai_monthly_story")
    @Builder.Default
    private Boolean aiMonthlyStory = true;

    @Column(name = "ai_decision_simulator")
    @Builder.Default
    private Boolean aiDecisionSimulator = true;

    @Column(name = "ai_notifications")
    @Builder.Default
    private Boolean aiNotifications = true;

    @Column(name = "ai_coaching_style")
    @Builder.Default
    private String aiCoachingStyle = "Friendly";

    // Financial Preferences
    @Column(name = "salary_date")
    @Builder.Default
    private Integer salaryDate = 1;

    @Column(name = "daily_budget", precision = 15, scale = 2)
    private BigDecimal dailyBudget;

    @Column(name = "monthly_budget", precision = 15, scale = 2)
    private BigDecimal monthlyBudget;

    @Column(name = "savings_goal", precision = 15, scale = 2)
    private BigDecimal savingsGoal;

    // Notifications
    @Column(name = "daily_reminder")
    @Builder.Default
    private Boolean dailyReminder = true;

    @Column(name = "budget_alert")
    @Builder.Default
    private Boolean budgetAlert = true;

    @Column(name = "goal_reminder")
    @Builder.Default
    private Boolean goalReminder = true;

    @Column(name = "weekly_report")
    @Builder.Default
    private Boolean weeklyReport = true;

    @Column(name = "monthly_report")
    @Builder.Default
    private Boolean monthlyReport = true;

    @Column(name = "subscription_reminder")
    @Builder.Default
    private Boolean subscriptionReminder = true;

    @Column(name = "ai_financial_tips")
    @Builder.Default
    private Boolean aiFinancialTips = true;

    // Appearance
    @Column(name = "theme")
    @Builder.Default
    private String theme = "System";

    @Column(name = "accent_color")
    @Builder.Default
    private String accentColor = "Blue";

    @Column(name = "font_size")
    @Builder.Default
    private String fontSize = "Medium";

    @Column(name = "language")
    @Builder.Default
    private String language = "English";
}
