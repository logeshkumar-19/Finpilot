package com.finpilot.ai.repository;

import com.finpilot.ai.model.SpendingProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpendingProfileRepository extends JpaRepository<SpendingProfile, Long> {
}
