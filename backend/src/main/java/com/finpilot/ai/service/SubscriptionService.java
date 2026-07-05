package com.finpilot.ai.service;

import com.finpilot.ai.model.Expense;
import com.finpilot.ai.model.Subscription;
import com.finpilot.ai.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    public List<Subscription> getSubscriptionsByUserId(Long userId) {
        return subscriptionRepository.findByUserId(userId);
    }

    @Transactional
    public void detectSubscriptionsFromExpenses(Long userId, List<Expense> expenses) {
        // Build a map of potential merchant and amount combinations to count occurrences
        Map<String, List<Expense>> merchantGroups = new HashMap<>();
        for (Expense e : expenses) {
            String key = e.getMerchant().toLowerCase().trim();
            if (key.contains("netflix") || key.contains("spotify") || key.contains("prime") 
                    || key.contains("gym") || key.contains("icloud") || key.contains("cloud") 
                    || key.contains("youtube") || key.contains("canva") || key.contains("adobe")) {
                
                merchantGroups.computeIfAbsent(e.getMerchant(), k -> new ArrayList<>()).add(e);
            }
        }

        List<Subscription> currentSubs = subscriptionRepository.findByUserId(userId);

        for (Map.Entry<String, List<Expense>> entry : merchantGroups.entrySet()) {
            String merchant = entry.getKey();
            List<Expense> group = entry.getValue();
            if (group.isEmpty()) continue;

            // Check if this merchant is already registered in active subscriptions
            boolean exists = currentSubs.stream()
                    .anyMatch(s -> s.getMerchant().equalsIgnoreCase(merchant));

            if (!exists) {
                // Determine nominal amount (usually the latest expense)
                Expense latest = group.stream()
                        .max(Comparator.comparing(Expense::getTransactionDate))
                        .orElse(group.get(0));

                Subscription sub = Subscription.builder()
                        .user(latest.getUser())
                        .merchant(latest.getMerchant())
                        .amount(latest.getAmount())
                        .billingCycle("MONTHLY")
                        .nextBillingDate(latest.getTransactionDate().plusMonths(1))
                        .isActive(true)
                        .isFlaggedUnused(false)
                        .build();

                // Flag duplicates (if another sub exists for the same amount category or if there's multiple billing indicators)
                subscriptionRepository.save(sub);
                log.info("Automatically detected new recurring subscription: {}", merchant);
            }
        }
    }

    @Transactional
    public Subscription toggleSubscriptionStatus(Long subId) {
        Subscription sub = subscriptionRepository.findById(subId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subId));
        sub.setIsActive(!sub.getIsActive());
        return subscriptionRepository.save(sub);
    }

    @Transactional
    public Subscription flagSubscriptionUnused(Long subId, boolean unused) {
        Subscription sub = subscriptionRepository.findById(subId)
                .orElseThrow(() -> new IllegalArgumentException("Subscription not found: " + subId));
        sub.setIsFlaggedUnused(unused);
        return subscriptionRepository.save(sub);
    }
}
