package com.finpilot.ai.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "expenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "raw_input", columnDefinition = "TEXT")
    private String rawInput;

    private String merchant;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Builder.Default
    @Column(precision = 15, scale = 2)
    private BigDecimal gst = BigDecimal.ZERO;

    @Column(nullable = false)
    private String category;

    private String subcategory;

    @Column(name = "meal_type")
    private String mealType;

    @Column(name = "expense_type", nullable = false)
    private String expenseType; // Essential, Non-Essential

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "transaction_time")
    private LocalTime transactionTime;

    private String location;

    @Column(name = "source")
    private String source; // TEXT, VOICE, RECEIPT, MANUAL

    @Column(name = "confidence_score", precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "is_recurring")
    @Builder.Default
    private Boolean isRecurring = false;

    @Column(name = "is_impulse")
    @Builder.Default
    private Boolean isImpulse = false;

    @Column(name = "ai_context_summary", columnDefinition = "TEXT")
    private String aiContextSummary;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
