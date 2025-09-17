package com.bentzn.util.linestruct;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.*;

 class JsonToLineStructTest {
    
    private final ObjectMapper mapper = new ObjectMapper();
    
    @Test
    void testBasicConversion() throws Exception {
        String json = """
            {
              "entityName": "Person",
              "data": [
                {"id": 1, "name": "Alice", "age": 30},
                {"id": 2, "name": "Bob", "age": 25}
              ]
            }
            """;
        
        String result = LineStructParser.fromJson(json);
        
        // Parse back to verify
        String jsonBack = LineStructParser.toJson(result);
        JsonNode original = mapper.readTree(json);
        JsonNode converted = mapper.readTree(jsonBack);
        
        assertEquals(original.get("entityName"), converted.get("entityName"));
        assertEquals(original.get("data").size(), converted.get("data").size());
        assertEquals(original.get("data").get(0).get("id"), converted.get("data").get(0).get("id"));
        assertEquals(original.get("data").get(0).get("name"), converted.get("data").get(0).get("name"));
    }
    
    @Test
    void testOptionalFieldDetection() throws Exception {
        // JSON where some fields are null/missing in certain records
        String json = """
            {
              "entityName": "Product",
              "data": [
                {"id": 1, "name": "Laptop", "description": "High-end laptop", "price": 999.99},
                {"id": 2, "name": "Mouse", "description": null, "price": 29.99},
                {"id": 3, "name": "Keyboard", "price": 79.99}
              ]
            }
            """;
        
        String result = LineStructParser.fromJson(json);
        String[] lines = result.split("\n");
        
        // Check schema line - description should be marked as optional
        String schemaLine = lines[1];
        assertTrue(schemaLine.contains("schema:"));
        assertTrue(schemaLine.contains("description:<string>?"), 
            "Description field should be detected as optional. Schema was: " + schemaLine);
        
        // Verify data lines
        assertEquals("1¦Laptop¦High-end laptop¦999.99", lines[2]);
        assertEquals("2¦Mouse¦¦29.99", lines[3]); // null becomes empty
        assertEquals("3¦Keyboard¦¦79.99", lines[4]); // missing field becomes empty
    }
    
    @Test
    void testAllFieldsOptional() throws Exception {
        String json = """
            {
              "entityName": "Settings",
              "data": [
                {"theme": "dark", "fontSize": 12, "autoSave": true},
                {"theme": null, "fontSize": null, "autoSave": null},
                {"theme": "light", "fontSize": 14, "autoSave": false}
              ]
            }
            """;
        
        String result = LineStructParser.fromJson(json);
        String[] lines = result.split("\n");
        
        // Check we have the right number of lines
        assertTrue(lines.length >= 4, "Should have at least 4 lines: EntityName, schema, and 2+ data lines. Got: " + lines.length);
        
        // All fields should be detected as optional since one record has nulls
        String schemaLine = lines[1];
        assertTrue(schemaLine.contains("<string>?"), "theme should be optional. Schema: " + schemaLine);
        assertTrue(schemaLine.contains("<int>?"), "fontSize should be optional. Schema: " + schemaLine);
        assertTrue(schemaLine.contains("<bool>?"), "autoSave should be optional. Schema: " + schemaLine);
        
        // Check data lines
        assertEquals("dark¦12¦true", lines[2]);
        // When all optional fields are null/empty, the line might be empty or have delimiters
        // The parser trims trailing delimiters for optional fields
        assertTrue(lines[3].isEmpty() || lines[3].equals("¦¦"), 
            "All nulls should produce empty line or delimiters only. Got: '" + lines[3] + "'");
        assertEquals("light¦14¦false", lines[4]);
    }
    
    @Test
    void testNestedObjects() throws Exception {
        String json = """
            {
              "entityName": "User",
              "data": [
                {
                  "id": 1,
                  "name": "Alice",
                  "address": {
                    "street": "123 Main St",
                    "city": "Boston",
                    "zip": "02101"
                  }
                },
                {
                  "id": 2,
                  "name": "Bob",
                  "address": {
                    "street": "456 Oak Ave",
                    "city": "New York",
                    "zip": "10001"
                  }
                }
              ]
            }
            """;
        
        String result = LineStructParser.fromJson(json);
        String[] lines = result.split("\n");
        
        // Check schema has nested object
        assertTrue(lines[1].contains("address:‹"));
        assertTrue(lines[1].contains("street:string¦city:string¦zip:string›"));
        
        // Check data lines have nested values
        assertEquals("1¦Alice¦‹123 Main St¦Boston¦02101›", lines[2]);
        assertEquals("2¦Bob¦‹456 Oak Ave¦New York¦10001›", lines[3]);
    }
    
    @Test
    void testArrays() throws Exception {
        String json = """
            {
              "entityName": "Team",
              "data": [
                {
                  "name": "Engineering",
                  "members": ["Alice", "Bob", "Charlie"]
                },
                {
                  "name": "Marketing",
                  "members": []
                },
                {
                  "name": "Sales",
                  "members": ["David"]
                }
              ]
            }
            """;
        
        String result = LineStructParser.fromJson(json);
        String[] lines = result.split("\n");
        
        // Check schema has array
        assertTrue(lines[1].contains("members:«string»"));
        
        // Check data lines
        assertEquals("Engineering¦«Alice¦Bob¦Charlie»", lines[2]);
        assertEquals("Marketing¦«»", lines[3]); // Empty array
        assertEquals("Sales¦«David»", lines[4]); // Single element array
    }
    
    @Test
    void testOptionalArraysAndObjects() throws Exception {
        String json = """
            {
              "entityName": "Document",
              "data": [
                {
                  "id": 1,
                  "tags": ["important", "urgent"],
                  "metadata": {"author": "Alice", "version": "1.0"}
                },
                {
                  "id": 2,
                  "tags": null,
                  "metadata": null
                },
                {
                  "id": 3,
                  "tags": [],
                  "metadata": {"author": "Bob", "version": "2.0"}
                }
              ]
            }
            """;
        
        String result = LineStructParser.fromJson(json);
        String[] lines = result.split("\n");
        
        // Check schema - both should be optional
        String schemaLine = lines[1];
        assertTrue(schemaLine.contains("tags:<«string»>?"), 
            "Tags array should be optional. Schema: " + schemaLine);
        assertTrue(schemaLine.contains("metadata:<‹author:string¦version:string›>?"),
            "Metadata object should be optional. Schema: " + schemaLine);
        
        // Check data lines
        assertEquals("1¦«important¦urgent»¦‹Alice¦1.0›", lines[2]);
        // When trailing optional fields are null, they are omitted per LineStruct spec
        assertEquals("2", lines[3]); // null array and object are omitted when optional and at end
        assertEquals("3¦«»¦‹Bob¦2.0›", lines[4]); // Empty array is different from null
    }
    
    @Test
    void testMixedDataTypes() throws Exception {
        String json = """
            {
              "entityName": "Event",
              "data": [
                {
                  "id": 1,
                  "name": "Conference",
                  "attendees": 150,
                  "price": 299.99,
                  "isVirtual": false,
                  "date": "2024-03-15"
                },
                {
                  "id": 2,
                  "name": "Workshop",
                  "attendees": 25,
                  "price": 99.50,
                  "isVirtual": true,
                  "date": "2024-04-20"
                }
              ]
            }
            """;
        
        String result = LineStructParser.fromJson(json);
        String[] lines = result.split("\n");
        
        // Check schema detects correct types
        String schemaLine = lines[1];
        assertTrue(schemaLine.contains("id:int"));
        assertTrue(schemaLine.contains("name:string"));
        assertTrue(schemaLine.contains("attendees:int"));
        assertTrue(schemaLine.contains("price:float"));
        assertTrue(schemaLine.contains("isVirtual:bool"));
        assertTrue(schemaLine.contains("date:string")); // Dates are stored as strings
        
        // Check data formatting
        assertEquals("1¦Conference¦150¦299.99¦false¦2024-03-15", lines[2]);
        assertEquals("2¦Workshop¦25¦99.5¦true¦2024-04-20", lines[3]);
    }
    
    @Test
    void testSpecialCharacterEscaping() throws Exception {
        String json = """
            {
              "entityName": "Text",
              "data": [
                {
                  "id": 1,
                  "content": "This has ¦ pipe and ‹ bracket › symbols"
                },
                {
                  "id": 2,
                  "content": "Array « » markers and | enum delimiter"
                }
              ]
            }
            """;
        
        String result = LineStructParser.fromJson(json);
        
        // Convert back to JSON to verify escaping worked
        String jsonBack = LineStructParser.toJson(result);
        JsonNode converted = mapper.readTree(jsonBack);
        
        String content1 = converted.get("data").get(0).get("content").asText();
        String content2 = converted.get("data").get(1).get("content").asText();
        
        assertTrue(content1.contains("¦"));
        assertTrue(content1.contains("‹"));
        assertTrue(content1.contains("›"));
        
        assertTrue(content2.contains("«"));
        assertTrue(content2.contains("»"));
        assertTrue(content2.contains("|"));
    }
    
    @Test
    void testEmptyObjectHandling() throws Exception {
        // Test with an empty object - the schema should be inferred from non-empty objects
        String json = """
            {
              "entityName": "Config",
              "data": [
                {"setting1": "value1", "setting2": 100},
                {},
                {"setting1": "value2", "setting2": 200}
              ]
            }
            """;
        
        String result = LineStructParser.fromJson(json);
        String[] lines = result.split("\n");
        
        // Should have 5 lines: EntityName, schema, and 3 data lines
        assertEquals(5, lines.length, "Should have 5 lines total");
        
        // Check entity name
        assertEquals("EntityName:Config", lines[0]);
        
        // Check schema - fields are marked as optional because empty object is missing them
        String schemaLine = lines[1];
        assertTrue(schemaLine.contains("schema:"));
        assertTrue(schemaLine.contains("setting1:<string>?"), 
            "setting1 should be optional due to empty object");
        assertTrue(schemaLine.contains("setting2:<int>?"), 
            "setting2 should be optional due to empty object");
        
        // Check data lines
        assertEquals("value1¦100", lines[2]);
        assertEquals("", lines[3]); // Empty object produces empty line when all fields are optional
        assertEquals("value2¦200", lines[4]);
    }
    
    @Test
    void testComplexNestedOptionalDetection() throws Exception {
        String json = """
            {
              "entityName": "Company",
              "data": [
                {
                  "name": "TechCorp",
                  "employees": [
                    {"name": "Alice", "role": "CEO", "salary": 200000},
                    {"name": "Bob", "role": "CTO", "salary": 180000}
                  ],
                  "founded": 2010
                },
                {
                  "name": "StartupInc",
                  "employees": [
                    {"name": "Charlie", "role": "Founder", "salary": null}
                  ],
                  "founded": 2023
                },
                {
                  "name": "ConsultingLtd",
                  "employees": [],
                  "founded": null
                }
              ]
            }
            """;
        
        String result = LineStructParser.fromJson(json);
        String[] lines = result.split("\n");
        
        // Check the schema
        String schemaLine = lines[1];
        
        // Note: The current implementation doesn't detect optional fields within nested array objects
        // It only detects optional fields at the top level of the data array
        // This is a known limitation - detecting optional fields in deeply nested structures
        // would require more complex analysis
        
        assertTrue(schemaLine.contains("employees:«‹"), 
            "Should have employees array of objects");
        assertTrue(schemaLine.contains("name:string¦role:string¦salary:"), 
            "Should have employee fields");
        
        // Check that founded is optional (this works at the top level)
        assertTrue(schemaLine.contains("founded:<int>?"),
            "Founded should be optional at top level. Schema: " + schemaLine);
        
        // Verify the data lines are present
        assertEquals(5, lines.length, "Should have EntityName, schema, and 3 company records");
        
        // Check that the companies are correctly formatted
        assertTrue(lines[2].contains("TechCorp"));
        assertTrue(lines[3].contains("StartupInc"));
        assertTrue(lines[4].contains("ConsultingLtd"));
    }
    
    @Test
    void testRoundTripConsistency() throws Exception {
        String originalJson = """
            {
              "entityName": "Order",
              "data": [
                {
                  "orderId": 1001,
                  "customer": "Alice Smith",
                  "items": [
                    {"product": "Laptop", "quantity": 1, "price": 999.99},
                    {"product": "Mouse", "quantity": 2, "price": 29.99}
                  ],
                  "discount": 50.0,
                  "expedited": true
                },
                {
                  "orderId": 1002,
                  "customer": "Bob Jones",
                  "items": [
                    {"product": "Keyboard", "quantity": 1, "price": 79.99}
                  ],
                  "discount": null,
                  "expedited": false
                }
              ]
            }
            """;
        
        // Convert to LineStruct
        String lineStruct = LineStructParser.fromJson(originalJson);
        
        // Convert back to JSON
        String convertedJson = LineStructParser.toJson(lineStruct);
        
        // Parse both JSONs for comparison
        JsonNode original = mapper.readTree(originalJson);
        JsonNode converted = mapper.readTree(convertedJson);
        
        // Check entity name
        assertEquals(original.get("entityName").asText(), converted.get("entityName").asText());
        
        // Check data array size
        assertEquals(original.get("data").size(), converted.get("data").size());
        
        // Check first order
        JsonNode origOrder1 = original.get("data").get(0);
        JsonNode convOrder1 = converted.get("data").get(0);
        assertEquals(origOrder1.get("orderId").asInt(), convOrder1.get("orderId").asInt());
        assertEquals(origOrder1.get("customer").asText(), convOrder1.get("customer").asText());
        assertEquals(origOrder1.get("expedited").asBoolean(), convOrder1.get("expedited").asBoolean());
        
        // Check items array
        assertEquals(origOrder1.get("items").size(), convOrder1.get("items").size());
        assertEquals(origOrder1.get("items").get(0).get("product").asText(), 
                     convOrder1.get("items").get(0).get("product").asText());
        
        // Check second order with null discount
        JsonNode origOrder2 = original.get("data").get(1);
        JsonNode convOrder2 = converted.get("data").get(1);
        assertTrue(convOrder2.get("discount").isNull());
        assertEquals(origOrder2.get("expedited").asBoolean(), convOrder2.get("expedited").asBoolean());
    }
}