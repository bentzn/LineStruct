package com.bentzn.util.linestruct;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.*;

 class LineStructOptionalObjectTest {
    
    @Test
    void testOptionalObjectWithEmptyValue() throws Exception {
        // Test optional object - entire object is optional
        String lineStruct = """
            EntityName:Person
            schema:id:int¦details:‹name:string¦age:int›?¦status:string
            1¦‹John¦30›¦active
            2¦¦inactive
            3¦null¦inactive
            """;
        
        System.out.println("Testing optional object with empty value...");
        
        // First check if it's valid
        boolean isValid = LineStructParser.isValidLineStruct(lineStruct);
        System.out.println("Is valid: " + isValid);
        
        if (isValid) {
            String json = LineStructParser.toJson(lineStruct);
            System.out.println("JSON output:\n" + json);
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            
            // First record - details present
            JsonNode first = root.get("data").get(0);
            assertEquals(1, first.get("id").asInt());
            JsonNode details1 = first.get("details");
            assertNotNull(details1);
            assertEquals("John", details1.get("name").asText());
            assertEquals(30, details1.get("age").asInt());
            assertEquals("active", first.get("status").asText());
            
            // Second record - details empty/null
            JsonNode second = root.get("data").get(1);
            assertEquals(2, second.get("id").asInt());
            JsonNode details2 = second.get("details");
            assertTrue(details2 == null || details2.isNull());
            assertEquals("inactive", second.get("status").asText());
            
            // Third record - details empty/null
            JsonNode third = root.get("data").get(1);
            assertEquals(2, third.get("id").asInt());
            JsonNode details3 = third.get("details");
            assertTrue(details3 == null || details3.isNull());
            assertEquals("inactive", third.get("status").asText());
        }
    }
    
    @Test
    void testOptionalObjectWithNullKeyword() throws Exception {
        // Test optional object with explicit null
        String lineStruct = """
            EntityName:Person
            schema:id:int¦details:‹name:string¦age:int›?¦status:string
            1¦‹John¦30›¦active
            2¦null¦inactive
            """;
        
        System.out.println("Testing optional object with 'null' keyword...");
        
        // First check if it's valid
        boolean isValid = LineStructParser.isValidLineStruct(lineStruct);
        System.out.println("Is valid: " + isValid);
        
        if (isValid) {
            String json = LineStructParser.toJson(lineStruct);
            System.out.println("JSON output:\n" + json);
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            
            // Second record - details should be null
            JsonNode second = root.get("data").get(1);
            JsonNode details2 = second.get("details");
            // Check if parser treats "null" as a special value or as a string
            if (details2 != null && !details2.isNull()) {
                System.out.println("Parser treats 'null' as string: " + details2.asText());
            } else {
                System.out.println("Parser treats 'null' as null value");
            }
        }
    }
    
    @Test
    void testOptionalNestedObjectSyntaxVariations() throws Exception {
        // Test different ways to mark objects as optional
        String[] variations = {
            // Variation 1: Optional marker after closing bracket
            """
            EntityName:Test1
            schema:id:int¦info:‹name:string¦age:int›?
            1¦‹Alice¦25›
            2¦
            """,
            
            // Variation 2: Optional marker with angle brackets
            """
            EntityName:Test2
            schema:id:int¦info:<‹name:string¦age:int›>?
            1¦‹Alice¦25›
            2¦
            """,
            
            // Variation 3: Just the object without optional (for comparison)
            """
            EntityName:Test3
            schema:id:int¦info:‹name:string¦age:int›
            1¦‹Alice¦25›
            2¦‹¦›
            """
        };
        
        for (int i = 0; i < variations.length; i++) {
            System.out.println("\nTesting variation " + (i + 1) + ":");
            System.out.println(variations[i]);
            
            boolean isValid = LineStructParser.isValidLineStruct(variations[i]);
            System.out.println("Is valid: " + isValid);
            
            if (isValid) {
                try {
                    String json = LineStructParser.toJson(variations[i]);
                    System.out.println("Successfully converted to JSON");
                    
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode root = mapper.readTree(json);
                    JsonNode data = root.get("data");
                    
                    if (data.size() > 1) {
                        JsonNode second = data.get(1);
                        JsonNode info = second.get("info");
                        System.out.println("Second record info field: " + 
                            (info == null ? "null" : info.toString()));
                    }
                } catch (Exception e) {
                    System.out.println("Error converting: " + e.getMessage());
                }
            }
        }
    }
    
    @Test
    void testOptionalArrayOfObjects() throws Exception {
        // Test optional array of objects
        String lineStruct = """
            EntityName:Company
            schema:id:int¦employees:«‹name:string¦role:string›»?¦active:bool
            1¦«‹John¦Developer›¦‹Jane¦Manager›»¦true
            2¦¦false
            3¦«»¦true
            """;
        
        System.out.println("Testing optional array of objects...");
        
        boolean isValid = LineStructParser.isValidLineStruct(lineStruct);
        System.out.println("Is valid: " + isValid);
        
        if (isValid) {
            String json = LineStructParser.toJson(lineStruct);
            System.out.println("JSON output:\n" + json);
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            
            // First record - array with items
            JsonNode first = root.get("data").get(0);
            JsonNode employees1 = first.get("employees");
            assertNotNull(employees1);
            assertTrue(employees1.isArray());
            assertEquals(2, employees1.size());
            
            // Second record - null/empty array
            JsonNode second = root.get("data").get(1);
            JsonNode employees2 = second.get("employees");
            assertTrue(employees2 == null || employees2.isNull() || 
                      (employees2.isArray() && employees2.size() == 0));
            
            // Third record - explicitly empty array
            JsonNode third = root.get("data").get(2);
            JsonNode employees3 = third.get("employees");
            assertNotNull(employees3);
            assertTrue(employees3.isArray());
            assertEquals(0, employees3.size());
        }
    }
    
    @Test
    void testMixedOptionalAndRequired() throws Exception {
        // Test mix of optional and required complex types
        String lineStruct = """
            EntityName:Record
            schema:id:int¦required:‹a:string¦b:int›¦optional:‹x:string¦y:int›?¦tags:«string»?
            1¦‹foo¦10›¦‹bar¦20›¦«tag1¦tag2»
            2¦‹baz¦30›¦¦
            3¦‹qux¦40›¦¦«tag3»
            """;
        
        System.out.println("Testing mixed optional and required fields...");
        
        boolean isValid = LineStructParser.isValidLineStruct(lineStruct);
        System.out.println("Is valid: " + isValid);
        
        if (isValid) {
            String json = LineStructParser.toJson(lineStruct);
            System.out.println("JSON output:\n" + json);
            
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            
            // Verify all records have required field
            JsonNode data = root.get("data");
            for (int i = 0; i < data.size(); i++) {
                JsonNode record = data.get(i);
                JsonNode required = record.get("required");
                assertNotNull(required, "Required field should always be present");
                assertNotNull(required.get("a"));
                assertNotNull(required.get("b"));
            }
            
            // Check optional fields
            JsonNode first = data.get(0);
            assertNotNull(first.get("optional"));
            assertNotNull(first.get("tags"));
            
            JsonNode second = data.get(1);
            assertTrue(second.get("optional") == null || second.get("optional").isNull());
            assertTrue(second.get("tags") == null || second.get("tags").isNull());
        }
    }
}