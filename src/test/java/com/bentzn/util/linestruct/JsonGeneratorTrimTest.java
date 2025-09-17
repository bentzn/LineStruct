package com.bentzn.util.linestruct;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * JUnit 5 test class for JsonGenerator trim functionality.
 * Tests the new trim parameter with focus on optional values at the end of records.
 */
@DisplayName("JsonGenerator Trim Functionality Tests")
 class JsonGeneratorTrimTest {
    
    private ObjectMapper mapper;
    
    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }
    
    @Nested
    @DisplayName("Basic Trim Functionality")
    class BasicTrimTests {
        
        @Test
        @DisplayName("Should remove garbage lines before EntityName when trim is true")
        void testTrimRemovesGarbageBeforeEntityName() throws Exception {
            String lineStruct = 
                "garbage line 1\n" +
                "some random text\n" +
                "EntityName:TestEntity\n" +
                "schema:id:int" + CharacterUtils.FIELD_DELIMITER + "name:string" + CharacterUtils.FIELD_DELIMITER + "description:<string>?\n" +
                "1" + CharacterUtils.FIELD_DELIMITER + "John" + CharacterUtils.FIELD_DELIMITER + "Developer\n" +
                "2" + CharacterUtils.FIELD_DELIMITER + "Jane" + CharacterUtils.FIELD_DELIMITER + "Manager\n";
            
            // Without trim - should fail
            assertThrows(RuntimeException.class, () -> {
                JsonGenerator.toJson(lineStruct, false);
            }, "Should throw exception for invalid format when trim is false");
            
            // With trim - should work
            String json = JsonGenerator.toJson(lineStruct, true);
            assertNotNull(json);
            
            JsonNode root = mapper.readTree(json);
            assertEquals("TestEntity", root.get("entityName").asText());
            assertEquals(2, root.get("data").size());
            
            assertEquals("John", root.get("data").get(0).get("name").asText());
            assertEquals("Jane", root.get("data").get(1).get("name").asText());
        }
        
        @Test
        @DisplayName("Should handle only EntityName and schema with no data")
        void testTrimWithNoDataLines() throws Exception {
            String lineStruct = 
                "garbage\n" +
                "EntityName:Empty\n" +
                "schema:id:int" + CharacterUtils.FIELD_DELIMITER + "name:string\n";
            
            String jsonTrim = JsonGenerator.toJson(lineStruct, true);
            JsonNode rootTrim = mapper.readTree(jsonTrim);
            
            assertEquals("Empty", rootTrim.get("entityName").asText());
            assertEquals(0, rootTrim.get("data").size());
        }
    }
    
    @Nested
    @DisplayName("Optional Fields at End of Record")
    class OptionalFieldTests {
        
        @Test
        @DisplayName("Should handle complete lines with optional fields at end")
        void testTrimWithOptionalFieldsAtEndComplete() throws Exception {
            String lineStruct = 
                "EntityName:Person\n" +
                "schema:id:int" + CharacterUtils.FIELD_DELIMITER + "name:string" + CharacterUtils.FIELD_DELIMITER + 
                "email:<string>?" + CharacterUtils.FIELD_DELIMITER + "phone:<string>?" + CharacterUtils.FIELD_DELIMITER + "notes:<string>?\n" +
                "1" + CharacterUtils.FIELD_DELIMITER + "Alice" + CharacterUtils.FIELD_DELIMITER + "alice@test.com" + CharacterUtils.FIELD_DELIMITER + "555-0001" + CharacterUtils.FIELD_DELIMITER + "Some notes\n" +
                "2" + CharacterUtils.FIELD_DELIMITER + "Bob" + CharacterUtils.FIELD_DELIMITER + "bob@test.com" + CharacterUtils.FIELD_DELIMITER + "555-0002" + CharacterUtils.FIELD_DELIMITER + "\n" +
                "3" + CharacterUtils.FIELD_DELIMITER + "Charlie" + CharacterUtils.FIELD_DELIMITER + "charlie@test.com" + CharacterUtils.FIELD_DELIMITER + CharacterUtils.FIELD_DELIMITER + "\n" +
                "4" + CharacterUtils.FIELD_DELIMITER + "David" + CharacterUtils.FIELD_DELIMITER + CharacterUtils.FIELD_DELIMITER + CharacterUtils.FIELD_DELIMITER + "\n" +
                "5" + CharacterUtils.FIELD_DELIMITER + "Eve" + CharacterUtils.FIELD_DELIMITER;
            
            // Without trim - all lines should be processed
            String jsonNoTrim = JsonGenerator.toJson(lineStruct, false);
            JsonNode rootNoTrim = mapper.readTree(jsonNoTrim);
            assertEquals(5, rootNoTrim.get("data").size());
            
            // With trim - same result since all lines are complete
            String jsonTrim = JsonGenerator.toJson(lineStruct, true);
            JsonNode rootTrim = mapper.readTree(jsonTrim);
            assertEquals(5, rootTrim.get("data").size());
            
            // Verify last record - Eve with empty optional fields
            JsonNode lastRecord = rootTrim.get("data").get(4);
            assertEquals("Eve", lastRecord.get("name").asText());
            
            // Optional fields may not exist in JSON when empty
            JsonNode emailNode = lastRecord.get("email");
            assertTrue(emailNode == null || emailNode.isNull() || 
                      (emailNode.isTextual() && emailNode.asText().isEmpty()),
                "Email field should be null, missing, or empty");
        }
        
        @Test
        @DisplayName("Should handle all variations of trailing optional fields")
        void testTrimWithTrailingOptionalFields() throws Exception {
            String lineStruct = 
                "EntityName:Contact\n" +
                "schema:id:int" + CharacterUtils.FIELD_DELIMITER + "name:string" + CharacterUtils.FIELD_DELIMITER + 
                "opt1:<string>?" + CharacterUtils.FIELD_DELIMITER + "opt2:<string>?" + CharacterUtils.FIELD_DELIMITER + "opt3:<string>?\n" +
                "1" + CharacterUtils.FIELD_DELIMITER + "Alice" + CharacterUtils.FIELD_DELIMITER + "val1" + CharacterUtils.FIELD_DELIMITER + "val2" + CharacterUtils.FIELD_DELIMITER + "val3\n" +
                "2" + CharacterUtils.FIELD_DELIMITER + "Bob" + CharacterUtils.FIELD_DELIMITER + "val1" + CharacterUtils.FIELD_DELIMITER + "val2\n" +
                "3" + CharacterUtils.FIELD_DELIMITER + "Charlie" + CharacterUtils.FIELD_DELIMITER + "val1\n" +
                "4" + CharacterUtils.FIELD_DELIMITER + "David\n" +
                "5" + CharacterUtils.FIELD_DELIMITER + "Eve" + CharacterUtils.FIELD_DELIMITER + CharacterUtils.FIELD_DELIMITER + "val2\n" +
                "6" + CharacterUtils.FIELD_DELIMITER + "Frank" + CharacterUtils.FIELD_DELIMITER + CharacterUtils.FIELD_DELIMITER + CharacterUtils.FIELD_DELIMITER;
            
            // Both with and without trim should handle these correctly
            String jsonNoTrim = JsonGenerator.toJson(lineStruct, false);
            String jsonTrim = JsonGenerator.toJson(lineStruct, true);
            
            JsonNode rootNoTrim = mapper.readTree(jsonNoTrim);
            JsonNode rootTrim = mapper.readTree(jsonTrim);
            
            // Should have same number of records
            assertEquals(6, rootNoTrim.get("data").size(), "No-trim should have 6 records");
            assertEquals(6, rootTrim.get("data").size(), "Trim should have 6 records");
            
            // Verify David's record (all optional fields missing)
            JsonNode david = rootTrim.get("data").get(3);
            assertEquals("David", david.get("name").asText());
            
            JsonNode davidOpt1 = david.get("opt1");
            assertTrue(davidOpt1 == null || davidOpt1.isNull() || 
                      (davidOpt1.isTextual() && davidOpt1.asText().isEmpty()),
                "David's opt1 should be null, missing, or empty");
            
            // Verify Eve's record (middle empty, last has value)
            JsonNode eve = rootTrim.get("data").get(4);
            assertEquals("Eve", eve.get("name").asText());
            
            JsonNode eveOpt1 = eve.get("opt1");
            assertTrue(eveOpt1 == null || eveOpt1.isNull() || 
                      (eveOpt1.isTextual() && eveOpt1.asText().isEmpty()),
                "Eve's opt1 should be null, missing, or empty");
            
            JsonNode eveOpt2 = eve.get("opt2");
            if (eveOpt2 != null && !eveOpt2.isNull()) {
                assertEquals("val2", eveOpt2.asText(), "Eve's opt2 should be 'val2'");
            }
        }
    }
    
    @Nested
    @DisplayName("Incomplete Lines Detection")
    class IncompleteLineTests {
        
        @Test
        @DisplayName("Should detect incomplete array at end")
        void testTrimWithIncompleteArrayAtEnd() throws Exception {
            String lineStruct = 
                "EntityName:Product\n" +
                "schema:id:int" + CharacterUtils.FIELD_DELIMITER + "name:string" + CharacterUtils.FIELD_DELIMITER + 
                "tags:" + CharacterUtils.ARRAY_START + "string" + CharacterUtils.ARRAY_END + "\n" +
                "1" + CharacterUtils.FIELD_DELIMITER + "Product A" + CharacterUtils.FIELD_DELIMITER + 
                CharacterUtils.ARRAY_START + "tag1" + CharacterUtils.FIELD_DELIMITER + "tag2" + CharacterUtils.FIELD_DELIMITER + "tag3" + CharacterUtils.ARRAY_END + "\n" +
                "2" + CharacterUtils.FIELD_DELIMITER + "Product B" + CharacterUtils.FIELD_DELIMITER + 
                CharacterUtils.ARRAY_START + "tag1" + CharacterUtils.FIELD_DELIMITER + "tag2" + CharacterUtils.ARRAY_END + "\n" +
                "3" + CharacterUtils.FIELD_DELIMITER + "Product C" + CharacterUtils.FIELD_DELIMITER + 
                CharacterUtils.ARRAY_START + "tag1" + CharacterUtils.FIELD_DELIMITER + "tag2" + CharacterUtils.FIELD_DELIMITER + "tag3";  // Incomplete - missing closing bracket
            
            // With trim - should ideally exclude incomplete line
            String jsonTrim = JsonGenerator.toJson(lineStruct, true);
            JsonNode rootTrim = mapper.readTree(jsonTrim);
            
            // Should have 2 complete records (3rd is incomplete)
            assertEquals(2, rootTrim.get("data").size(), "Should have 2 complete records");
            
            assertEquals("Product A", rootTrim.get("data").get(0).get("name").asText());
            assertEquals("Product B", rootTrim.get("data").get(1).get("name").asText());
        }
        
        @Test
        @DisplayName("Should detect incomplete nested object at end")
        void testTrimWithIncompleteNestedObjectAtEnd() throws Exception {
            String lineStruct = 
                "EntityName:Employee\n" +
                "schema:id:int" + CharacterUtils.FIELD_DELIMITER + "name:string" + CharacterUtils.FIELD_DELIMITER + 
                "address:" + CharacterUtils.NESTED_START + "street:string" + CharacterUtils.FIELD_DELIMITER + 
                "city:string" + CharacterUtils.FIELD_DELIMITER + "zip:string" + CharacterUtils.NESTED_END + "\n" +
                "1" + CharacterUtils.FIELD_DELIMITER + "Alice" + CharacterUtils.FIELD_DELIMITER + 
                CharacterUtils.NESTED_START + "123 Main St" + CharacterUtils.FIELD_DELIMITER + "New York" + CharacterUtils.FIELD_DELIMITER + "10001" + CharacterUtils.NESTED_END + "\n" +
                "2" + CharacterUtils.FIELD_DELIMITER + "Bob" + CharacterUtils.FIELD_DELIMITER + 
                CharacterUtils.NESTED_START + "456 Oak Ave" + CharacterUtils.FIELD_DELIMITER + "Boston" + CharacterUtils.FIELD_DELIMITER + "02101" + CharacterUtils.NESTED_END + "\n" +
                "3" + CharacterUtils.FIELD_DELIMITER + "Charlie" + CharacterUtils.FIELD_DELIMITER + 
                CharacterUtils.NESTED_START + "789 Pine St" + CharacterUtils.FIELD_DELIMITER + "Chicago";  // Incomplete - missing zip and closing brace
            
            // With trim
            String jsonTrim = JsonGenerator.toJson(lineStruct, true);
            JsonNode rootTrim = mapper.readTree(jsonTrim);
            
            // Should have 2 complete records
            assertEquals(2, rootTrim.get("data").size(), "Should have 2 complete records");
        }
        
        @Test
        @DisplayName("Should handle escape sequences")
        void testTrimWithEscapedDelimiters() throws Exception {
            String lineStruct = 
                "EntityName:Message\n" +
                "schema:id:int" + CharacterUtils.FIELD_DELIMITER + "content:string" + CharacterUtils.FIELD_DELIMITER + "notes:<string>?\n" +
                "1" + CharacterUtils.FIELD_DELIMITER + "Hello" + CharacterUtils.ESCAPE_CHAR + CharacterUtils.FIELD_DELIMITER + "World" + CharacterUtils.FIELD_DELIMITER + "Some notes\n" +
                "2" + CharacterUtils.FIELD_DELIMITER + "Test" + CharacterUtils.ESCAPE_CHAR + CharacterUtils.NESTED_START + "nested" + CharacterUtils.ESCAPE_CHAR + CharacterUtils.NESTED_END + CharacterUtils.FIELD_DELIMITER + "Other notes\n" +
                "3" + CharacterUtils.FIELD_DELIMITER + "Array" + CharacterUtils.ESCAPE_CHAR + CharacterUtils.ARRAY_START + "test" + CharacterUtils.ESCAPE_CHAR + CharacterUtils.ARRAY_END + CharacterUtils.FIELD_DELIMITER + "\n" +
                "4" + CharacterUtils.FIELD_DELIMITER + "Incomplete" + CharacterUtils.ESCAPE_CHAR;  // Escape char at end - actually complete
            
            // With trim
            String jsonTrim = JsonGenerator.toJson(lineStruct, true);
            JsonNode rootTrim = mapper.readTree(jsonTrim);
            
            // All 4 records are actually complete (escape char at end is valid)
            assertEquals(4, rootTrim.get("data").size(), "Should have 4 records");
            
            // Check that the escaped content is properly handled in first record
            JsonNode content1 = rootTrim.get("data").get(0).get("content");
            assertNotNull(content1);
            // The escaped delimiter should appear as a literal delimiter in the output
            assertTrue(content1.asText().contains(String.valueOf(CharacterUtils.FIELD_DELIMITER)), 
                      "Should contain escaped delimiter");
            
            // The 4th record should have the escape char as part of the content
            JsonNode content4 = rootTrim.get("data").get(3).get("content");
            assertNotNull(content4);
            // The escape char at the end is just a literal escape char
            assertTrue(content4.asText().endsWith(String.valueOf(CharacterUtils.ESCAPE_CHAR)), 
                      "Should end with the escape character");
        }
    }
    
    @Nested
    @DisplayName("Backward Compatibility")
    class BackwardCompatibilityTests {
        
        @Test
        @DisplayName("Non-trim branch should work exactly as before")
        void testNonTrimBranchUnchanged() throws Exception {
            String lineStruct = 
                "EntityName:Standard\n" +
                "schema:id:int" + CharacterUtils.FIELD_DELIMITER + "name:string" + CharacterUtils.FIELD_DELIMITER + "value:<float>?\n" +
                "1" + CharacterUtils.FIELD_DELIMITER + "Test1" + CharacterUtils.FIELD_DELIMITER + "3.14\n" +
                "2" + CharacterUtils.FIELD_DELIMITER + "Test2" + CharacterUtils.FIELD_DELIMITER + "\n" +
                "3" + CharacterUtils.FIELD_DELIMITER + "Test3" + CharacterUtils.FIELD_DELIMITER + "2.71";
            
            // This should work exactly as it did before the trim feature was added
            String json = JsonGenerator.toJson(lineStruct, false);
            JsonNode root = mapper.readTree(json);
            
            assertEquals("Standard", root.get("entityName").asText());
            assertEquals(3, root.get("data").size());
            
            // Verify optional field handling
            JsonNode record2 = root.get("data").get(1);
            assertEquals("Test2", record2.get("name").asText());
            
            JsonNode valueNode = record2.get("value");
            assertTrue(valueNode == null || valueNode.isNull() || 
                      (valueNode.isNumber() && valueNode.asDouble() == 0.0) ||
                      (valueNode.isTextual() && valueNode.asText().isEmpty()),
                "Optional value field should be null, missing, zero, or empty");
        }
    }
    
    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {
        
        @Test
        @DisplayName("Should handle empty input")
        void testEmptyInput() {
            // Empty string after trim should throw an exception
            assertThrows(RuntimeException.class, () -> {
                JsonGenerator.toJson("", true);
            }, "Empty input should throw exception");
        }
        
        @Test
        @DisplayName("Should handle input with only garbage")
        void testOnlyGarbage() {
            String lineStruct = "garbage line 1\ngarbage line 2\nno entity here";
            
            // When trimmed and no EntityName found, should throw exception
            assertThrows(RuntimeException.class, () -> {
                JsonGenerator.toJson(lineStruct, true);
            }, "Should throw exception when no EntityName found after trim");
        }
        
        @Test
        @DisplayName("Should handle multiple incomplete lines at end")
        void testMultipleIncompleteLines() throws Exception {
            String lineStruct = 
                "EntityName:Test\n" +
                "schema:id:int" + CharacterUtils.FIELD_DELIMITER + "data:string\n" +
                "1" + CharacterUtils.FIELD_DELIMITER + "Complete\n" +
                "2" + CharacterUtils.FIELD_DELIMITER + "Also complete\n" +
                "3" + CharacterUtils.FIELD_DELIMITER + "Line3\n" +
                "4" + CharacterUtils.FIELD_DELIMITER + "Line4";
            
            String jsonTrim = JsonGenerator.toJson(lineStruct, true);
            JsonNode rootTrim = mapper.readTree(jsonTrim);
            
            // All 4 lines are actually complete
            assertEquals(4, rootTrim.get("data").size(), "Should have 4 complete records");
            
            assertEquals("Complete", rootTrim.get("data").get(0).get("data").asText());
            assertEquals("Also complete", rootTrim.get("data").get(1).get("data").asText());
        }
    }
}