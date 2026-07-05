package com.finpilot.ai.controller;

import com.finpilot.ai.model.Subscription;
import com.finpilot.ai.model.User;
import com.finpilot.ai.repository.UserRepository;
import com.finpilot.ai.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@CrossOrigin
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserRepository userRepository;

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));
    }

    @GetMapping
    public ResponseEntity<List<Subscription>> getSubscriptions() {
        User user = getAuthenticatedUser();
        return ResponseEntity.ok(subscriptionService.getSubscriptionsByUserId(user.getId()));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Subscription> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionService.toggleSubscriptionStatus(id));
    }

    @PatchMapping("/{id}/unused")
    public ResponseEntity<Subscription> toggleUnused(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        Boolean unused = body.getOrDefault("unused", false);
        return ResponseEntity.ok(subscriptionService.flagSubscriptionUnused(id, unused));
    }
}
