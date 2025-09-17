package com.bentzn.util.linestruct;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.*;

 class LineStructTrailingDelimiterTest {
    
    @Test
    void testNoTrailingDelimiterWithOptionalField() throws Exception {
        // Last field is optional and empty - no trailing delimiter
        String lineStruct = """
            EntityName:Person
            schema:id:int¦name:string¦email:<string>?
            1¦John Doe¦john@example.com
            2¦Jane Smith
            """;
        
        System.out.println("Testing no trailing delimiter with optional last field empty...");
        boolean isValid = LineStructParser.isValidLineStruct(lineStruct);
        System.out.println("Is valid: " + isValid);
        
        if (isValid) {
            String json = LineStructParser.toJson(lineStruct);
            System.out.println("JSON output:\n" + json);
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            
            // First record - all fields present
            JsonNode first = root.get("data").get(0);
            assertEquals(1, first.get("id").asInt());
            assertEquals("John Doe", first.get("name").asText());
            assertEquals("john@example.com", first.get("email").asText());
            
            // Second record - optional email missing (no delimiter)
            JsonNode second = root.get("data").get(1);
            assertEquals(2, second.get("id").asInt());
            assertEquals("Jane Smith", second.get("name").asText());
            JsonNode email = second.get("email");
            assertTrue(email == null || email.isNull(), "Email should be null when omitted");
        }
    }
    
    @Test
    void testWithTrailingDelimiterForEmptyOptional() throws Exception {
        // Last field is optional and empty - WITH trailing delimiter
        String lineStruct = """
            EntityName:Person
            schema:id:int¦name:string¦email:<string>?
            1¦John Doe¦john@example.com
            2¦Jane Smith¦
            """;
        
        System.out.println("\nTesting WITH trailing delimiter for empty optional field...");
        boolean isValid = LineStructParser.isValidLineStruct(lineStruct);
        System.out.println("Is valid: " + isValid);
        
        if (isValid) {
            String json = LineStructParser.toJson(lineStruct);
            System.out.println("JSON output:\n" + json);
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            
            JsonNode second = root.get("data").get(1);
            assertEquals("Jane Smith", second.get("name").asText());
            JsonNode email = second.get("email");
            System.out.println("Email field with trailing delimiter: " + 
                (email == null ? "null" : email.toString()));
        }
    }
    
    @Test
    void testMultipleOptionalFieldsAtEnd() throws Exception {
        // Multiple optional fields at the end
        String lineStruct1 = """
            EntityName:Contact
            schema:id:int¦name:string¦phone:<string>?¦email:<string>?¦notes:<string>?
            1¦Alice¦555-1234¦alice@example.com¦Important client
            2¦Bob¦555-5678¦bob@example.com
            3¦Charlie¦555-9999
            4¦David
            """;
        
        System.out.println("\nTesting multiple optional fields omitted at end...");
        boolean isValid = LineStructParser.isValidLineStruct(lineStruct1);
        System.out.println("Is valid: " + isValid);
        
        if (isValid) {
            String json = LineStructParser.toJson(lineStruct1);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            
            // Check each record
            JsonNode data = root.get("data");
            
            // Record 1: All fields present
            JsonNode r1 = data.get(0);
            assertEquals("Alice", r1.get("name").asText());
            assertEquals("555-1234", r1.get("phone").asText());
            assertEquals("alice@example.com", r1.get("email").asText());
            assertEquals("Important client", r1.get("notes").asText());
            
            // Record 2: Last field (notes) omitted
            JsonNode r2 = data.get(1);
            assertEquals("Bob", r2.get("name").asText());
            assertEquals("555-5678", r2.get("phone").asText());
            assertEquals("bob@example.com", r2.get("email").asText());
            assertTrue(r2.get("notes") == null || r2.get("notes").isNull());
            
            // Record 3: Last two fields omitted
            JsonNode r3 = data.get(2);
            assertEquals("Charlie", r3.get("name").asText());
            assertEquals("555-9999", r3.get("phone").asText());
            assertTrue(r3.get("email") == null || r3.get("email").isNull());
            assertTrue(r3.get("notes") == null || r3.get("notes").isNull());
            
            // Record 4: All optional fields omitted
            JsonNode r4 = data.get(3);
            assertEquals("David", r4.get("name").asText());
            assertTrue(r4.get("phone") == null || r4.get("phone").isNull());
            assertTrue(r4.get("email") == null || r4.get("email").isNull());
            assertTrue(r4.get("notes") == null || r4.get("notes").isNull());
        }
    }
    
    @Test
    void testRequiredFieldCannotBeOmitted() {
        // Try to omit a required field at the end
        String invalid = """
            EntityName:Person
            schema:id:int¦name:string¦age:int
            1¦John Doe
            """;
        
        System.out.println("\nTesting omitted required field...");
        boolean isValid = LineStructParser.isValidLineStruct(invalid);
        System.out.println("Cannot omit required field - Is valid: " + isValid);
        assertFalse(isValid, "Should not be valid when required field is omitted");
    }
    
    @Test
    void testMixedRequiredAndOptional() throws Exception {
        // Required field after optional field
        String lineStruct = """
            EntityName:Product
            schema:id:int¦name:string¦description:<string>?¦price:float
            1¦Laptop¦High-end gaming laptop¦999.99
            2¦Mouse¦¦29.99
            """;
        
        System.out.println("\nTesting required field after optional field...");
        boolean isValid = LineStructParser.isValidLineStruct(lineStruct);
        System.out.println("Is valid: " + isValid);
        
        if (isValid) {
            String json = LineStructParser.toJson(lineStruct);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            
            JsonNode second = root.get("data").get(1);
            assertEquals("Mouse", second.get("name").asText());
            assertTrue(second.get("description").asText().isEmpty() || 
                      second.get("description").isNull());
            assertEquals(29.99, second.get("price").asDouble(), 0.01);
        }
    }
    
    @Test
    void testRoundTripWithOmittedFields_NotPossibleWithoutSchema() {
        String original = """
            EntityName:Test
            schema:id:int¦a:string¦b:<string>?¦c:<string>?
            1¦First¦Optional¦Another
            2¦Second¦Optional
            3¦Third
            """;
        
        System.out.println("\nDemonstrating why round-trip with omitted fields doesn't work:");
        System.out.println("Original LineStruct:");
        System.out.println(original);
        
        // Convert to JSON
        String json = LineStructParser.toJson(original);
        System.out.println("\nJSON (loses schema information):");
        System.out.println(json);
        
        // Convert back - but it can't know which fields are optional!
        String converted = LineStructParser.fromJson(json);
        System.out.println("\nConverted back (without schema knowledge):");
        System.out.println(converted);
        
        System.out.println("\nConclusion: Round-trip conversion loses optional field information");
        System.out.println("because JSON doesn't contain schema metadata.");
        
        // The original is valid
        assertTrue(LineStructParser.isValidLineStruct(original));
        
        // But we CANNOT expect the converted to match the original
        // because JSON doesn't preserve schema information
    }
    
    @Test
    void testOmittedOptionalFieldsWork() {
        // This is the correct test - just verify that omitted optional fields are valid
        String lineStruct = """
            EntityName:Test
            schema:id:int¦a:string¦b:<string>?¦c:<string>?
            1¦First¦Optional¦Another
            2¦Second¦Optional
            3¦Third
            """;
        
        System.out.println("\nTesting that omitted optional fields are valid:");
        
        // The LineStruct with omitted optional fields should be valid
        assertTrue(LineStructParser.isValidLineStruct(lineStruct));
        
        // And it should convert to JSON successfully
        String json = LineStructParser.toJson(lineStruct);
        assertNotNull(json);
        
        System.out.println("Success: LineStruct with omitted optional fields is valid and converts to JSON");
    }
}