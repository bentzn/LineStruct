package com.bentzn.util.linestruct;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for LineStructParser with large datasets.
 * Tests parsing of 200 lines of complex LineStruct data to JSON.
 */
 class LargeLineStructTest {
    
    private ObjectMapper mapper;
    
    @BeforeEach
    public void setUp() {
        mapper = new ObjectMapper();
    }
    
    @Test
    public void testLargeLineStructToJson() throws Exception {
        // Create a LineStruct with 200 data lines
        StringBuilder lineStruct = new StringBuilder();
        
        // Entity name
        lineStruct.append("EntityName:Employee\n");
        
        // Complex schema with various data types
        lineStruct.append("schema:")
            .append("id:int")
            .append("¦employeeCode:string")
            .append("¦firstName:string")
            .append("¦lastName:string")
            .append("¦email:string")
            .append("¦phone:<string>?")
            .append("¦age:int")
            .append("¦salary:float")
            .append("¦isActive:bool")
            .append("¦department:{ENGINEERING|SALES|MARKETING|HR|FINANCE|OPERATIONS}")
            .append("¦joinDate:date")
            .append("¦lastLogin:datetime")
            .append("¦skills:«string»")
            .append("¦address:‹street:string¦city:string¦state:string¦zipCode:string¦country:string›")
            .append("¦emergencyContacts:«‹name:string¦relationship:string¦phone:string›»")
            .append("¦projects:«‹projectId:int¦projectName:string¦role:string¦startDate:date¦endDate:<date>?›»")
            .append("¦performanceRatings:«‹year:int¦rating:float¦comments:<string>?›»")
            .append("¦certifications:<«‹name:string¦issuer:string¦date:date¦expiryDate:<date>?›»>?")
            .append("¦metadata:‹createdBy:string¦createdAt:datetime¦updatedBy:<string>?¦updatedAt:<datetime>?›")
            .append("\n");
        
        // Generate 200 data lines with varied content
        for (int i = 1; i <= 200; i++) {
            lineStruct.append(generateEmployeeDataLine(i)).append("\n");
        }
        
        // Convert to JSON
        String jsonResult = LineStructParser.toJson(lineStruct.toString());
        assertNotNull(jsonResult, "JSON result should not be null");
        
        // Parse JSON and validate
        JsonNode rootNode = mapper.readTree(jsonResult);
        assertNotNull(rootNode, "Parsed JSON should not be null");
        
        // Verify entity name
        assertEquals("Employee", rootNode.get("entityName").asText(), 
            "Entity name should be 'Employee'");
        
        // Verify data array
        assertTrue(rootNode.has("data"), "JSON should have 'data' field");
        JsonNode dataArray = rootNode.get("data");
        assertTrue(dataArray.isArray(), "Data should be an array");
        assertEquals(200, dataArray.size(), "Should have exactly 200 data records");
        
        // Verify first record structure
        JsonNode firstRecord = dataArray.get(0);
        validateEmployeeRecord(firstRecord, 1);
        
        // Verify middle record
        JsonNode middleRecord = dataArray.get(99);
        validateEmployeeRecord(middleRecord, 100);
        
        // Verify last record
        JsonNode lastRecord = dataArray.get(199);
        validateEmployeeRecord(lastRecord, 200);
        
        // Spot check random records
        validateEmployeeRecord(dataArray.get(24), 25);
        validateEmployeeRecord(dataArray.get(149), 150);
        validateEmployeeRecord(dataArray.get(174), 175);
        
        // Verify all records have required fields
        for (int i = 0; i < 200; i++) {
            JsonNode record = dataArray.get(i);
            assertTrue(record.has("id"), "Record " + i + " should have 'id' field");
            assertTrue(record.has("employeeCode"), "Record " + i + " should have 'employeeCode' field");
            assertTrue(record.has("firstName"), "Record " + i + " should have 'firstName' field");
            assertTrue(record.has("lastName"), "Record " + i + " should have 'lastName' field");
            assertTrue(record.has("email"), "Record " + i + " should have 'email' field");
            assertTrue(record.has("department"), "Record " + i + " should have 'department' field");
            assertTrue(record.has("isActive"), "Record " + i + " should have 'isActive' field");
            
            // Verify complex nested structures
            assertTrue(record.has("address"), "Record " + i + " should have 'address' field");
            assertTrue(record.get("address").isObject(), "Address should be an object");
            
            assertTrue(record.has("skills"), "Record " + i + " should have 'skills' field");
            assertTrue(record.get("skills").isArray(), "Skills should be an array");
            
            assertTrue(record.has("emergencyContacts"), "Record " + i + " should have 'emergencyContacts' field");
            assertTrue(record.get("emergencyContacts").isArray(), "Emergency contacts should be an array");
            
            assertTrue(record.has("projects"), "Record " + i + " should have 'projects' field");
            assertTrue(record.get("projects").isArray(), "Projects should be an array");
        }
        
        System.out.println("Successfully parsed 200 lines of LineStruct data to JSON");
        System.out.println("Total JSON size: " + jsonResult.length() + " characters");
    }
    
    /**
     * Generate a single employee data line with realistic varied content
     */
    private String generateEmployeeDataLine(int index) {
        StringBuilder line = new StringBuilder();
        
        // id
        line.append(index);
        
        // employeeCode
        line.append("¦EMP").append(String.format("%05d", index));
        
        // firstName
        String[] firstNames = {"John", "Jane", "Michael", "Sarah", "David", "Emma", "Robert", "Lisa", "James", "Mary"};
        line.append("¦").append(firstNames[index % 10]);
        
        // lastName
        String[] lastNames = {"Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez"};
        line.append("¦").append(lastNames[(index * 3) % 10]);
        
        // email
        line.append("¦").append("employee").append(index).append("@company.com");
        
        // phone (optional - some null)
        if (index % 5 == 0) {
            line.append("¦"); // null phone
        } else {
            line.append("¦+1-555-").append(String.format("%04d", (index * 7) % 10000));
        }
        
        // age
        line.append("¦").append(22 + (index % 45));
        
        // salary
        line.append("¦").append(String.format("%.2f", 50000 + (index * 1234.56) % 150000));
        
        // isActive
        line.append("¦").append(index % 10 != 0 ? "true" : "false");
        
        // department enum
        String[] departments = {"ENGINEERING", "SALES", "MARKETING", "HR", "FINANCE", "OPERATIONS"};
        line.append("¦").append(departments[index % 6]);
        
        // joinDate
        line.append("¦").append(String.format("20%02d-%02d-%02d", 
            10 + (index % 15), 
            1 + (index % 12), 
            1 + (index % 28)));
        
        // lastLogin datetime
        line.append("¦").append(String.format("2024-%02d-%02dT%02d:%02d:00Z",
            1 + (index % 12),
            1 + (index % 28),
            (index % 24),
            (index % 60)));
        
        // skills array
        line.append("¦«");
        int numSkills = 1 + (index % 4);
        String[] allSkills = {"Java", "Python", "JavaScript", "SQL", "Docker", "Kubernetes", "AWS", "React", "Spring", "Node.js"};
        for (int s = 0; s < numSkills; s++) {
            if (s > 0) line.append("¦");
            line.append(allSkills[(index + s) % 10]);
        }
        line.append("»");
        
        // address object
        line.append("¦‹");
        line.append(index).append(" Main St");
        line.append("¦").append(index % 2 == 0 ? "New York" : "San Francisco");
        line.append("¦").append(index % 2 == 0 ? "NY" : "CA");
        line.append("¦").append(String.format("%05d", 10000 + index));
        line.append("¦USA");
        line.append("›");
        
        // emergencyContacts array of objects
        line.append("¦«");
        int numContacts = 1 + (index % 3);
        for (int c = 0; c < numContacts; c++) {
            if (c > 0) line.append("¦");
            line.append("‹");
            line.append("Contact").append(c + 1);
            line.append("¦").append(c == 0 ? "Spouse" : c == 1 ? "Parent" : "Sibling");
            line.append("¦+1-555-").append(String.format("%04d", (index + c) * 13 % 10000));
            line.append("›");
        }
        line.append("»");
        
        // projects array of objects with optional fields
        line.append("¦«");
        int numProjects = index % 4;
        for (int p = 0; p < numProjects; p++) {
            if (p > 0) line.append("¦");
            line.append("‹");
            line.append(100 + index + p);
            line.append("¦Project-").append((char)('A' + p));
            line.append("¦").append(p == 0 ? "Lead" : p == 1 ? "Developer" : "Analyst");
            line.append("¦2023-0").append(1 + p).append("-01");
            if (p % 2 == 0) {
                line.append("¦2024-0").append(1 + p).append("-01");
            } else {
                line.append("¦"); // null end date
            }
            line.append("›");
        }
        line.append("»");
        
        // performanceRatings array
        line.append("¦«");
        int numRatings = 1 + (index % 3);
        for (int r = 0; r < numRatings; r++) {
            if (r > 0) line.append("¦");
            line.append("‹");
            line.append(2022 + r);
            line.append("¦").append(String.format("%.1f", 3.0 + (index % 3) * 0.5));
            if (index % 2 == 0) {
                line.append("¦Good performance");
            } else {
                line.append("¦"); // null comments
            }
            line.append("›");
        }
        line.append("»");
        
        // certifications (optional array) - some employees have none
        if (index % 3 == 0) {
            line.append("¦"); // null certifications
        } else {
            line.append("¦«");
            int numCerts = 1 + (index % 2);
            for (int cert = 0; cert < numCerts; cert++) {
                if (cert > 0) line.append("¦");
                line.append("‹");
                line.append(cert == 0 ? "AWS Certified" : "Java Certified");
                line.append("¦").append(cert == 0 ? "Amazon" : "Oracle");
                line.append("¦2023-0").append(1 + cert).append("-15");
                if (cert == 0) {
                    line.append("¦2025-0").append(1 + cert).append("-15");
                } else {
                    line.append("¦"); // no expiry
                }
                line.append("›");
            }
            line.append("»");
        }
        
        // metadata object
        line.append("¦‹");
        line.append("admin").append(index % 3);
        line.append("¦2024-01-01T00:00:00Z");
        if (index % 2 == 0) {
            line.append("¦user").append(index);
            line.append("¦2024-06-15T12:30:00Z");
        } else {
            line.append("¦¦"); // null updatedBy and updatedAt
        }
        line.append("›");
        
        return line.toString();
    }
    
    /**
     * Validate an employee record has the expected structure and values
     */
    private void validateEmployeeRecord(JsonNode record, int expectedId) {
        assertNotNull(record, "Record should not be null");
        
        // Basic fields
        assertEquals(expectedId, record.get("id").asInt(), 
            "ID should be " + expectedId);
        assertEquals("EMP" + String.format("%05d", expectedId), 
            record.get("employeeCode").asText(),
            "Employee code should match expected format");
        
        // Validate email format
        String expectedEmail = "employee" + expectedId + "@company.com";
        assertEquals(expectedEmail, record.get("email").asText(),
            "Email should match expected format");
        
        // Validate boolean field
        assertTrue(record.has("isActive"), "Should have isActive field");
        assertTrue(record.get("isActive").isBoolean(), "isActive should be boolean");
        
        // Validate enum field
        String dept = record.get("department").asText();
        assertTrue(dept.matches("ENGINEERING|SALES|MARKETING|HR|FINANCE|OPERATIONS"),
            "Department should be valid enum value");
        
        // Validate date format
        String joinDate = record.get("joinDate").asText();
        assertTrue(joinDate.matches("\\d{4}-\\d{2}-\\d{2}"),
            "Join date should be in YYYY-MM-DD format");
        
        // Validate datetime format
        String lastLogin = record.get("lastLogin").asText();
        assertTrue(lastLogin.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"),
            "Last login should be in ISO 8601 format");
        
        // Validate address structure
        JsonNode address = record.get("address");
        assertTrue(address.has("street"), "Address should have street");
        assertTrue(address.has("city"), "Address should have city");
        assertTrue(address.has("state"), "Address should have state");
        assertTrue(address.has("zipCode"), "Address should have zipCode");
        assertTrue(address.has("country"), "Address should have country");
        assertEquals("USA", address.get("country").asText(), "Country should be USA");
        
        // Validate skills array
        JsonNode skills = record.get("skills");
        assertTrue(skills.size() >= 1 && skills.size() <= 4, 
            "Should have 1-4 skills");
        
        // Validate emergency contacts
        JsonNode contacts = record.get("emergencyContacts");
        assertTrue(contacts.size() >= 1 && contacts.size() <= 3,
            "Should have 1-3 emergency contacts");
        if (contacts.size() > 0) {
            JsonNode firstContact = contacts.get(0);
            assertTrue(firstContact.has("name"), "Contact should have name");
            assertTrue(firstContact.has("relationship"), "Contact should have relationship");
            assertTrue(firstContact.has("phone"), "Contact should have phone");
        }
        
        // Validate projects array structure
        JsonNode projects = record.get("projects");
        assertTrue(projects.isArray(), "Projects should be an array");
        for (int i = 0; i < projects.size(); i++) {
            JsonNode project = projects.get(i);
            assertTrue(project.has("projectId"), "Project should have projectId");
            assertTrue(project.has("projectName"), "Project should have projectName");
            assertTrue(project.has("role"), "Project should have role");
            assertTrue(project.has("startDate"), "Project should have startDate");
            // endDate is optional
        }
        
        // Validate metadata
        JsonNode metadata = record.get("metadata");
        assertTrue(metadata.has("createdBy"), "Metadata should have createdBy");
        assertTrue(metadata.has("createdAt"), "Metadata should have createdAt");
        // updatedBy and updatedAt are optional
    }
    
    @Test
    public void testLargeLineStructToJsonSample() throws Exception {
        // Create the same large LineStruct
        StringBuilder lineStruct = new StringBuilder();
        lineStruct.append("EntityName:Employee\n");
        lineStruct.append("schema:")
            .append("id:int")
            .append("¦employeeCode:string")
            .append("¦firstName:string")
            .append("¦lastName:string")
            .append("¦email:string")
            .append("\n");
        
        // Add 200 lines but only check that toJsonSample returns 3
        for (int i = 1; i <= 200; i++) {
            lineStruct.append(i)
                .append("¦EMP").append(String.format("%05d", i))
                .append("¦FirstName").append(i)
                .append("¦LastName").append(i)
                .append("¦employee").append(i).append("@company.com")
                .append("\n");
        }
        
        // Convert using toJsonSample (should only return 3 records)
        String jsonSample = LineStructParser.toJsonSample(lineStruct.toString());
        assertNotNull(jsonSample, "JSON sample should not be null");
        
        // Parse and verify
        JsonNode rootNode = mapper.readTree(jsonSample);
        JsonNode dataArray = rootNode.get("data");
        
        assertEquals(3, dataArray.size(), 
            "toJsonSample should return exactly 3 records even with 200 input lines");
        
        // Verify the 3 records are the first 3
        for (int i = 0; i < 3; i++) {
            JsonNode record = dataArray.get(i);
            assertEquals(i + 1, record.get("id").asInt(), 
                "Record " + i + " should have id " + (i + 1));
            assertEquals("EMP" + String.format("%05d", i + 1), 
                record.get("employeeCode").asText(),
                "Record " + i + " should have correct employee code");
        }
        
        System.out.println("Successfully verified toJsonSample returns only 3 records from 200 lines");
    }
}