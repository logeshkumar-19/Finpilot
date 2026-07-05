package com.finpilot.ai.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseParseResult {
    private String merchant;
    private BigDecimal amount;
    private BigDecimal gst;
    private String category;
    private String subcategory;
    private String mealType;
    private String expenseType; // Essential, Non-Essential
    private LocalDate transactionDate;
    private LocalTime transactionTime;
    private BigDecimal confidenceScore;
    private String aiContextSummary;
}
