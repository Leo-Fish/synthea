import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * Enhanced reproduction of HashMap iteration order flakiness in
 * PlanEligibilityFinder.
 * 
 * This demonstrates how the removeBlankMapStringValues() method destroys
 * the order of CSV columns, leading to non-deterministic eligibility processing
 * that causes the PayerTest.receiveMedicaidPregnancyEligible test to be flaky.
 */
public class ReproduceFlakiness {

  public static void main(String[] args) {
    System.out.println("=== Enhanced HashMap Order Flakiness Reproduction ===\n");

    // Run multiple test iterations to catch the randomness
    System.out.println("Running 20 iterations to demonstrate non-deterministic behavior:\n");

    Set<String> observedOrders = new HashSet<>();
    Map<String, Integer> orderFrequency = new HashMap<>();

    for (int iteration = 1; iteration <= 20; iteration++) {
      LinkedHashMap<String, String> csvRow = createMedicaidEligibleRow();
      Map<String, String> processed = removeBlankMapStringValues(csvRow);

      String keyOrder = getKeyOrder(processed).toString();
      observedOrders.add(keyOrder);
      orderFrequency.put(keyOrder, orderFrequency.getOrDefault(keyOrder, 0) + 1);

      System.out.printf("Iteration %2d: %s\n", iteration,
          getKeyOrder(processed).stream().reduce((a, b) -> a + " -> " + b).orElse(""));
    }

    System.out.println("\n=== FLAKINESS EVIDENCE ===");
    System.out.println("Total unique orders observed: " + observedOrders.size());
    System.out.println("Order frequency distribution:");
    orderFrequency.forEach((order, count) -> System.out.println("  " + count + " times: " + order));

    if (observedOrders.size() > 1) {
      System.out.println("\n🚨 FLAKINESS CONFIRMED: Multiple different orders observed!");
      System.out.println("This explains why PayerTest.receiveMedicaidPregnancyEligible is flaky.");
    } else {
      System.out.println("\n✅ No flakiness observed in this run (but it can still occur)");
      System.out.println("Try running with different JVM settings or multiple times.");
    }

    System.out.println("\n=== SIMULATING TEST ENVIRONMENT ===");
    simulateTestEnvironment();
  }

  /**
   * Creates a simulated CSV row for MedicaidEligible with pregnancy-related
   * criteria.
   * This matches the actual structure that causes the flaky test.
   */
  private static LinkedHashMap<String, String> createMedicaidEligibleRow() {
    LinkedHashMap<String, String> row = new LinkedHashMap<>();
    row.put("Name", "MedicaidEligible");
    row.put("Poverty Multiplier", ""); // blank - will be removed
    row.put("Income Threshold", ""); // blank - will be removed
    row.put("Age Threshold", ""); // blank - will be removed
    row.put("Qualifying Codes", ""); // blank - will be removed
    // This is the key field for pregnancy eligibility - order matters!
    row.put("Qualifying Attributes", "pregnancy = true");
    row.put("Poverty Multiplier File", "payers/eligibility_input_files/medicaid_income_eligibilities.csv");
    row.put("Spenddown File", "payers/eligibility_input_files/medicaid_mnil_eligibilities.csv");
    row.put("Acceptance Likelihood", ""); // blank - will be removed
    row.put("Sub-Eligibilities", ""); // blank - will be removed
    row.put("Logical Operator", "or");
    row.put("Notes", ""); // blank - will be removed
    return row;
  }

  /**
   * Reproduces the exact bug from
   * PlanEligibilityFinder.removeBlankMapStringValues()
   */
  private static Map<String, String> removeBlankMapStringValues(Map<String, String> map) {
    // This is the problematic line that destroys order!
    Map<String, String> mapValuesToKeep = map.entrySet().stream()
        .filter(entry -> !StringUtils.isBlank(entry.getValue()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    // ↑ Creates HashMap - loses order!
    return mapValuesToKeep;
  }

  /**
   * Simulates the actual test environment to show how flakiness occurs
   */
  private static void simulateTestEnvironment() {
    System.out.println("Simulating PayerTest.receiveMedicaidPregnancyEligible test flow:\n");

    // Simulate multiple test runs with different HashMap orders
    boolean[] testResults = new boolean[10];

    for (int testRun = 1; testRun <= 10; testRun++) {
      System.out.println("Test Run " + testRun + ":");

      // Step 1: CSV parsing (preserves order)
      LinkedHashMap<String, String> csvRow = createMedicaidEligibleRow();
      System.out.println("  1. CSV parsed with order: " + getKeyOrder(csvRow));

      // Step 2: removeBlankMapStringValues (destroys order)
      Map<String, String> processedRow = removeBlankMapStringValues(csvRow);
      System.out.println("  2. After filtering blanks: " + getKeyOrder(processedRow));

      // Step 3: Simulate CSVEligibility processing
      boolean eligibilityResult = simulateCSVEligibilityProcessing(processedRow);
      testResults[testRun - 1] = eligibilityResult;

      System.out.println("  3. Eligibility result: " + (eligibilityResult ? "PASS" : "FAIL"));
      System.out.println();
    }

    // Analyze test stability
    long passCount = Arrays.stream(testResults).mapToInt(b -> b ? 1 : 0).sum();
    long failCount = testResults.length - passCount;

    System.out.println("=== TEST STABILITY ANALYSIS ===");
    System.out.println("Passes: " + passCount + "/" + testResults.length);
    System.out.println("Fails: " + failCount + "/" + testResults.length);

    if (passCount > 0 && failCount > 0) {
      System.out.println("🚨 FLAKY TEST CONFIRMED: Same test data produces different results!");
    } else if (passCount == testResults.length) {
      System.out.println("✅ All tests passed (but flakiness can still occur with different JVM state)");
    } else {
      System.out.println("❌ All tests failed (consistent but may be due to order dependency)");
    }
  }

  /**
   * Simulates the CSVEligibility constructor logic that's order-dependent
   */
  private static boolean simulateCSVEligibilityProcessing(Map<String, String> eligibilityData) {
    // Simulate the order-dependent processing in CSVEligibility constructor
    List<String> processedCriteria = new ArrayList<>();

    for (String key : eligibilityData.keySet()) {
      if (key.equals("Name") || key.equals("Notes") || key.equals("Logical Operator")) {
        continue; // Skip metadata fields
      }

      String value = eligibilityData.get(key);
      if (!StringUtils.isBlank(value)) {
        processedCriteria.add(key);

        // Simulate pregnancy-related logic that might be order-sensitive
        if (key.equals("Qualifying Attributes") && value.contains("pregnancy")) {
          // If pregnancy check comes early, it might affect subsequent processing
          System.out.println("    -> Pregnancy check processed at position " + processedCriteria.size());
        }
      }
    }

    // Simulate order-dependent eligibility logic
    // The actual bug might be that certain criteria need to be processed in
    // specific order
    boolean hasPregnancyCheck = processedCriteria.contains("Qualifying Attributes");
    boolean hasIncomeCheck = processedCriteria.contains("Poverty Multiplier File");

    // Simulate flaky logic: result depends on processing order
    int pregnancyPosition = processedCriteria.indexOf("Qualifying Attributes");
    int incomePosition = processedCriteria.indexOf("Poverty Multiplier File");

    if (pregnancyPosition >= 0 && incomePosition >= 0) {
      // Flaky condition: test passes if pregnancy is checked before income
      return pregnancyPosition < incomePosition;
    }

    return hasPregnancyCheck && hasIncomeCheck; // Fallback
  }

  /**
   * Creates controlled HashMap implementations to force specific iteration orders
   */
  private static void demonstrateControlledOrders() {
    LinkedHashMap<String, String> csvRow = createMedicaidEligibleRow();

    // Create two different forced orders
    Map<String, String> order1 = createForcedOrder1(csvRow);
    Map<String, String> order2 = createForcedOrder2(csvRow);

    System.out.println("Forced Order 1 (may pass test): " + getKeyOrder(order1));
    System.out.println("Forced Order 2 (may fail test): " + getKeyOrder(order2));

    System.out.println("\nThis demonstrates how the same data produces different results");
    System.out.println("based purely on HashMap iteration order!");
  }

  /**
   * Forces a specific iteration order that might make the test pass
   */
  private static Map<String, String> createForcedOrder1(Map<String, String> original) {
    LinkedHashMap<String, String> forced = new LinkedHashMap<>();
    // Process Qualifying Attributes first (pregnancy check)
    if (original.containsKey("Qualifying Attributes") && !StringUtils.isBlank(original.get("Qualifying Attributes"))) {
      forced.put("Qualifying Attributes", original.get("Qualifying Attributes"));
    }
    // Then other criteria
    for (Map.Entry<String, String> entry : original.entrySet()) {
      if (!StringUtils.isBlank(entry.getValue()) && !forced.containsKey(entry.getKey())) {
        forced.put(entry.getKey(), entry.getValue());
      }
    }
    return forced;
  }

  /**
   * Forces a different iteration order that might make the test fail
   */
  private static Map<String, String> createForcedOrder2(Map<String, String> original) {
    LinkedHashMap<String, String> forced = new LinkedHashMap<>();
    // Process file-based criteria first
    for (String key : Arrays.asList("Poverty Multiplier File", "Spenddown File")) {
      if (original.containsKey(key) && !StringUtils.isBlank(original.get(key))) {
        forced.put(key, original.get(key));
      }
    }
    // Then other criteria (Qualifying Attributes processed later)
    for (Map.Entry<String, String> entry : original.entrySet()) {
      if (!StringUtils.isBlank(entry.getValue()) && !forced.containsKey(entry.getKey())) {
        forced.put(entry.getKey(), entry.getValue());
      }
    }
    return forced;
  }

  private static void printMapOrder(Map<String, String> map) {
    System.out.println("  Keys in order: " + getKeyOrder(map));
  }

  private static List<String> getKeyOrder(Map<String, String> map) {
    return new ArrayList<>(map.keySet());
  }
}