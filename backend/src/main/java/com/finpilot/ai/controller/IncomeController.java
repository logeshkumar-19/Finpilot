package com.finpilot.ai.controller;

import com.finpilot.ai.model.MonthlyIncome;
import com.finpilot.ai.model.User;
import com.finpilot.ai.repository.MonthlyIncomeRepository;
import com.finpilot.ai.repository.UserRepository;
import com.finpilot.ai.repository.LifestyleAnalysisRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/income")
@RequiredArgsConstructor
@CrossOrigin
public class IncomeController {

    private final UserRepository userRepository;
    private final MonthlyIncomeRepository monthlyIncomeRepository;
    private final LifestyleAnalysisRepository lifestyleAnalysisRepository;

    private void invalidateLifestyleAnalysisCache(User user) {
        if (user == null) return;
        lifestyleAnalysisRepository.findByUserId(user.getId()).ifPresent(analysis -> {
            analysis.setDirty(true);
            lifestyleAnalysisRepository.save(analysis);
        });
    }

    private User getAuthenticatedUser(Principal principal) {
        if (principal == null) return null;
        return userRepository.findByEmail(principal.getName()).orElse(null);
    }

    @PostMapping
    public ResponseEntity<?> createIncome(@RequestBody IncomeRequest request, Principal principal) {
        User user = getAuthenticatedUser(principal);
        if (user == null) return ResponseEntity.status(401).build();

        LocalDate today = LocalDate.now();
        int currentMonth = today.getMonthValue();
        int currentYear = today.getYear();

        Optional<MonthlyIncome> existing = monthlyIncomeRepository.findByUserIdAndMonthAndYear(user.getId(), currentMonth, currentYear);
        if (existing.isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Income for this month already exists. Use PUT to update."));
        }

        Integer salaryDate = request.getSalaryDate();
        if (salaryDate == null && user.getUserSettings() != null) {
            salaryDate = user.getUserSettings().getSalaryDate();
        }

        MonthlyIncome income = MonthlyIncome.builder()
                .user(user)
                .month(currentMonth)
                .year(currentYear)
                .income(request.getIncome())
                .salaryDate(salaryDate)
                .incomeType(request.getIncomeType())
                .build();

        monthlyIncomeRepository.save(income);
        invalidateLifestyleAnalysisCache(user);
        return ResponseEntity.ok(income);
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentIncome(Principal principal) {
        User user = getAuthenticatedUser(principal);
        if (user == null) return ResponseEntity.status(401).build();

        LocalDate today = LocalDate.now();
        Optional<MonthlyIncome> income = monthlyIncomeRepository.findByUserIdAndMonthAndYear(user.getId(), today.getMonthValue(), today.getYear());
        
        if (income.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(income.get());
    }

    @PutMapping("/current")
    public ResponseEntity<?> updateCurrentIncome(@RequestBody IncomeRequest request, Principal principal) {
        User user = getAuthenticatedUser(principal);
        if (user == null) return ResponseEntity.status(401).build();

        LocalDate today = LocalDate.now();
        Optional<MonthlyIncome> incomeOpt = monthlyIncomeRepository.findByUserIdAndMonthAndYear(user.getId(), today.getMonthValue(), today.getYear());
        
        if (incomeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        MonthlyIncome income = incomeOpt.get();
        if (request.getIncome() != null) {
            income.setIncome(request.getIncome());
        }
        if (request.getSalaryDate() != null) {
            income.setSalaryDate(request.getSalaryDate());
        }
        if (request.getIncomeType() != null) {
            income.setIncomeType(request.getIncomeType());
        }
        
        monthlyIncomeRepository.save(income);
        invalidateLifestyleAnalysisCache(user);
        return ResponseEntity.ok(income);
    }

    @GetMapping("/history")
    public ResponseEntity<List<MonthlyIncome>> getIncomeHistory(Principal principal) {
        User user = getAuthenticatedUser(principal);
        if (user == null) return ResponseEntity.status(401).build();

        return ResponseEntity.ok(monthlyIncomeRepository.findByUserIdOrderByYearDescMonthDesc(user.getId()));
    }

    @Getter
    @Setter
    public static class IncomeRequest {
        private BigDecimal income;
        private Integer salaryDate;
        private String incomeType;
    }
}
