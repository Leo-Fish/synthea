package org.mitre.synthea.export.flexporter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hl7.fhir.r4.model.Patient;
import org.junit.Test;

public class DebugArrayOrder {

  @Test
  public void demonstrateHashMapOrderVariation() {
    System.out.println("=== Demonstrating HashMap iteration order variation ===");
    
    // Run multiple times to show different orders
    for (int run = 1; run <= 5; run++) {
      System.out.println("\nRun " + run + ":");
      
      Map<String, Object> fhirPathMapping = new HashMap<>();
      fhirPathMapping.put("Patient.name.given[0]", "Billy");
      fhirPathMapping.put("Patient.name.given[1]", "Bob");
      
      System.out.print("HashMap iteration order: ");
      for (String key : fhirPathMapping.keySet()) {
        System.out.print(key + " ");
      }
      System.out.println();
      
      // Show what sortedPaths returns
      CustomFHIRPathResourceGeneratorR4<Patient> generator = 
          new CustomFHIRPathResourceGeneratorR4<>();
      generator.setMapping(fhirPathMapping);
      
      // We'd need to make sortedPaths public or use reflection to call it
      // For now, just show the HashMap order variation
    }
  }

  @Test
  public void demonstrateControlledOrder() {
    System.out.println("\n=== Demonstrating controlled order with LinkedHashMap ===");
    
    System.out.println("\nCorrect order (0 then 1):");
    Map<String, Object> correctOrder = new LinkedHashMap<>();
    correctOrder.put("Patient.name.given[0]", "Billy");
    correctOrder.put("Patient.name.given[1]", "Bob");
    
    for (String key : correctOrder.keySet()) {
      System.out.print(key + " ");
    }
    
    System.out.println("\n\nProblematic order (1 then 0):");
    Map<String, Object> problematicOrder = new LinkedHashMap<>();
    problematicOrder.put("Patient.name.given[1]", "Bob");
    problematicOrder.put("Patient.name.given[0]", "Billy");
    
    for (String key : problematicOrder.keySet()) {
      System.out.print(key + " ");
    }
    System.out.println();
  }
}