package com.finpilot.ai.controller;

import com.finpilot.ai.model.*;
import com.finpilot.ai.repository.*;
import com.finpilot.ai.service.AIService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ExpenseController {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final AIService aiService;
    private final LifestyleAnalysisRepository lifestyleAnalysisRepository;

    private void invalidateLifestyleAnalysisCache(User user) {
        lifestyleAnalysisRepository.findByUserId(user.getId()).ifPresent(analysis -> {
            analysis.setDirty(true);
            lifestyleAnalysisRepository.save(analysis);
        });
    }

    private User getAuthenticatedUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + email));
    }

    @GetMapping
    public ResponseEntity<List<Expense>> getUserExpenses() {
        User user = getAuthenticatedUser();
        List<Expense> expenses = expenseRepository.findByUserIdOrderByTransactionDateDesc(user.getId());
        return ResponseEntity.ok(expenses);
    }

    @PostMapping
    public ResponseEntity<Expense> createExpense(@RequestBody Expense expense) {
        User user = getAuthenticatedUser();
        expense.setUser(user);
        
        // Set default values if not provided
        if (expense.getTransactionDate() == null) {
            expense.setTransactionDate(LocalDate.now());
        }
        if (expense.getTransactionTime() == null) {
            expense.setTransactionTime(LocalTime.now());
        }
        if (expense.getExpenseType() == null) {
            expense.setExpenseType("Non-Essential");
        }
        if (expense.getCategory() == null) {
            expense.setCategory("Other");
        }
        if (expense.getGst() == null) {
            expense.setGst(BigDecimal.ZERO);
        }
        if (expense.getIsRecurring() == null) {
            expense.setIsRecurring(false);
        }
        if (expense.getIsImpulse() == null) {
            expense.setIsImpulse(false);
        }
        
        Expense saved = expenseRepository.save(expense);
        invalidateLifestyleAnalysisCache(user);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/parse-text")
    public ResponseEntity<Expense> parseTextExpense(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be empty");
        }

        User user = getAuthenticatedUser();
        Expense expense = parseExpenseFromText(text.trim(), user);
        
        Expense saved = expenseRepository.save(expense);
        invalidateLifestyleAnalysisCache(user);
        return ResponseEntity.ok(saved);
    }

    private Expense parseExpenseFromText(String text, User user) {
        Expense expense = Expense.builder()
                .user(user)
                .rawInput(text)
                .transactionDate(LocalDate.now())
                .transactionTime(LocalTime.now())
                .gst(BigDecimal.ZERO)
                .isRecurring(false)
                .isImpulse(false)
                .build();
        
        // Pattern to extract amount (supports ₹, Rs, INR, or just numbers)
        Pattern amountPattern = Pattern.compile("(?:₹|Rs\\.?|INR)?\\s*(\\d+(?:\\.\\d{1,2})?)");
        Matcher amountMatcher = amountPattern.matcher(text);
        if (amountMatcher.find()) {
            expense.setAmount(new BigDecimal(amountMatcher.group(1)));
        } else {
            // Try to find any number as amount
            Pattern numberPattern = Pattern.compile("(\\d+(?:\\.\\d{1,2})?)");
            Matcher numberMatcher = numberPattern.matcher(text);
            if (numberMatcher.find()) {
                expense.setAmount(new BigDecimal(numberMatcher.group(1)));
            } else {
                // Default if no amount found
                expense.setAmount(BigDecimal.ZERO);
            }
        }

        // Try to extract merchant name (words that are not numbers or common stop words)
        String[] words = text.split("\\s+");
        StringBuilder merchantBuilder = new StringBuilder();
        for (String word : words) {
            if (!word.matches(".*\\d.*") && 
                !word.matches("(?:₹|Rs\\.?|INR)") &&
                !word.matches("(?i)(?:at|for|on|in|to|from|with|and|or|but|at|paid|spent|on|for|rupees|rs)")) {
                if (merchantBuilder.length() > 0) merchantBuilder.append(" ");
                merchantBuilder.append(word);
            }
        }
        String merchant = merchantBuilder.toString().trim();
        if (merchant.isEmpty()) {
            merchant = "General Expense";
        }
        expense.setMerchant(merchant);

        // Determine category based on keywords
        String lowerText = text.toLowerCase();
        if (lowerText.contains("food") || lowerText.contains("pizza") || lowerText.contains("coffee") || 
            lowerText.contains("restaurant") || lowerText.contains("cafe") || lowerText.contains("lunch") || 
            lowerText.contains("dinner") || lowerText.contains("breakfast") || lowerText.contains("starbucks") ||
            lowerText.contains("mcdonald") || lowerText.contains("kfc") || lowerText.contains("burger")) {
            expense.setCategory("Food & Dining");
            expense.setExpenseType("Non-Essential");
            expense.setSubcategory("Restaurant");
            
            // Determine meal type
            if (lowerText.contains("breakfast")) {
                expense.setMealType("Breakfast");
            } else if (lowerText.contains("lunch")) {
                expense.setMealType("Lunch");
            } else if (lowerText.contains("dinner")) {
                expense.setMealType("Dinner");
            } else {
                expense.setMealType("Snacks");
            }
        } else if (lowerText.contains("uber") || lowerText.contains("ola") || lowerText.contains("taxi") || 
                   lowerText.contains("cab") || lowerText.contains("auto") || lowerText.contains("petrol") || 
                   lowerText.contains("fuel") || lowerText.contains("gas") || lowerText.contains("metro") ||
                   lowerText.contains("bus") || lowerText.contains("train")) {
            expense.setCategory("Transportation");
            expense.setExpenseType("Essential");
            if (lowerText.contains("uber") || lowerText.contains("ola") || lowerText.contains("taxi")) {
                expense.setSubcategory("Ride Sharing");
            } else {
                expense.setSubcategory("Fuel");
            }
        } else if (lowerText.contains("amazon") || lowerText.contains("flipkart") || lowerText.contains("shopping") || 
                   lowerText.contains("clothes") || lowerText.contains("dress") || lowerText.contains("shirt") || 
                   lowerText.contains("pants") || lowerText.contains("shoes") || lowerText.contains("electronics") ||
                   lowerText.contains("laptop") || lowerText.contains("phone") || lowerText.contains("gadget")) {
            expense.setCategory("Shopping");
            expense.setExpenseType("Non-Essential");
            expense.setSubcategory("Retail");
            expense.setIsImpulse(true);
        } else if (lowerText.contains("grocery") || lowerText.contains("vegetable") || lowerText.contains("fruit") || 
                   lowerText.contains("milk") || lowerText.contains("bread") || lowerText.contains("rice") || 
                   lowerText.contains("dal") || lowerText.contains("ration") || lowerText.contains("supermarket")) {
            expense.setCategory("Groceries");
            expense.setExpenseType("Essential");
            expense.setSubcategory("Groceries");
        } else if (lowerText.contains("rent") || lowerText.contains("electricity") || lowerText.contains("bill") || 
                   lowerText.contains("water") || lowerText.contains("internet") || lowerText.contains("phone") || 
                   lowerText.contains("maintenance") || lowerText.contains("electric") || lowerText.contains("gas")) {
            expense.setCategory("Utilities");
            expense.setExpenseType("Essential");
            if (lowerText.contains("rent")) {
                expense.setSubcategory("Rent");
            } else if (lowerText.contains("electricity")) {
                expense.setSubcategory("Electricity");
            } else {
                expense.setSubcategory("Bills");
            }
        } else if (lowerText.contains("movie") || lowerText.contains("netflix") || lowerText.contains("prime") || 
                   lowerText.contains("spotify") || lowerText.contains("entertainment") || lowerText.contains("game") ||
                   lowerText.contains("youtube") || lowerText.contains("subscription")) {
            expense.setCategory("Entertainment");
            expense.setExpenseType("Non-Essential");
            expense.setSubcategory("Subscription");
        } else if (lowerText.contains("salary") || lowerText.contains("income") || lowerText.contains("credit")) {
            // This is likely income, not an expense
            expense.setCategory("Income");
            expense.setExpenseType("Essential");
            expense.setSubcategory("Salary");
        } else if (lowerText.contains("medical") || lowerText.contains("doctor") || lowerText.contains("hospital") || 
                   lowerText.contains("medicine") || lowerText.contains("health") || lowerText.contains("insurance")) {
            expense.setCategory("Healthcare");
            expense.setExpenseType("Essential");
            expense.setSubcategory("Medical");
        } else if (lowerText.contains("school") || lowerText.contains("college") || lowerText.contains("university") || 
                   lowerText.contains("course") || lowerText.contains("class") || lowerText.contains("education")) {
            expense.setCategory("Education");
            expense.setExpenseType("Essential");
            expense.setSubcategory("Education");
        } else if (lowerText.contains("gym") || lowerText.contains("fitness") || lowerText.contains("yoga") || 
                   lowerText.contains("sports")) {
            expense.setCategory("Fitness");
            expense.setExpenseType("Non-Essential");
            expense.setSubcategory("Fitness");
        } else {
            expense.setCategory("Other");
            expense.setExpenseType("Non-Essential");
            expense.setSubcategory("Miscellaneous");
        }

        // Generate AI context summary
        try {
            String summary = aiService.generateExpenseContext(
                expense.getMerchant(), 
                expense.getAmount(), 
                expense.getCategory()
            );
            expense.setAiContextSummary(summary);
        } catch (Exception e) {
            expense.setAiContextSummary("Expense logged via text parsing from: " + text);
        }

        // Set confidence score based on parsing quality
        BigDecimal confidence = calculateConfidence(text, expense);
        expense.setConfidenceScore(confidence);

        // Try to extract location if mentioned
        String location = extractLocation(text);
        if (location != null) {
            expense.setLocation(location);
        }

        return expense;
    }

    private BigDecimal calculateConfidence(String text, Expense expense) {
        int score = 50; // Base score
        
        // Increase confidence if amount was found
        if (expense.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            score += 20;
        }
        
        // Increase confidence if merchant was found
        if (!expense.getMerchant().equals("General Expense")) {
            score += 15;
        }
        
        // Increase confidence if category was determined
        if (!expense.getCategory().equals("Other")) {
            score += 15;
        }
        
        return new BigDecimal(Math.min(score, 100));
    }

    private String extractLocation(String text) {
        // Simple pattern to extract location (at X, in Y, near Z)
        Pattern locationPattern = Pattern.compile("(?:at|in|near)\\s+([A-Za-z\\s]+)(?:$|\\s+)");
        Matcher matcher = locationPattern.matcher(text.toLowerCase());
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    @PostMapping("/voice")
    public ResponseEntity<Expense> parseVoiceExpense(@RequestParam("file") MultipartFile file) {
        User user = getAuthenticatedUser();
        // In production, you'd use a speech-to-text service
        Expense expense = Expense.builder()
                .user(user)
                .merchant("Voice Entry")
                .amount(new BigDecimal("100"))
                .category("Other")
                .expenseType("Non-Essential")
                .subcategory("Miscellaneous")
                .transactionDate(LocalDate.now())
                .transactionTime(LocalTime.now())
                .gst(BigDecimal.ZERO)
                .source("VOICE")
                .aiContextSummary("Voice expense entry")
                .isRecurring(false)
                .isImpulse(false)
                .confidenceScore(new BigDecimal("70"))
                .build();
        
        Expense saved = expenseRepository.save(expense);
        invalidateLifestyleAnalysisCache(user);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/receipt")
    public ResponseEntity<Expense> parseReceipt(@RequestParam("file") MultipartFile file) {
        User user = getAuthenticatedUser();
        // In production, you'd use OCR (Google Vision API, Tesseract, etc.)
        Expense expense = Expense.builder()
                .user(user)
                .merchant("Receipt Scan")
                .amount(new BigDecimal("250"))
                .category("Shopping")
                .expenseType("Non-Essential")
                .subcategory("Retail")
                .transactionDate(LocalDate.now())
                .transactionTime(LocalTime.now())
                .gst(new BigDecimal("12.50"))
                .source("RECEIPT")
                .aiContextSummary("Receipt processed via OCR")
                .isRecurring(false)
                .isImpulse(false)
                .confidenceScore(new BigDecimal("85"))
                .build();
        
        Expense saved = expenseRepository.save(expense);
        invalidateLifestyleAnalysisCache(user);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpense(@PathVariable Long id) {
        User user = getAuthenticatedUser();
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        
        if (!expense.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized to delete this expense");
        }
        
        expenseRepository.deleteById(id);
        invalidateLifestyleAnalysisCache(user);
        return ResponseEntity.ok().build();
    }
}