package com.finpilot.ai.repository;

import com.finpilot.ai.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByUserId(Long userId);
    List<Expense> findByUserIdOrderByTransactionDateDesc(Long userId);
    List<Expense> findByUserIdAndTransactionDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
    List<Expense> findTop20ByUserIdOrderByTransactionDateDescIdDesc(Long userId);
}
