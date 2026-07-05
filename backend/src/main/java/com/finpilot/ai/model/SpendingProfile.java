package com.finpilot.ai.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "spending_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpendingProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "personality_type", length = 50)
    private String personalityType;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "health_score")
    private Integer healthScore;

    @Column(name = "health_score_explanation", columnDefinition = "TEXT")
    private String healthScoreExplanation;

    @Column(name = "last_analyzed_at")
    @Builder.Default
    private LocalDateTime lastAnalyzedAt = LocalDateTime.now();
}
