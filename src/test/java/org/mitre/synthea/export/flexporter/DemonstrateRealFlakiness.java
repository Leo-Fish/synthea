package org.mitre.synthea.export.flexporter;

import static org.junit.Assert.assertEquals;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;
import org.junit.Test;

public class DemonstrateRealFlakiness {

  private Patient createPatient(Map<String, Object> fhirPathMapping) {
    CustomFHIRPathResourceGeneratorR4<Patient> fhirPathGenerator =
        new CustomFHIRPathResourceGeneratorR4<>();
    fhirPathGenerator.setMapping(fhirPathMapping);
    return fhirPathGenerator.generateResource(Patient.class);
  }

  @Test
  public void testWorkingCase_PatientNameGiven() {
    // This works because hash codes put [0] before [1]
    Map<String, Object> fhirPathMapping = new HashMap<>();
    fhirPathMapping.put("Patient.name.given[0]", "Billy");
    fhirPathMapping.put("Patient.name.given[1]", "Bob");

    Patient patient = createPatient(fhirPathMapping);
    List<StringType> given = patient.getNameFirstRep().getGiven();

    System.out.println("Working case - name.given:");
    for (int i = 0; i < given.size(); i++) {
      System.out.println("  Index " + i + ": " + given.get(i).getValueAsString());
    }

    assertEquals(2, given.size());
    assertEquals("Billy", given.get(0).getValueAsString());
    assertEquals("Bob", given.get(1).getValueAsString());
  }

  @Test  
  public void testFailingCase_PatientAddress() {
    // This should fail because hash codes put [1] before [0]
    Map<String, Object> fhirPathMapping = new HashMap<>();
    fhirPathMapping.put("Patient.address[0].city", "Boston");
    fhirPathMapping.put("Patient.address[1].city", "NYC");

    Patient patient = createPatient(fhirPathMapping);
    
    // Check if we have addresses
    if (patient.getAddress().size() > 0) {
      System.out.println("Failing case - address:");
      for (int i = 0; i < patient.getAddress().size(); i++) {
        System.out.println("  Index " + i + ": " + patient.getAddress().get(i).getCity());
      }

      // This assertion will likely fail due to wrong order
      assertEquals(2, patient.getAddress().size());
      assertEquals("Boston", patient.getAddress().get(0).getCity());
      assertEquals("NYC", patient.getAddress().get(1).getCity());
    } else {
      System.out.println("No addresses created - FHIR path might not work for address");
    }
  }
}