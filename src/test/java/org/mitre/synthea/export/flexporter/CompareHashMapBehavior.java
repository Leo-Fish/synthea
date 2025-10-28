package org.mitre.synthea.export.flexporter;

import static org.junit.Assert.assertEquals;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;
import org.junit.Test;

public class CompareHashMapBehavior {

  private Patient createPatient(Map<String, Object> fhirPathMapping) {
    CustomFHIRPathResourceGeneratorR4<Patient> fhirPathGenerator =
        new CustomFHIRPathResourceGeneratorR4<>();
    fhirPathGenerator.setMapping(fhirPathMapping);
    return fhirPathGenerator.generateResource(Patient.class);
  }

  @Test
  public void testOriginalWithHashMap() {
    // This is exactly like the original testArray1
    Map<String, Object> fhirPathMapping = new HashMap<>();
    fhirPathMapping.put("Patient.name.given[0]", "Billy");
    fhirPathMapping.put("Patient.name.given[1]", "Bob");

    System.out.println("=== HashMap Test (like original testArray1) ===");
    System.out.print("Iteration order: ");
    for (String key : fhirPathMapping.keySet()) {
      System.out.print(key.substring(key.indexOf('[')) + " ");
    }
    System.out.println();

    Patient patient = createPatient(fhirPathMapping);
    List<StringType> given = patient.getNameFirstRep().getGiven();

    System.out.println("Result:");
    for (int i = 0; i < given.size(); i++) {
      System.out.println("Index " + i + ": " + given.get(i).getValueAsString());
    }

    // This assertion might pass or fail depending on HashMap order
    try {
      assertEquals("Billy", given.get(0).getValueAsString());
      assertEquals("Bob", given.get(1).getValueAsString());
      System.out.println("✅ Test PASSED - HashMap happened to iterate in good order");
    } catch (AssertionError e) {
      System.out.println("❌ Test FAILED - HashMap iterated in bad order");
      System.out.println("Error: " + e.getMessage());
    }
  }

  @Test
  public void testForcedBadOrderWithLinkedHashMap() {
    // This forces the bad order every time
    Map<String, Object> fhirPathMapping = new LinkedHashMap<>();
    fhirPathMapping.put("Patient.name.given[1]", "Bob");    // Bad order
    fhirPathMapping.put("Patient.name.given[0]", "Billy");

    System.out.println("\n=== LinkedHashMap Test (forced bad order) ===");
    System.out.print("Iteration order: ");
    for (String key : fhirPathMapping.keySet()) {
      System.out.print(key.substring(key.indexOf('[')) + " ");
    }
    System.out.println();

    Patient patient = createPatient(fhirPathMapping);
    List<StringType> given = patient.getNameFirstRep().getGiven();

    System.out.println("Result:");
    for (int i = 0; i < given.size(); i++) {
      System.out.println("Index " + i + ": " + given.get(i).getValueAsString());
    }

    // This will always fail because we forced the bad order
    try {
      assertEquals("Billy", given.get(0).getValueAsString());
      assertEquals("Bob", given.get(1).getValueAsString());
      System.out.println("✅ Test PASSED - unexpected!");
    } catch (AssertionError e) {
      System.out.println("❌ Test FAILED - as expected with bad order");
      System.out.println("Error: " + e.getMessage());
    }
  }

  @Test
  public void testGoodOrderWithLinkedHashMap() {
    // This forces the good order every time
    Map<String, Object> fhirPathMapping = new LinkedHashMap<>();
    fhirPathMapping.put("Patient.name.given[0]", "Billy");  // Good order
    fhirPathMapping.put("Patient.name.given[1]", "Bob");

    System.out.println("\n=== LinkedHashMap Test (forced good order) ===");
    System.out.print("Iteration order: ");
    for (String key : fhirPathMapping.keySet()) {
      System.out.print(key.substring(key.indexOf('[')) + " ");
    }
    System.out.println();

    Patient patient = createPatient(fhirPathMapping);
    List<StringType> given = patient.getNameFirstRep().getGiven();

    System.out.println("Result:");
    for (int i = 0; i < given.size(); i++) {
      System.out.println("Index " + i + ": " + given.get(i).getValueAsString());
    }

    // This should always pass because we forced the good order
    assertEquals("Billy", given.get(0).getValueAsString());
    assertEquals("Bob", given.get(1).getValueAsString());
    System.out.println("✅ Test PASSED - as expected with good order");
  }
}