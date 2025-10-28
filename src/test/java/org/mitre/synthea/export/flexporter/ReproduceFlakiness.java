package org.mitre.synthea.export.flexporter;

import static org.junit.Assert.assertEquals;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.StringType;
import org.junit.Test;

// public class ReproduceFlakiness {

//   private Patient createPatient(Map<String, Object> fhirPathMapping) {
//     CustomFHIRPathResourceGeneratorR4<Patient> fhirPathGenerator =
//         new CustomFHIRPathResourceGeneratorR4<>();
//     fhirPathGenerator.setMapping(fhirPathMapping);
//     return fhirPathGenerator.generateResource(Patient.class);
//   }

//   @Test
//   public void testArrayOrderMatters_ReversedOrder() {
//     // Use LinkedHashMap to control iteration order
//     Map<String, Object> fhirPathMapping = new LinkedHashMap<>();
    
//     // Deliberately put index 1 BEFORE index 0 to force the problematic order
//     fhirPathMapping.put("Patient.name.given[1]", "Bob");
//     fhirPathMapping.put("Patient.name.given[0]", "Billy");

//     Patient patient = createPatient(fhirPathMapping);
//     List<StringType> given = patient.getNameFirstRep().getGiven();

//     System.out.println("Array size: " + given.size());
//     for (int i = 0; i < given.size(); i++) {
//       System.out.println("Index " + i + ": " + 
//         (given.get(i) == null ? "NULL" : given.get(i).getValueAsString()));
//     }

//     assertEquals(2, given.size());
//     assertEquals("Billy", given.get(0).getValueAsString());
//     assertEquals("Bob", given.get(1).getValueAsString());
//   }

//   @Test
//   public void testArrayOrderMatters_LargerGap() {
//     // Test with a larger gap to make the issue more obvious
//     Map<String, Object> fhirPathMapping = new LinkedHashMap<>();
    
//     // Put index 3 first, then 0 - this should create nulls at 1,2
//     fhirPathMapping.put("Patient.name.given[3]", "Fourth");
//     fhirPathMapping.put("Patient.name.given[0]", "First");

//     Patient patient = createPatient(fhirPathMapping);
//     List<StringType> given = patient.getNameFirstRep().getGiven();

//     System.out.println("Array size: " + given.size());
//     for (int i = 0; i < given.size(); i++) {
//       System.out.println("Index " + i + ": " + 
//         (given.get(i) == null ? "NULL" : given.get(i).getValueAsString()));
//     }

//     // This might fail if nulls are created at indices 1,2
//     assertEquals(4, given.size());
//     assertEquals("First", given.get(0).getValueAsString());
//     assertEquals("Fourth", given.get(3).getValueAsString());
//   }
// }