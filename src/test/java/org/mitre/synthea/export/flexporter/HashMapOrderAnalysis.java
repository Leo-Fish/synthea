package org.mitre.synthea.export.flexporter;

import static org.junit.Assert.fail;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class HashMapOrderAnalysis {

  @Test
  public void analyzeHashMapOrder() {
    StringBuilder report = new StringBuilder();
    report.append("\n=== HashMap Order Analysis ===\n");
    
    // Test 1: Original strings
    Map<String, String> map1 = new HashMap<>();
    map1.put("Patient.name.given[0]", "Billy");
    map1.put("Patient.name.given[1]", "Bob");
    
    String[] keys1 = map1.keySet().toArray(new String[0]);
    String order1 = keys1[0].contains("[0]") ? "[0] first" : "[1] first";
    report.append("Original test: ").append(order1).append("\n");
    
    // Test 2: Different strings
    String[] testPrefixes = {
        "Patient.address",
        "Patient.telecom", 
        "Patient.identifier",
        "Observation.component",
        "Condition.evidence"
    };
    
    report.append("\nTesting different prefixes:\n");
    boolean foundDifferentOrder = false;
    
    for (String prefix : testPrefixes) {
      Map<String, String> map = new HashMap<>();
      String key0 = prefix + "[0].value";
      String key1 = prefix + "[1].value";
      
      map.put(key0, "first");
      map.put(key1, "second");
      
      String[] keys = map.keySet().toArray(new String[0]);
      String order = keys[0].contains("[0]") ? "[0] first" : "[1] first";
      
      report.append("  ").append(prefix).append(": ").append(order);
      report.append(" (hash0: ").append(key0.hashCode());
      report.append(", hash1: ").append(key1.hashCode());
      report.append(", diff: ").append(key1.hashCode() - key0.hashCode()).append(")\n");
      
      if (order.equals("[1] first")) {
        foundDifferentOrder = true;
      }
    }
    
    // Test 3: Multiple instances to check consistency
    report.append("\nTesting consistency across multiple HashMap instances:\n");
    for (int i = 1; i <= 5; i++) {
      Map<String, String> map = new HashMap<>();
      map.put("Patient.name.given[0]", "Billy");
      map.put("Patient.name.given[1]", "Bob");
      
      String[] keys = map.keySet().toArray(new String[0]);
      String order = keys[0].contains("[0]") ? "[0] first" : "[1] first";
      report.append("  Instance ").append(i).append(": ").append(order).append("\n");
    }
    
    // Test 4: JVM info
    report.append("\nJVM Information:\n");
    report.append("  Java Version: ").append(System.getProperty("java.version")).append("\n");
    report.append("  JVM Vendor: ").append(System.getProperty("java.vendor")).append("\n");
    
    // Always fail to show the report
    fail("HashMap Order Analysis Report:" + report.toString() + 
         "\nFound different order: " + foundDifferentOrder +
         "\nThis test always fails to display the analysis results.");
  }
}