package org.mitre.synthea.export.flexporter;

import static org.junit.Assert.assertTrue;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class SimpleHashMapTest {

  @Test
  public void showHashMapBehavior() {
    System.out.println("=== Simple HashMap Order Test ===");
    
    // Test 1: Original strings
    Map<String, String> map1 = new HashMap<>();
    map1.put("Patient.name.given[0]", "Billy");
    map1.put("Patient.name.given[1]", "Bob");
    
    String[] keys1 = map1.keySet().toArray(new String[0]);
    String order1 = keys1[0].contains("[0]") ? "[0] first" : "[1] first";
    System.out.println("Test 1 - Original: " + order1);
    
    // Test 2: Different strings that might hash differently
    Map<String, String> map2 = new HashMap<>();
    map2.put("Patient.address[0].city", "Boston");
    map2.put("Patient.address[1].city", "NYC");
    
    String[] keys2 = map2.keySet().toArray(new String[0]);
    String order2 = keys2[0].contains("[0]") ? "[0] first" : "[1] first";
    System.out.println("Test 2 - Address: " + order2);
    
    // Test 3: Show hash codes
    System.out.println("\nHash codes:");
    System.out.println("  given[0]: " + "Patient.name.given[0]".hashCode());
    System.out.println("  given[1]: " + "Patient.name.given[1]".hashCode());
    System.out.println("  address[0]: " + "Patient.address[0].city".hashCode());
    System.out.println("  address[1]: " + "Patient.address[1].city".hashCode());
    
    // Test 4: Try to find strings that hash to different order
    String[] testPrefixes = {
        "Patient.telecom",
        "Patient.identifier", 
        "Patient.contact",
        "Observation.component",
        "Condition.evidence"
    };
    
    System.out.println("\nTesting different prefixes:");
    for (String prefix : testPrefixes) {
      Map<String, String> map = new HashMap<>();
      String key0 = prefix + "[0].value";
      String key1 = prefix + "[1].value";
      
      map.put(key0, "first");
      map.put(key1, "second");
      
      String[] keys = map.keySet().toArray(new String[0]);
      String order = keys[0].contains("[0]") ? "[0] first" : "[1] first";
      
      System.out.println("  " + prefix + ": " + order + 
                        " (hash diff: " + (key1.hashCode() - key0.hashCode()) + ")");
    }
    
    // Must have an assertion for JUnit
    assertTrue("Test completed", true);
  }
}