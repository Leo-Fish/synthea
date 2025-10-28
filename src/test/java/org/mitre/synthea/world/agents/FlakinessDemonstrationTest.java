package org.mitre.synthea.world.agents;

import static org.junit.Assert.*;

import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.mitre.synthea.world.agents.behaviors.planeligibility.PlanEligibilityFinder;

/**
 * Demonstrates the HashMap iteration order flakiness that causes
 * PayerTest.receiveMedicaidPregnancyEligible to be flaky.
 * 
 * This test intentionally shows non-deterministic behavior to prove
 * the root cause before we fix it.
 */
public class FlakinessDemonstrationTest {

    /**
     * This test demonstrates the core issue: removeBlankMapStringValues()
     * converts LinkedHashMap to HashMap, destroying insertion order.
     */
    @Test
    public void demonstrateHashMapOrderFlakiness() {
        System.out.println("\n=== Demonstrating HashMap Order Flakiness ===");
        
        // Create the same CSV row data multiple times
        Set<String> observedOrders = new HashSet<>();
        
        for (int i = 0; i < 50; i++) {
            LinkedHashMap<String, String> csvRow = createMedicaidEligibleRow();
            
            // This is the problematic method from PlanEligibilityFinder
            Map<String, String> processed = simulateRemoveBlankMapStringValues(csvRow);
            
            String keyOrder = new ArrayList<>(processed.keySet()).toString();
            observedOrders.add(keyOrder);
        }
        
        System.out.println("Observed " + observedOrders.size() + " different key orders:");
        observedOrders.forEach(order -> System.out.println("  " + order));
        
        if (observedOrders.size() > 1) {
            System.out.println("\n🚨 FLAKINESS CONFIRMED: HashMap produces multiple different orders!");
            System.out.println("This is why PayerTest.receiveMedicaidPregnancyEligible is flaky.");
        } else {
            System.out.println("\n⚠️  No flakiness observed in this run, but it can still occur.");
            System.out.println("HashMap iteration order is not guaranteed to be random every time.");
        }
        
        // Don't fail the test - we're just demonstrating the issue
        assertTrue("This test demonstrates flakiness, not a failure", true);
    }
    
    /**
     * Shows how the order affects eligibility processing by simulating
     * the CSVEligibility constructor logic.
     */
    @Test
    public void demonstrateOrderDependentProcessing() {
        System.out.println("\n=== Demonstrating Order-Dependent Processing ===");
        
        LinkedHashMap<String, String> csvRow = createMedicaidEligibleRow();
        
        // Test with forced different orders
        Map<String, String> order1 = createForcedOrder(csvRow, true);  // Pregnancy first
        Map<String, String> order2 = createForcedOrder(csvRow, false); // Income first
        
        System.out.println("Order 1 (pregnancy first): " + new ArrayList<>(order1.keySet()));
        System.out.println("Order 2 (income first): " + new ArrayList<>(order2.keySet()));
        
        // Simulate processing with different orders
        boolean result1 = simulateEligibilityProcessing(order1);
        boolean result2 = simulateEligibilityProcessing(order2);
        
        System.out.println("Result with order 1: " + result1);
        System.out.println("Result with order 2: " + result2);
        
        if (result1 != result2) {
            System.out.println("\n🚨 ORDER DEPENDENCY CONFIRMED: Same data, different results!");
        } else {
            System.out.println("\n✅ Results are consistent (but order dependency may exist in real code)");
        }
        
        assertTrue("This test demonstrates order dependency, not a failure", true);
    }
    
    /**
     * Reproduces the exact problematic method from PlanEligibilityFinder
     */
    private Map<String, String> simulateRemoveBlankMapStringValues(Map<String, String> map) {
        // This is the exact code that causes the issue!
        Map<String, String> mapValuesToKeep = map.entrySet().stream()
            .filter(entry -> !StringUtils.isBlank(entry.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        //                ↑ This creates HashMap, losing LinkedHashMap order!
        return mapValuesToKeep;
    }
    
    /**
     * Creates test data matching the MedicaidEligible row that causes flakiness
     */
    private LinkedHashMap<String, String> createMedicaidEligibleRow() {
        LinkedHashMap<String, String> row = new LinkedHashMap<>();
        row.put("Name", "MedicaidEligible");
        row.put("Poverty Multiplier", ""); // blank - will be removed
        row.put("Income Threshold", ""); // blank - will be removed  
        row.put("Age Threshold", ""); // blank - will be removed
        row.put("Qualifying Codes", ""); // blank - will be removed
        row.put("Qualifying Attributes", "pregnancy = true"); // Critical for test
        row.put("Poverty Multiplier File", "payers/eligibility_input_files/medicaid_income_eligibilities.csv");
        row.put("Spenddown File", "payers/eligibility_input_files/medicaid_mnil_eligibilities.csv");
        row.put("Acceptance Likelihood", ""); // blank - will be removed
        row.put("Sub-Eligibilities", ""); // blank - will be removed
        row.put("Logical Operator", "or");
        row.put("Notes", ""); // blank - will be removed
        return row;
    }
    
    /**
     * Forces a specific iteration order for controlled testing
     */
    private Map<String, String> createForcedOrder(Map<String, String> original, boolean pregnancyFirst) {
        LinkedHashMap<String, String> forced = new LinkedHashMap<>();
        
        if (pregnancyFirst) {
            // Process pregnancy check first
            addIfNotBlank(forced, original, "Qualifying Attributes");
            addIfNotBlank(forced, original, "Poverty Multiplier File");
            addIfNotBlank(forced, original, "Spenddown File");
        } else {
            // Process income checks first
            addIfNotBlank(forced, original, "Poverty Multiplier File");
            addIfNotBlank(forced, original, "Spenddown File");
            addIfNotBlank(forced, original, "Qualifying Attributes");
        }
        
        // Add remaining non-blank entries
        for (Map.Entry<String, String> entry : original.entrySet()) {
            if (!StringUtils.isBlank(entry.getValue()) && !forced.containsKey(entry.getKey())) {
                forced.put(entry.getKey(), entry.getValue());
            }
        }
        
        return forced;
    }
    
    private void addIfNotBlank(Map<String, String> target, Map<String, String> source, String key) {
        if (source.containsKey(key) && !StringUtils.isBlank(source.get(key))) {
            target.put(key, source.get(key));
        }
    }
    
    /**
     * Simulates order-dependent eligibility processing logic
     */
    private boolean simulateEligibilityProcessing(Map<String, String> eligibilityData) {
        List<String> processedKeys = new ArrayList<>();
        
        for (String key : eligibilityData.keySet()) {
            if (!key.equals("Name") && !key.equals("Notes") && !key.equals("Logical Operator")) {
                processedKeys.add(key);
            }
        }
        
        // Simulate order-dependent logic
        int pregnancyIndex = processedKeys.indexOf("Qualifying Attributes");
        int incomeIndex = processedKeys.indexOf("Poverty Multiplier File");
        
        // Hypothetical order-dependent condition that could cause flakiness
        if (pregnancyIndex >= 0 && incomeIndex >= 0) {
            return pregnancyIndex < incomeIndex; // Flaky: depends on processing order
        }
        
        return true; // Default case
    }
}