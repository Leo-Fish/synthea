package org.mitre.synthea.export.flexporter;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class DeepDiveHashMap {

  @Test
  public void analyzeHashMapBehavior() {
    System.out.println("=== HashMap Analysis ===");
    System.out.println("Java Version: " + System.getProperty("java.version"));
    System.out.println("Java Vendor: " + System.getProperty("java.vendor"));
    System.out.println("JVM Name: " + System.getProperty("java.vm.name"));
    
    // Test HashMap iteration order stability
    System.out.println("\n=== Testing HashMap iteration order over multiple runs ===");
    
    for (int run = 1; run <= 20; run++) {
      Map<String, Object> map = new HashMap<>();
      map.put("Patient.name.given[0]", "Billy");
      map.put("Patient.name.given[1]", "Bob");
      
      String[] keys = map.keySet().toArray(new String[0]);
      String order = keys[0].contains("[0]") ? "[0] first" : "[1] first";
      
      System.out.println("Run " + run + ": " + order);
    }
    
    // Check hash codes
    System.out.println("\n=== Hash Code Analysis ===");
    String key0 = "Patient.name.given[0]";
    String key1 = "Patient.name.given[1]";
    
    System.out.println("Key '[0]' hash: " + key0.hashCode());
    System.out.println("Key '[1]' hash: " + key1.hashCode());
    System.out.println("Hash difference: " + (key1.hashCode() - key0.hashCode()));
    
    // Check if there are any system properties affecting HashMap
    System.out.println("\n=== System Properties (HashMap related) ===");
    System.getProperties().entrySet().stream()
        .filter(e -> e.getKey().toString().toLowerCase().contains("hash") ||
                     e.getKey().toString().toLowerCase().contains("map") ||
                     e.getKey().toString().toLowerCase().contains("jdk"))
        .forEach(e -> System.out.println(e.getKey() + " = " + e.getValue()));
  }

  @Test
  public void testExactOriginalScenario() {
    System.out.println("\n=== Exact Original Test Replication ===");
    
    // Replicate the exact original test multiple times
    for (int i = 1; i <= 10; i++) {
      Map<String, Object> fhirPathMapping = new HashMap<>();
      fhirPathMapping.put("Patient.name.given[0]", "Billy");
      fhirPathMapping.put("Patient.name.given[1]", "Bob");

      String[] keys = fhirPathMapping.keySet().toArray(new String[0]);
      String firstKey = keys[0];
      String order = firstKey.contains("[0]") ? "good ([0] first)" : "bad ([1] first)";
      
      System.out.println("Iteration " + i + ": " + order);
      
      // If we get bad order, let's see what happens
      if (firstKey.contains("[1]")) {
        System.out.println("  ⚠️  Found bad order! Testing actual behavior...");
        
        CustomFHIRPathResourceGeneratorR4<org.hl7.fhir.r4.model.Patient> generator = 
            new CustomFHIRPathResourceGeneratorR4<>();
        generator.setMapping(fhirPathMapping);
        
        try {
          org.hl7.fhir.r4.model.Patient patient = generator.generateResource(org.hl7.fhir.r4.model.Patient.class);
          java.util.List<org.hl7.fhir.r4.model.StringType> given = patient.getNameFirstRep().getGiven();
          
          System.out.println("    Result size: " + given.size());
          for (int j = 0; j < given.size(); j++) {
            System.out.println("    Index " + j + ": " + given.get(j).getValueAsString());
          }
          
          boolean correct = given.size() == 2 && 
                           "Billy".equals(given.get(0).getValueAsString()) &&
                           "Bob".equals(given.get(1).getValueAsString());
          
          System.out.println("    Result: " + (correct ? "✅ CORRECT" : "❌ INCORRECT"));
          
        } catch (Exception e) {
          System.out.println("    Exception: " + e.getMessage());
        }
      }
    }
  }
}