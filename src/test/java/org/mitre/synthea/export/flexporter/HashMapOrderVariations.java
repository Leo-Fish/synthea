package org.mitre.synthea.export.flexporter;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class HashMapOrderVariations {

  @Test
  public void demonstrateHashMapOrderFactors() {
    System.out.println("=== Factors Affecting HashMap Order ===");
    
    // 1. Different string content
    testDifferentStrings();
    
    // 2. Different HashMap sizes
    testDifferentSizes();
    
    // 3. Different JVM runs (hash seed)
    testMultipleInstances();
    
    // Add assertion to prevent caching
    assert true;
  }

  private void testDifferentStrings() {
    System.out.println("\n1. Different String Content:");
    
    // Test with different path patterns
    String[][] testCases = {
        {"Patient.name.given[0]", "Patient.name.given[1]"},
        {"Patient.name.give[0]", "Patient.name.give[1]"},
        {"Patient.name.give[0]", "Patient.name.give[1]"},
        {"Patient.address[0].city", "Patient.address[1].city"},
        {"Patient.telecom[0].value", "Patient.telecom[1].value"},
        {"Observation.component[0].code", "Observation.component[1].code"}
    };
    
    for (String[] testCase : testCases) {
      Map<String, String> map = new HashMap<>();
      map.put(testCase[0], "first");
      map.put(testCase[1], "second");
      
      String[] keys = map.keySet().toArray(new String[0]);
      String order = keys[0].contains("[0]") ? "[0] first" : "[1] first";
      
      System.out.println("  " + testCase[0].substring(0, testCase[0].indexOf('[')) + ": " + order);
      System.out.println("    Hash [0]: " + testCase[0].hashCode());
      System.out.println("    Hash [1]: " + testCase[1].hashCode());
    }
  }

  private void testDifferentSizes() {
    System.out.println("\n2. Different HashMap Sizes (capacity changes):");
    
    for (int size = 1; size <= 5; size++) {
      Map<String, String> map = new HashMap<>();
      
      // Add different numbers of entries before our test keys
      for (int i = 0; i < size; i++) {
        map.put("dummy" + i, "value" + i);
      }
      
      map.put("Patient.name.given[0]", "Billy");
      map.put("Patient.name.given[1]", "Bob");
      
      // Find our test keys in iteration order
      String[] allKeys = map.keySet().toArray(new String[0]);
      String order = "unknown";
      
      for (int i = 0; i < allKeys.length - 1; i++) {
        if (allKeys[i].contains("given[0]") && allKeys[i + 1].contains("given[1]")) {
          order = "[0] then [1]";
          break;
        } else if (allKeys[i].contains("given[1]") && allKeys[i + 1].contains("given[0]")) {
          order = "[1] then [0]";
          break;
        }
      }
      
      System.out.println("  Size " + map.size() + ": " + order);
    }
  }

  private void testMultipleInstances() {
    System.out.println("\n3. Multiple HashMap Instances:");
    
    for (int i = 1; i <= 10; i++) {
      Map<String, String> map = new HashMap<>();
      map.put("Patient.name.given[0]", "Billy");
      map.put("Patient.name.given[1]", "Bob");
      
      String[] keys = map.keySet().toArray(new String[0]);
      String order = keys[0].contains("[0]") ? "[0] first" : "[1] first";
      
      System.out.println("  Instance " + i + ": " + order);
    }
  }

  @Test
  public void demonstrateJVMDifferences() {
    System.out.println("\n=== JVM-Level Factors ===");
    
    System.out.println("Current JVM:");
    System.out.println("  Java Version: " + System.getProperty("java.version"));
    System.out.println("  JVM Vendor: " + System.getProperty("java.vendor"));
    System.out.println("  JVM Implementation: " + System.getProperty("java.vm.name"));
    
    // Check for hash randomization
    System.out.println("\nHash Randomization Properties:");
    String[] hashProps = {
        "jdk.map.althashing.threshold",
        "java.util.HashMap.randomHashSeed",
        "jdk.util.HashMap.randomHashSeed"
    };
    
    for (String prop : hashProps) {
      String value = System.getProperty(prop);
      System.out.println("  " + prop + ": " + (value != null ? value : "not set"));
    }
    
    // Add assertion to prevent caching
    assert true;
  }

  @Test
  public void demonstrateCapacityEffects() {
    System.out.println("\n=== HashMap Capacity Effects ===");
    
    // Test with different initial capacities
    int[] capacities = {2, 4, 8, 16, 32};
    
    for (int capacity : capacities) {
      Map<String, String> map = new HashMap<>(capacity);
      map.put("Patient.name.given[0]", "Billy");
      map.put("Patient.name.given[1]", "Bob");
      
      String[] keys = map.keySet().toArray(new String[0]);
      String order = keys[0].contains("[0]") ? "[0] first" : "[1] first";
      
      System.out.println("  Capacity " + capacity + ": " + order);
    }
    
    // Add assertion to prevent caching
    assert true;
  }
}