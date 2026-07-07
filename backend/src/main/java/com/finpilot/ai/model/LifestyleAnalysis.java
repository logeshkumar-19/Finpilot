package com.finpilot.ai.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lifestyle_analyses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LifestyleAnalysis {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "personality_type")
    private String personalityType;

    @Column(name = "biggest_spending_habit")
    private String biggestSpendingHabit;

    @Column(name = "positive_habit")
    private String positiveHabit;

    @Column(name = "improvement_suggestion")
    private String improvementSuggestion;

    @Column(name = "savings_opportunity")
    private String savingsOpportunity;

    @Column(name = "wellness_score")
    private Integer wellnessScore;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "is_dirty", nullable = false)
    @Builder.Default
    private boolean isDirty = true;
}
