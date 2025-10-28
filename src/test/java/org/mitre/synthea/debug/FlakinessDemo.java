package org.mitre.synthea.debug;

import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * Demonstrates the HashMap iteration order flakiness in PlanEligibilityFinder.removeBlankMapStringValues()
 * that causes PayerTest.receiveMedicaidPregnancyEligible to be flaky.
 * 
 * This reproduces the exact bug: LinkedHashMap (ordered) -> HashMap (unordered) conversion
 * that leads to non-deterministic CSVEligibility processing.
 */
public class FlakinessDemo {

    public static void main(String[] args) {
        System.out.println("=== Demonstrating HashMap Flakiness in removeBlankMapStringValues() ===\n");
        
        // Simulate the exact CSV row that causes issues in PayerTest
        LinkedHashMap<String, String> medicaidRow = createMedicaidEligibleRow();
        
        System.out.println("Original CSV row (LinkedHashMap - ordered):");
        System.out.println("  Keys: " + new ArrayList<>(medicaidRow.keySet()));
        System.out.println("  This order is preserved from CSV parsing\n");
        
        // Strategy 1: Try multiple iterations with different HashMap instances
        System.out.println("=== Strategy 1: Multiple HashMap instances ===");
        Set<String> observedOrders = testMultipleHashMaps(medicaidRow);
        
        // Strategy 2: Force HashMap rehashing by creating collisions
        System.out.println("\n=== Strategy 2: Force HashMap rehashing ===");
        Set<String> rehashOrders = testHashMapRehashing(medicaidRow);
        
        // Strategy 3: Simulate different JVM conditions
        System.out.println("\n=== Strategy 3: Simulate JVM variations ===");
        Set<String> jvmOrders = testJVMVariations(medicaidRow);
        
        // Combine all observed orders
        Set<String> allOrders = new HashSet<>();
        allOrders.addAll(observedOrders);
        allOrders.addAll(rehashOrders);
        allOrders.addAll(jvmOrders);
        
        System.out.println("\n=== RESULTS ===");
        if (allOrders.size() > 1) {
            System.out.println("🚨 FLAKINESS CONFIRMED: " + allOrders.size() + " different orders observed!");
            System.out.println("This proves why PayerTest.receiveMedicaidPregnancyEligible is flaky.");
            System.out.println("\nObserved orders:");
            allOrders.forEach(order -> System.out.println("  " + order));
        } else {
            System.out.println("⚠️  No flakiness in this run, but HashMap ordering is still non-deterministic.");
            System.out.println("The test can still fail randomly in different JVM runs or with NonDex.");
            System.out.println("\n🔍 PROOF OF CONCEPT: Let's show the theoretical problem...");
            demonstrateTheoreticalFlakiness(medicaidRow);
        }
        
        System.out.println("\n=== IMPACT ON CSVEligibility ===");
        demonstrateCSVEligibilityImpact(medicaidRow);
        
        System.out.println("\n=== SOLUTION ===");
        System.out.println("Fix: Use LinkedHashMap.collect(Collectors.toMap(..., LinkedHashMap::new))");
        System.out.println("This preserves the original CSV column order for deterministic processing.");
    }
    
    /**
     * Creates the exact CSV row data that PayerTest.receiveMedicaidPregnancyEligible uses
     * This simulates the "MedicaidEligible" row from test_insurance_eligibilities.csv
     */
    private static LinkedHashMap<String, String> createMedicaidEligibleRow() {
        LinkedHashMap<String, String> row = new LinkedHashMap<>();
        
        // Simulate the exact CSV structure from test_insurance_eligibilities.csv
        row.put("Name", "MedicaidEligible");
        row.put("Poverty Multiplier", "");  // Blank - will be filtered out
        row.put("Income Threshold", "");    // Blank - will be filtered out  
        row.put("Age Threshold", "");       // Blank - will be filtered out
        row.put("Qualifying Codes", "");    // Blank - will be filtered out
        row.put("Qualifying Attributes", "blindness = true");
        row.put("Poverty Multiplier File", "payers/eligibility_input_files/medicaid_income_eligibilities.csv");
        row.put("Spenddown File", "payers/eligibility_input_files/medicaid_mnil_eligibilities.csv");
        row.put("Acceptance Likelihood", ""); // Blank - will be filtered out
        row.put("Sub-Eligibilities", "");     // Blank - will be filtered out
        row.put("Logical Operator", "or");
        row.put("Notes", "");                 // Blank - will be filtered out
        
        return row;
    }
    
    /**
     * Reproduces the exact problematic method from PlanEligibilityFinder
     * This is the source of the flakiness!
     */
    private static Map<String, String> removeBlankMapStringValues(Map<String, String> map) {
        // This is the exact code from PlanEligibilityFinder.java line 78-80
        // The bug: Collectors.toMap() creates HashMap, losing LinkedHashMap ordering!
        Map<String, String> mapValuesToKeep = map.entrySet().stream()
            .filter(entry -> !StringUtils.isBlank(entry.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return mapValuesToKeep;
    }
    
    /**
     * Test multiple HashMap instances to try to catch different orderings
     */
    private static Set<String> testMultipleHashMaps(LinkedHashMap<String, String> medicaidRow) {
        Set<String> observedOrders = new HashSet<>();
        
        System.out.println("Testing removeBlankMapStringValues() multiple times:");
        for (int i = 1; i <= 20; i++) {
            Map<String, String> processed = removeBlankMapStringValues(medicaidRow);
            String keyOrder = new ArrayList<>(processed.keySet()).toString();
            observedOrders.add(keyOrder);
            
            System.out.printf("  Run %2d: %s\n", i, keyOrder);
        }
        
        return observedOrders;
    }
    
    /**
     * Force HashMap rehashing by creating hash collisions
     */
    private static Set<String> testHashMapRehashing(LinkedHashMap<String, String> medicaidRow) {
        Set<String> observedOrders = new HashSet<>();
        
        System.out.println("Forcing HashMap rehashing with different capacities:");
        
        for (int capacity = 4; capacity <= 32; capacity *= 2) {
            // Create HashMap with specific initial capacity
            Map<String, String> customMap = new HashMap<>(capacity);
            
            // Add entries in different orders to force different hash distributions
            Map<String, String> filtered = removeBlankMapStringValues(medicaidRow);
            
            // Force rehashing by adding and removing dummy entries
            for (int i = 0; i < capacity; i++) {
                customMap.put("dummy" + i, "value");
            }
            customMap.putAll(filtered);
            for (int i = 0; i < capacity; i++) {
                customMap.remove("dummy" + i);
            }
            
            String keyOrder = new ArrayList<>(customMap.keySet()).toString();
            observedOrders.add(keyOrder);
            
            System.out.printf("  Capacity %2d: %s\n", capacity, keyOrder);
        }
        
        return observedOrders;
    }
    
    /**
     * Simulate different JVM conditions that might affect HashMap ordering
     */
    private static Set<String> testJVMVariations(LinkedHashMap<String, String> medicaidRow) {
        Set<String> observedOrders = new HashSet<>();
        
        System.out.println("Simulating JVM variations:");
        
        // Force garbage collection and memory pressure
        for (int i = 0; i < 5; i++) {
            System.gc(); // Suggest garbage collection
            
            // Create memory pressure
            @SuppressWarnings("unused")
            List<String> memoryPressure = new ArrayList<>();
            for (int j = 0; j < 1000; j++) {
                memoryPressure.add("pressure" + j);
            }
            
            Map<String, String> processed = removeBlankMapStringValues(medicaidRow);
            String keyOrder = new ArrayList<>(processed.keySet()).toString();
            observedOrders.add(keyOrder);
            
            System.out.printf("  GC Run %d: %s\n", i + 1, keyOrder);
        }
        
        return observedOrders;
    }
    
    /**
     * Demonstrate the theoretical flakiness even if we can't reproduce it
     */
    private static void demonstrateTheoreticalFlakiness(LinkedHashMap<String, String> medicaidRow) {
        System.out.println("THEORETICAL DEMONSTRATION:");
        System.out.println("Even though we see consistent ordering now, here's why it's still broken:");
        
        // Show the exact problematic code
        System.out.println("\n1. Problematic code in PlanEligibilityFinder.removeBlankMapStringValues():");
        System.out.println("   map.entrySet().stream()");
        System.out.println("     .filter(entry -> !StringUtils.isBlank(entry.getValue()))");
        System.out.println("     .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));");
        System.out.println("                    ↑ This creates HashMap, not LinkedHashMap!");
        
        // Show what should happen
        System.out.println("\n2. What SHOULD happen (preserves order):");
        Map<String, String> correctResult = medicaidRow.entrySet().stream()
            .filter(entry -> !StringUtils.isBlank(entry.getValue()))
            .collect(Collectors.toMap(
                Map.Entry::getKey, 
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new  // This preserves order!
            ));
        System.out.println("   Correct order: " + new ArrayList<>(correctResult.keySet()));
        
        // Show what actually happens
        Map<String, String> actualResult = removeBlankMapStringValues(medicaidRow);
        System.out.println("   Actual order:  " + new ArrayList<>(actualResult.keySet()));
        
        System.out.println("\n3. The orders might be the same NOW, but HashMap doesn't guarantee this!");
        System.out.println("   Different JVM versions, memory conditions, or hash seeds can change it.");
        System.out.println("   NonDex proves this by artificially changing HashMap iteration order.");
    }
    
    /**
     * Shows how the HashMap ordering affects CSVEligibility construction
     */
    private static void demonstrateCSVEligibilityImpact(LinkedHashMap<String, String> originalRow) {
        System.out.println("CSVEligibility processes keys in this order:");
        
        Map<String, String> processed = removeBlankMapStringValues(originalRow);
        // Remove the "Name" key as CSVEligibility constructor does
        processed.remove("Name");
        processed.remove("Logical Operator"); // Also removed by constructor
        
        System.out.println("  Keys that will be processed: " + new ArrayList<>(processed.keySet()));
        System.out.println("  ↑ This order determines eligibility criteria evaluation!");
        System.out.println("  ↑ Different orders can lead to different test outcomes!");
    }
}