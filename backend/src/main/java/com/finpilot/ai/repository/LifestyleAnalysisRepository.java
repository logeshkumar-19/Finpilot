package com.finpilot.ai.repository;

import com.finpilot.ai.model.LifestyleAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface LifestyleAnalysisRepository extends JpaRepository<LifestyleAnalysis, Long> {
    Optional<LifestyleAnalysis> findByUserId(Long userId);
}
