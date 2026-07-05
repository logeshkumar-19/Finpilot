package com.finpilot.ai.repository;

import com.finpilot.ai.model.MonthlyIncome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonthlyIncomeRepository extends JpaRepository<MonthlyIncome, Long> {
    Optional<MonthlyIncome> findByUserIdAndMonthAndYear(Long userId, int month, int year);
    List<MonthlyIncome> findByUserIdOrderByYearDescMonthDesc(Long userId);
}
