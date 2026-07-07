package com.finpilot.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finpilot.ai.dto.ExpenseParseResult;
import com.finpilot.ai.dto.SimulationResponse;
import com.finpilot.ai.model.Expense;
import com.finpilot.ai.model.FinancialGoal;
import com.finpilot.ai.model.SpendingProfile;
import com.finpilot.ai.model.LifestyleAnalysis;
import com.finpilot.ai.model.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIService {

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    // Groq is the primary AI provider (free, fast, OpenAI-compatible)
    @Value("${ai.groq.api-key:mock-key}")
    private String groqApiKey;

    @Value("${ai.groq.model:llama-3.3-70b-versatile}")
    private String groqModel;

    @Value("${ai.groq.base-url:https://api.groq.com/openai/v1}")
    private String groqBaseUrl;

    /**
     * Parse raw expense string using AI (with fallback rules engine)
     */
    public ExpenseParseResult parseExpenseQuickInput(String text, String userTimeContext) {
        log.info("Parsing expense input: '{}'", text);
        
        if (isMockApiKey()) {
            return fallbackParseExpense(text, userTimeContext);
        }

        try {
            String systemPrompt = "You are FinPilot AI expense categorizer.\n" +
                    "Parse raw user text or receipt transcription and output strict JSON matching the schema.\n" +
                    "Meal rules based on transaction time:\n" +
                    "- 05:00 to 10:00: Breakfast\n" +
                    "- 10:00 to 12:00: Morning Snack\n" +
                    "- 12:00 to 15:00: Lunch\n" +
                    "- 15:00 to 18:00: Evening Snack\n" +
                    "- 18:00 to 22:00: Dinner\n" +
                    "- After 22:00: Late Night Meal\n" +
                    "Categorize as Food, Shopping, Travel, Utilities, Subscriptions, Entertainment, Medical, Other.\n" +
                    "Identify if the transaction is Non-essential (desires, restaurant, leisure, shopping) vs Essential (bills, groceries, transport, medical).\n" +
                    "Formulate a friendly, brief, context-card summary analysis for this specific purchase in 'aiContextSummary'.\n" +
                    "Schema:\n" +
                    "{\n" +
                    "  \"merchant\": \"String\",\n" +
                    "  \"amount\": 0.00,\n" +
                    "  \"gst\": 0.00,\n" +
                    "  \"category\": \"String\",\n" +
                    "  \"subcategory\": \"String\",\n" +
                    "  \"mealType\": \"String\",\n" +
                    "  \"expenseType\": \"String\",\n" +
                    "  \"transactionDate\": \"YYYY-MM-DD\",\n" +
                    "  \"transactionTime\": \"HH:MM:SS\",\n" +
                    "  \"confidenceScore\": 0.95,\n" +
                    "  \"aiContextSummary\": \"String\"\n" +
                    "}";

            String userContent = String.format("User input: \"%s\"\nUser Context Time: %s", text, userTimeContext);
            String jsonResponse = queryGroq(systemPrompt, userContent, true);

            JsonNode root = objectMapper.readTree(jsonResponse);
            
            LocalDate txnDate = root.has("transactionDate") ? 
                    LocalDate.parse(root.get("transactionDate").asText()) : LocalDate.now();
            LocalTime txnTime = root.has("transactionTime") ? 
                    LocalTime.parse(root.get("transactionTime").asText()) : LocalTime.now();

            return ExpenseParseResult.builder()
                    .merchant(root.path("merchant").asText("Unknown Merchant"))
                    .amount(new BigDecimal(root.path("amount").asText("0")))
                    .gst(new BigDecimal(root.path("gst").asText("0")))
                    .category(root.path("category").asText("Other"))
                    .subcategory(root.path("subcategory").asText("Miscellaneous"))
                    .mealType(root.path("mealType").asText("None"))
                    .expenseType(root.path("expenseType").asText("Non-Essential"))
                    .transactionDate(txnDate)
                    .transactionTime(txnTime)
                    .confidenceScore(new BigDecimal(root.path("confidenceScore").asText("0.90")))
                    .aiContextSummary(root.path("aiContextSummary").asText("Logged automatically."))
                    .build();

        } catch (Exception e) {
            log.error("AI parsing failed, falling back to rules engine", e);
            return fallbackParseExpense(text, userTimeContext);
        }
    }

    /**
     * Simulates a purchase decision impact on goals
     */
    public SimulationResponse simulatePurchase(String itemName, BigDecimal itemCost, BigDecimal monthlySavings, List<FinancialGoal> goals) {
        log.info("Simulating purchase of {} with cost {}", itemName, itemCost);

        if (isMockApiKey()) {
            return calculateDeterministicSimulation(itemName, itemCost, monthlySavings, goals);
        }

        try {
            String systemPrompt = "You are the FinPilot Simulation Engine.\n" +
                    "Perform quantitative calculations on the planned purchase and output strict JSON matching the schema.\n" +
                    "Simulate these 4 scenarios:\n" +
                    "- Scenario A: Buy Today (using current savings, delaying goals)\n" +
                    "- Scenario B: Postpone 3 months (saving monthly for it)\n" +
                    "- Scenario C: Buy Second-Hand / Alternative (saving 25% on price)\n" +
                    "- Scenario D: EMI (paying over 12 months with 10% rate, lowering savings rate)\n" +
                    "Return detailed calculations of timeline impact and monthly costs.\n" +
                    "Output JSON Schema:\n" +
                    "{\n" +
                    "  \"recommendation\": \"Written analysis and best choice reasoning\",\n" +
                    "  \"scenarioA\": { \"name\": \"Buy Today\", \"description\": \"Draw down from current reserves.\", \"monthlyCost\": 0, \"goalDelayMonths\": 2, \"totalCost\": 80000, \"impactAnalysis\": \"Delays laptop goal by 2 months due to capital depletion.\" },\n" +
                    "  \"scenarioB\": { \"name\": \"Buy in 3 Months\", \"description\": \"Postpone and save.\", \"monthlyCost\": 26666, \"goalDelayMonths\": 0, \"totalCost\": 80000, \"impactAnalysis\": \"Allows saving targets to remain active without delays.\" },\n" +
                    "  \"scenarioC\": { \"name\": \"Used/Alternative\", \"description\": \"Find alternative at 25% discount.\", \"monthlyCost\": 0, \"goalDelayMonths\": 1, \"totalCost\": 60000, \"impactAnalysis\": \"Saves money, minimal delay of 10 days on goals.\" },\n" +
                    "  \"scenarioD\": { \"name\": \"12-Month EMI\", \"description\": \"Pay in installments.\", \"monthlyCost\": 7000, \"goalDelayMonths\": 3, \"totalCost\": 84000, \"impactAnalysis\": \"Reduces monthly investable surplus by 15%.\"\n" +
                    "  }\n" +
                    "}";

            StringBuilder goalsDesc = new StringBuilder();
            for (FinancialGoal g : goals) {
                goalsDesc.append(String.format("- Goal: %s, Target: %s, Current: %s, Target Date: %s\n", 
                        g.getName(), g.getTargetAmount(), g.getCurrentAmount(), g.getTargetDate()));
            }

            String userContent = String.format("Item: %s\nCost: %s\nMonthly Savings Capacity: %s\nActive Goals:\n%s",
                    itemName, itemCost, monthlySavings, goalsDesc);

            String jsonResponse = queryGroq(systemPrompt, userContent, true);
            return objectMapper.readValue(jsonResponse, SimulationResponse.class);

        } catch (Exception e) {
            log.error("AI simulation failed, falling back to rule-based engine", e);
            return calculateDeterministicSimulation(itemName, itemCost, monthlySavings, goals);
        }
    }

    /**
     * Financial Coach Chat Session with RAG-based context
     */
    public String askCoach(String message, List<Expense> history, List<FinancialGoal> goals, SpendingProfile profile, BigDecimal monthlyIncome) {
        if (isMockApiKey()) {
            return generateMockCoachReply(message, history, goals, profile, monthlyIncome);
        }

        try {
            String systemPrompt = "You are FinPilot, a supportive, highly knowledgeable, and personalized AI Financial Coach. " +
                    "Your primary goal is to accurately answer the user's specific question based on their provided context. " +
                    "Do NOT just summarize the data unless explicitly asked. " +
                    "Only reference their transaction data, goals, or spending profile if it is relevant to their question.\n" +
                    "Follow these style rules:\n" +
                    "- Directly answer the user's specific question first.\n" +
                    "- Keep answers concise, direct, and actionable.\n" +
                    "- Do not recommend simple generic ideas like 'spend less'. Provide numbers based on their data if applicable.\n" +
                    "- Use bullet points or Markdown tables if summarizing categories.\n" +
                    "- Give helpful suggestions if relevant to their inquiry.";

            StringBuilder context = new StringBuilder();
            context.append(String.format("User Profile: Income Budget: %s, Current Health Score: %d, Personality: %s\n", 
                    monthlyIncome, profile != null ? profile.getHealthScore() : 80, profile != null ? profile.getPersonalityType() : "Saver"));
            context.append("Current Goals:\n");
            for (FinancialGoal g : goals) {
                context.append(String.format("- Goal %s: Target: %s, Current: %s, Target Date: %s\n", g.getName(), g.getTargetAmount(), g.getCurrentAmount(), g.getTargetDate()));
            }
            context.append("Recent Expenses:\n");
            for (Expense e : history) {
                context.append(String.format("- Date: %s, Merchant: %s, Amt: %s, Category: %s (%s, %s)\n",
                        e.getTransactionDate(), e.getMerchant(), e.getAmount(), e.getCategory(), e.getMealType(), e.getExpenseType()));
            }
            context.append("\nUser message: ").append(message);

            return queryGroq(systemPrompt, context.toString(), false);

        } catch (Exception e) {
            log.error("AI chat failed", e);
            return generateMockCoachReply(message, history, goals, profile, monthlyIncome);
        }
    }

    /**
     * Dynamically compute spending profile parameters (Personality, Health Score)
     */
    public SpendingProfile analyzeSpendingPatterns(List<Expense> expenses, UserSpendingStats stats) {
        log.info("Analyzing spending patterns for user. Total expenses count: {}", expenses.size());
        
        int healthScore = 75; // Default baseline
        String personality = "Balanced Spender";
        String explanation = "You manage a balanced budget, tracking essential needs while leaving room for occasional treats.";
        String healthExplanation = "Score reflects steady emergency fund progress and low default debt levels.";

        if (!expenses.isEmpty()) {
            BigDecimal totalSpent = BigDecimal.ZERO;
            BigDecimal nonEssentialSpent = BigDecimal.ZERO;
            int lateNightCount = 0;
            int impulseCount = 0;

            for (Expense e : expenses) {
                totalSpent = totalSpent.add(e.getAmount());
                if ("Non-Essential".equalsIgnoreCase(e.getExpenseType())) {
                    nonEssentialSpent = nonEssentialSpent.add(e.getAmount());
                }
                if ("Late Night".equalsIgnoreCase(e.getMealType())) {
                    lateNightCount++;
                }
                if (e.getIsImpulse() != null && e.getIsImpulse()) {
                    impulseCount++;
                }
            }

            BigDecimal nonEssentialRatio = totalSpent.compareTo(BigDecimal.ZERO) > 0 ? 
                    nonEssentialSpent.divide(totalSpent, 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            // Simple algorithmic classification
            if (nonEssentialRatio.compareTo(new BigDecimal("0.50")) > 0) {
                personality = "Lifestyle Spender";
                explanation = "A large portion of your outgoing money goes to dining, leisure, and retail. Focus on dialing back non-essential transactions.";
                healthScore -= 15;
            } else if (lateNightCount > 3) {
                personality = "Late Night Explorer";
                explanation = "You frequently purchase meals and items after 10 PM. This is a common trigger for impulse spending.";
                healthScore -= 10;
            } else if (impulseCount > 2) {
                personality = "Impulse Buyer";
                explanation = "You have registered multiple direct retail buys that weren't budgeted beforehand.";
                healthScore -= 12;
            } else if (nonEssentialRatio.compareTo(new BigDecimal("0.20")) < 0) {
                personality = "Minimalist Saver";
                explanation = "Excellent savings focus! Over 80% of your expenses belong strictly to essential needs.";
                healthScore += 15;
            }

            if (stats.getBudgetUtilization().compareTo(new BigDecimal("1.0")) > 0) {
                healthScore -= 10;
                healthExplanation = "Score decreased because you exceeded your declared monthly budget limit.";
            } else {
                healthExplanation = "Great budget discipline! You are comfortably within your monthly limits.";
            }

            healthScore = Math.max(10, Math.min(100, healthScore));
        }

        return SpendingProfile.builder()
                .personalityType(personality)
                .explanation(explanation)
                .healthScore(healthScore)
                .healthScoreExplanation(healthExplanation)
                .lastAnalyzedAt(LocalDateTime.now())
                .build();
    }

    // ==========================================
    // PRIVATE HELPERS
    // ==========================================

    /**
     * Returns true only if NO real API key is configured (falls back to rules engine).
     */
    private boolean isMockApiKey() {
        return "mock-key".equalsIgnoreCase(groqApiKey) || groqApiKey == null || groqApiKey.trim().isEmpty();
    }

    /**
     * Query the configured LLM. Uses Groq.
     */
    private String queryGroq(String systemPrompt, String userContent, boolean jsonFormat) throws Exception {
        String url = groqBaseUrl + "/chat/completions";
        
        log.info("Using AI provider: Groq | model: {}", groqModel);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + groqApiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", groqModel);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userContent));
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.3);
        requestBody.put("max_tokens", 1024);

        if (jsonFormat) {
            requestBody.put("response_format", Map.of("type", "json_object"));
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        JsonNode responseJson = objectMapper.readTree(response.getBody());
        String content = responseJson.path("choices").get(0).path("message").path("content").asText();
        log.debug("AI response received ({} chars)", content.length());
        return content;
    }

    /**
     * Deterministic rules-engine expense parser fallback
     */
    private ExpenseParseResult fallbackParseExpense(String text, String userTimeContext) {
        log.info("Running deterministic fallback parser.");
        BigDecimal amount = BigDecimal.ZERO;
        String merchant = "Unknown Merchant";
        String category = "Other";
        String subcategory = "Miscellaneous";
        String mealType = "None";
        String expenseType = "Non-Essential";

        // Regex for Amount: e.g. 250, ₹350, 4200 rupees
        Pattern amtPattern = Pattern.compile("(?:₹|rs\\.?|rupees?\\s*)?(\\d+(?:\\.\\d{2})?)");
        Matcher amtMatcher = amtPattern.matcher(text.toLowerCase());
        if (amtMatcher.find()) {
            amount = new BigDecimal(amtMatcher.group(1));
        }

        String lowerText = text.toLowerCase();
        
        // Categorization heuristic
        if (lowerText.contains("pizza") || lowerText.contains("food") || lowerText.contains("swiggy") 
                || lowerText.contains("zomato") || lowerText.contains("restaurant") || lowerText.contains("starbucks")
                || lowerText.contains("coffee") || lowerText.contains("maggi") || lowerText.contains("lunch") || lowerText.contains("dinner")) {
            category = "Food";
            expenseType = "Non-Essential";
            if (lowerText.contains("starbucks") || lowerText.contains("coffee")) {
                subcategory = "Cafe";
            } else if (lowerText.contains("groceries") || lowerText.contains("milk")) {
                category = "Food";
                subcategory = "Groceries";
                expenseType = "Essential";
            } else {
                subcategory = "Restaurant";
            }

            // Timed meal classifications
            int hour = LocalTime.now().getHour();
            // Try to extract time from userTimeContext
            if (userTimeContext != null && userTimeContext.contains("time:")) {
                try {
                    String timeStr = userTimeContext.substring(userTimeContext.indexOf("time:") + 5).trim();
                    hour = Integer.parseInt(timeStr.split(":")[0]);
                } catch (Exception ignore) {}
            }

            if (hour >= 5 && hour < 10) mealType = "Breakfast";
            else if (hour >= 10 && hour < 12) mealType = "Morning Snack";
            else if (hour >= 12 && hour < 15) mealType = "Lunch";
            else if (hour >= 15 && hour < 18) mealType = "Evening Snack";
            else if (hour >= 18 && hour < 22) mealType = "Dinner";
            else mealType = "Late Night";

        } else if (lowerText.contains("netflix") || lowerText.contains("spotify") || lowerText.contains("prime") || lowerText.contains("youtube")) {
            category = "Subscriptions";
            subcategory = "Entertainment Streaming";
            expenseType = "Non-Essential";
        } else if (lowerText.contains("cab") || lowerText.contains("uber") || lowerText.contains("ola") || lowerText.contains("petrol") || lowerText.contains("metro") || lowerText.contains("auto")) {
            category = "Travel";
            subcategory = "Local Transport";
            expenseType = "Essential";
        } else if (lowerText.contains("amazon") || lowerText.contains("myntra") || lowerText.contains("flipkart") || lowerText.contains("clothes")) {
            category = "Shopping";
            subcategory = "Apparel / E-commerce";
            expenseType = "Non-Essential";
        } else if (lowerText.contains("rent") || lowerText.contains("electricity") || lowerText.contains("wifi") || lowerText.contains("water")) {
            category = "Utilities";
            subcategory = "Bills";
            expenseType = "Essential";
        }

        // Merchant extraction
        if (lowerText.contains("swiggy")) merchant = "Swiggy";
        else if (lowerText.contains("zomato")) merchant = "Zomato";
        else if (lowerText.contains("starbucks")) merchant = "Starbucks";
        else if (lowerText.contains("netflix")) merchant = "Netflix";
        else if (lowerText.contains("spotify")) merchant = "Spotify";
        else if (lowerText.contains("amazon")) merchant = "Amazon";
        else if (lowerText.contains("uber")) merchant = "Uber";
        else if (lowerText.contains("ola")) merchant = "Ola";
        else {
            // Pick first few words
            String[] words = text.split("\\s+");
            if (words.length > 1) {
                merchant = words[1];
            }
        }

        BigDecimal gst = amount.multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP); // 18% nominal GST extraction

        String summary = String.format("Identified a %s transaction at %s for amount %s. Timed as %s.", 
                expenseType.toLowerCase(), merchant, amount, mealType.equals("None") ? "general" : mealType);

        return ExpenseParseResult.builder()
                .merchant(merchant)
                .amount(amount)
                .gst(gst)
                .category(category)
                .subcategory(subcategory)
                .mealType(mealType)
                .expenseType(expenseType)
                .transactionDate(LocalDate.now())
                .transactionTime(LocalTime.now())
                .confidenceScore(new BigDecimal("0.85"))
                .aiContextSummary(summary)
                .build();
    }

    /**
     * Deterministic rule-based simulator fallback
     */
    private SimulationResponse calculateDeterministicSimulation(String itemName, BigDecimal cost, BigDecimal monthlySavings, List<FinancialGoal> goals) {
        log.info("Calculating deterministic rule-based simulation.");

        // Calculate Scenario A: Buy Today
        BigDecimal totalCost = cost;
        int delayMonths = cost.divide(monthlySavings.compareTo(BigDecimal.ZERO) > 0 ? monthlySavings : BigDecimal.ONE, 0, RoundingMode.UP).intValue();
        
        // Scenario B: Postpone 3 Months
        BigDecimal monthlyCostB = cost.divide(new BigDecimal("3"), 2, RoundingMode.HALF_UP);
        
        // Scenario C: Used / Alternative (25% off)
        BigDecimal costC = cost.multiply(new BigDecimal("0.75")).setScale(2, RoundingMode.HALF_UP);
        int delayMonthsC = costC.divide(monthlySavings.compareTo(BigDecimal.ZERO) > 0 ? monthlySavings : BigDecimal.ONE, 0, RoundingMode.UP).intValue();

        // Scenario D: 12-Month EMI (10% flat interest rate for calculation simplicity)
        BigDecimal totalEmiCost = cost.multiply(new BigDecimal("1.10")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal emiMonthly = totalEmiCost.divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
        BigDecimal emiSavingsReductionPercent = emiMonthly.divide(monthlySavings.compareTo(BigDecimal.ZERO) > 0 ? monthlySavings : BigDecimal.ONE, 2, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));

        String recText = String.format("Based on your monthly savings rate of %s, I recommend Scenario C (Alternative / Used purchase) " +
                "or Scenario B (Postponing by 3 months). Buying immediately (Scenario A) will deplete your reserves and delay your active financial goals by %d months. " +
                "Choosing the EMI option (Scenario D) will commit %s%% of your monthly savings capacity for the next full year.",
                monthlySavings, delayMonths, emiSavingsReductionPercent.setScale(1, RoundingMode.HALF_UP));

        return SimulationResponse.builder()
                .recommendation(recText)
                .scenarioA(SimulationResponse.ScenarioDetails.builder()
                        .name("Buy Today")
                        .description("Draw down from current reserves.")
                        .monthlyCost(BigDecimal.ZERO)
                        .goalDelayMonths(delayMonths)
                        .totalCost(totalCost)
                        .impactAnalysis(String.format("Depletes reserves immediately. Delays target goals by %d months.", delayMonths))
                        .build())
                .scenarioB(SimulationResponse.ScenarioDetails.builder()
                        .name("Buy in 3 Months")
                        .description("Postpone and save specifically for this purchase.")
                        .monthlyCost(monthlyCostB)
                        .goalDelayMonths(0)
                        .totalCost(totalCost)
                        .impactAnalysis("No impact on current goals. Fully funded from future savings.")
                        .build())
                .scenarioC(SimulationResponse.ScenarioDetails.builder()
                        .name("Used/Alternative")
                        .description("Find the item refurbished or choose a competitor model (25% savings).")
                        .monthlyCost(BigDecimal.ZERO)
                        .goalDelayMonths(delayMonthsC)
                        .totalCost(costC)
                        .impactAnalysis(String.format("Saves %s. Goal delay is reduced to %d months.", cost.subtract(costC), delayMonthsC))
                        .build())
                .scenarioD(SimulationResponse.ScenarioDetails.builder()
                        .name("12-Month EMI")
                        .description("Purchase today using a 12-month installment plan at 10% annual rate.")
                        .monthlyCost(emiMonthly)
                        .goalDelayMonths(delayMonths / 2)
                        .totalCost(totalEmiCost)
                        .impactAnalysis(String.format("Commits %s monthly to installments, reducing savings velocity.", emiMonthly))
                        .build())
                .build();
    }

    /**
     * Deterministic chatbot advisor fallback
     */
    private String generateMockCoachReply(String message, List<Expense> history, List<FinancialGoal> goals, SpendingProfile profile, BigDecimal monthlyIncome) {
        String msg = message.toLowerCase();

        BigDecimal totalFood = BigDecimal.ZERO;
        BigDecimal totalShopping = BigDecimal.ZERO;
        BigDecimal totalSpent = BigDecimal.ZERO;

        for (Expense e : history) {
            totalSpent = totalSpent.add(e.getAmount());
            String cat = e.getCategory() == null ? "" : e.getCategory().toLowerCase();
            if (cat.contains("food") || cat.contains("dining")) {
                totalFood = totalFood.add(e.getAmount());
            }
            if (cat.contains("shopping")) {
                totalShopping = totalShopping.add(e.getAmount());
            }
        }

        if (msg.contains("salary") || msg.contains("go") || msg.contains("spend")) {
            return String.format("### FinPilot AI Assistant\n" +
                    "Here is a summary of your recent outflow context:\n" +
                    "- **Total Outflow:** ₹%s\n" +
                    "- **Food & Restaurants:** ₹%s (%s%% of total)\n" +
                    "- **Shopping:** ₹%s (%s%% of total)\n\n" +
                    "**Analysis & Coaching Advice:**\n" +
                    "Your food expenses (including dining and delivery apps) account for a significant portion of your budget. " +
                    "If you cook at home 3 times a week instead of ordering out, you could save approximately **₹3,500/month**, which would fund your goals **%s**.",
                    totalSpent, totalFood, 
                    totalSpent.compareTo(BigDecimal.ZERO) > 0 ? totalFood.multiply(new BigDecimal("100")).divide(totalSpent, 0, RoundingMode.HALF_UP) : 0,
                    totalShopping,
                    totalSpent.compareTo(BigDecimal.ZERO) > 0 ? totalShopping.multiply(new BigDecimal("100")).divide(totalSpent, 0, RoundingMode.HALF_UP) : 0,
                    goals.isEmpty() ? "sooner" : goals.get(0).getName() + " faster");
        }

        if (msg.contains("coffee") || msg.contains("what if")) {
            return "### What-If Scenario: Stopping Daily Starbucks/Coffee\n" +
                    "If you spend **₹150 daily** on takeaway coffees and decide to prepare it at home:\n" +
                    "- **1 Month Savings:** ₹4,500\n" +
                    "- **6 Months Savings:** ₹27,000\n" +
                    "- **1 Year Savings:** ₹54,000\n" +
                    "- **5 Years Savings (invested at 8% p.a.):** ₹324,500\n\n" +
                    "**Coaching recommendation:** You don't have to quit coffee completely, but making it at home on weekdays could easily finance your savings goals.";
        }

        if (msg.contains("iphone") || msg.contains("laptop") || msg.contains("afford")) {
            return "### FinPilot Purchase Advice\n" +
                    "Looking at your income and active savings target:\n" +
                    "Yes, you can afford it in **3 months** if you stick to your current budget limit. " +
                    "However, buying it immediately on credit will reduce your financial health score by **8 points** and delay your emergency fund target.";
        }

        return "Hello! I am **FinPilot**, your intelligent financial coach. You can ask me questions like:\n" +
                "- *Where did my salary go?*\n" +
                "- *What if I cut down on coffee?*\n" +
                "- *Can I buy a laptop next month?*\n" +
                "I base my coaching on your real-time expenses, goals, and monthly budgets.";
    }

    @Getter
    @Setter
    @Builder
    public static class UserSpendingStats {
        private BigDecimal totalSpent;
        private BigDecimal budgetUtilization;
    }

    /**
     * Generate a friendly AI context summary for an expense
     */
    public String generateExpenseContext(String merchant, BigDecimal amount, String category) {
        try {
            if (isMockApiKey()) {
                return String.format("Logged a %s expense of ₹%s at %s. %s",
                        category, amount, merchant,
                        "Non-Essential".equalsIgnoreCase(category) ? "Consider if this aligns with your financial goals." : "Marked as an essential spend.");
            }
            String prompt = String.format("Generate a one-sentence friendly financial insight for: %s purchase of ₹%s in %s category.", merchant, amount, category);
            return queryGroq("You are a concise financial advisor.", prompt, false);
        } catch (Exception e) {
            return String.format("Expense logged: ₹%s at %s (%s).", amount, merchant, category);
        }
    }

    public LifestyleAnalysis generateLifestyleAnalysis(List<Expense> expenses, BigDecimal monthlyIncome, User user) {
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        BigDecimal totalSpent = BigDecimal.ZERO;
        BigDecimal weekendSpent = BigDecimal.ZERO;
        BigDecimal weekdaySpent = BigDecimal.ZERO;
        BigDecimal largestSingleExpense = BigDecimal.ZERO;
        
        Map<String, BigDecimal> categorySums = new HashMap<>();
        Map<String, Integer> categoryCounts = new HashMap<>();
        
        int smallRecurringCount = 0;
        BigDecimal smallRecurringTotal = BigDecimal.ZERO;

        for (Expense e : expenses) {
            if (e.getTransactionDate() != null && !e.getTransactionDate().isBefore(thirtyDaysAgo)) {
                BigDecimal amt = e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO;
                totalSpent = totalSpent.add(amt);

                // Weekday / Weekend (SATURDAY/SUNDAY are weekends)
                java.time.DayOfWeek dow = e.getTransactionDate().getDayOfWeek();
                if (dow == java.time.DayOfWeek.SATURDAY || dow == java.time.DayOfWeek.SUNDAY) {
                    weekendSpent = weekendSpent.add(amt);
                } else {
                    weekdaySpent = weekdaySpent.add(amt);
                }

                // Largest single expense
                if (amt.compareTo(largestSingleExpense) > 0) {
                    largestSingleExpense = amt;
                }

                // Small recurring expenses (amount < 1000 and isRecurring is true)
                if (Boolean.TRUE.equals(e.getIsRecurring()) && amt.compareTo(new BigDecimal("1000")) < 0) {
                    smallRecurringCount++;
                    smallRecurringTotal = smallRecurringTotal.add(amt);
                }

                // Category grouping
                String category = e.getCategory() != null ? e.getCategory() : "Others";
                categorySums.put(category, categorySums.getOrDefault(category, BigDecimal.ZERO).add(amt));
                categoryCounts.put(category, categoryCounts.getOrDefault(category, 0) + 1);
            }
        }

        BigDecimal avgDailySpending = totalSpent.divide(new BigDecimal("30"), 2, RoundingMode.HALF_UP);

        // Highest and lowest spending categories
        String highestCategory = "None";
        BigDecimal maxCatSpent = BigDecimal.ZERO;
        String lowestCategory = "None";
        BigDecimal minCatSpent = null;

        for (Map.Entry<String, BigDecimal> entry : categorySums.entrySet()) {
            BigDecimal amt = entry.getValue();
            if (amt.compareTo(maxCatSpent) > 0) {
                maxCatSpent = amt;
                highestCategory = entry.getKey();
            }
            if (minCatSpent == null || amt.compareTo(minCatSpent) < 0) {
                minCatSpent = amt;
                lowestCategory = entry.getKey();
            }
        }
        if (lowestCategory.equals("None") && !categorySums.isEmpty()) {
            lowestCategory = categorySums.keySet().iterator().next();
        }

        // Most frequent category
        String mostFrequentCategory = "None";
        int maxCount = 0;
        for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mostFrequentCategory = entry.getKey();
            }
        }

        BigDecimal income = (monthlyIncome != null && monthlyIncome.compareTo(BigDecimal.ZERO) > 0) ? monthlyIncome : new BigDecimal("50000");
        
        // Calculate wellness score
        int wellnessScore = 100;
        if (income.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal spentRatio = totalSpent.divide(income, 2, RoundingMode.HALF_UP);
            if (spentRatio.compareTo(new BigDecimal("1.0")) > 0) {
                wellnessScore = Math.max(30, 100 - spentRatio.subtract(BigDecimal.ONE).multiply(new BigDecimal("100")).intValue());
            } else {
                wellnessScore = 100 - spentRatio.multiply(new BigDecimal("50")).intValue();
            }
        }
        wellnessScore = Math.min(100, Math.max(0, wellnessScore));

        if (isMockApiKey()) {
            return generateLifestyleAnalysisFallback(totalSpent, avgDailySpending, highestCategory, maxCatSpent, 
                    lowestCategory, minCatSpent, mostFrequentCategory, maxCount, weekendSpent, weekdaySpent, 
                    largestSingleExpense, smallRecurringCount, smallRecurringTotal, income, wellnessScore, user);
        }

        try {
            String systemPrompt = "You are an AI Financial Coach.\n" +
                    "Analyze the user's spending over the last 30 days and generate personalized financial observations.\n" +
                    "Respond with strict JSON matching the following schema:\n" +
                    "{\n" +
                    "  \"personalityType\": \"Spending Personality (e.g. Balanced Saver, Weekend Splurger, Prudent Planner, Impulse Buyer)\",\n" +
                    "  \"biggestSpendingHabit\": \"Description of the biggest spending habit/category based on stats\",\n" +
                    "  \"positiveHabit\": \"One positive financial habit\",\n" +
                    "  \"improvementSuggestion\": \"One area that needs improvement\",\n" +
                    "  \"savingsOpportunity\": \"One savings opportunity\",\n" +
                    "  \"wellnessScore\": 85,\n" +
                    "  \"aiSummary\": \"A concise summary of their overall spending archetype and health over the last 30 days\"\n" +
                    "}\n" +
                    "Keep responses concise. The total length of the JSON string values must be under 150 words.";

            String userContent = String.format(
                    "Income: ₹%s\n" +
                    "Total Spending: ₹%s\n" +
                    "Average Daily Spending: ₹%s\n" +
                    "Highest Spending Category: %s (₹%s)\n" +
                    "Lowest Spending Category: %s (₹%s)\n" +
                    "Most Frequent Category: %s (%d transactions)\n" +
                    "Weekend Spending: ₹%s\n" +
                    "Weekday Spending: ₹%s\n" +
                    "Largest Single Expense: ₹%s\n" +
                    "Small Recurring Expenses: %d transactions (Total ₹%s)\n" +
                    "Outflow breakdown by category:\n%s",
                    income, totalSpent, avgDailySpending, highestCategory, maxCatSpent, lowestCategory, 
                    minCatSpent != null ? minCatSpent : BigDecimal.ZERO, mostFrequentCategory, maxCount, 
                    weekendSpent, weekdaySpent, largestSingleExpense, smallRecurringCount, smallRecurringTotal, categorySums
            );

            String jsonResponse = queryGroq(systemPrompt, userContent, true);
            JsonNode root = objectMapper.readTree(jsonResponse);

            return LifestyleAnalysis.builder()
                    .user(user)
                    .personalityType(root.path("personalityType").asText("Balanced Saver"))
                    .biggestSpendingHabit(root.path("biggestSpendingHabit").asText(highestCategory))
                    .positiveHabit(root.path("positiveHabit").asText("You consistently control expenses in minor categories."))
                    .improvementSuggestion(root.path("improvementSuggestion").asText("Watch out for weekend discretionary spending."))
                    .savingsOpportunity(root.path("savingsOpportunity").asText("Consider cutting back on restaurant deliveries."))
                    .wellnessScore(root.has("wellnessScore") ? root.get("wellnessScore").asInt() : wellnessScore)
                    .aiSummary(root.path("aiSummary").asText("No summary generated."))
                    .isDirty(false)
                    .build();

        } catch (Exception e) {
            log.error("AI Lifestyle Analysis generation failed, falling back to deterministic", e);
            return generateLifestyleAnalysisFallback(totalSpent, avgDailySpending, highestCategory, maxCatSpent, 
                    lowestCategory, minCatSpent, mostFrequentCategory, maxCount, weekendSpent, weekdaySpent, 
                    largestSingleExpense, smallRecurringCount, smallRecurringTotal, income, wellnessScore, user);
        }
    }

    private LifestyleAnalysis generateLifestyleAnalysisFallback(BigDecimal totalSpent, BigDecimal avgDailySpending,
                                                                 String highestCategory, BigDecimal maxCatSpent,
                                                                 String lowestCategory, BigDecimal minCatSpent,
                                                                 String mostFrequentCategory, int maxCount,
                                                                 BigDecimal weekendSpent, BigDecimal weekdaySpent,
                                                                 BigDecimal largestSingleExpense, int smallRecurringCount,
                                                                 BigDecimal smallRecurringTotal, BigDecimal income, int wellnessScore, User user) {
        String personality = weekendSpent.compareTo(weekdaySpent) > 0 ? "Weekend Splurger" : "Balanced Saver";
        String positive = String.format("You consistently control transportation and other expenses, keeping %s lowest.", lowestCategory);
        String improvement = String.format("Spending in %s is your highest outflow at ₹%s.", highestCategory, maxCatSpent.setScale(0, RoundingMode.HALF_UP));
        String opportunity = String.format("Reducing spending in %s by 15%% could save approximately ₹%s every month.", 
                highestCategory, maxCatSpent.multiply(new BigDecimal("0.15")).setScale(0, RoundingMode.HALF_UP));
        String summary = String.format("Over the last 30 days, your average daily spending was ₹%s. Your largest single expense was ₹%s, and you spent ₹%s on weekdays compared to ₹%s on weekends.",
                avgDailySpending.setScale(0, RoundingMode.HALF_UP), largestSingleExpense.setScale(0, RoundingMode.HALF_UP),
                weekdaySpent.setScale(0, RoundingMode.HALF_UP), weekendSpent.setScale(0, RoundingMode.HALF_UP));

        return LifestyleAnalysis.builder()
                .user(user)
                .personalityType(personality)
                .biggestSpendingHabit(highestCategory + " (₹" + maxCatSpent.setScale(0, RoundingMode.HALF_UP) + ")")
                .positiveHabit(positive)
                .improvementSuggestion(improvement)
                .savingsOpportunity(opportunity)
                .wellnessScore(wellnessScore)
                .aiSummary(summary)
                .isDirty(false)
                .build();
    }
}
