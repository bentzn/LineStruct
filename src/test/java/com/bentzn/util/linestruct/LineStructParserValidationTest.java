package com.bentzn.util.linestruct;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import com.bentzn.util.linestruct.LineStructParser.DataType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

 class LineStructParserValidationTest {
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() throws Exception {
        // Create a test linestruct_description.txt file for getPrompt() testing
        // Note: In real usage, this would be in src/main/resources
        File descFile = tempDir.resolve("linestruct_description.txt").toFile();
        try (FileWriter writer = new FileWriter(descFile)) {
            writer.write("LineStruct Format Description\n");
            writer.write("A custom data format with special delimiters");
        }
    }
    
    // ===== getDataType() Tests =====
    
    @Test
    void testGetDataType_ValidJson() {
        String json = """
            {
                "name": "John",
                "age": 30,
                "active": true
            }
            """;
        
        assertEquals(DataType.JSON, LineStructParser.getDataType(json));
        
        String jsonArray = "[1, 2, 3, 4]";
        assertEquals(DataType.JSON, LineStructParser.getDataType(jsonArray));
    }
    
    @Test
    void testGetDataType_ValidLineStruct() {
        String lineStruct = """
            EntityName:Person
            schema:id:int¦name:string¦age:int
            1¦John Doe¦30
            2¦Jane Smith¦25
            """;
        
        assertEquals(DataType.LINESTRUCT, LineStructParser.getDataType(lineStruct));
    }
    
    @Test
    void testGetDataType_Other() {
        assertEquals(DataType.OTHER, LineStructParser.getDataType("random text"));
        assertEquals(DataType.OTHER, LineStructParser.getDataType(""));
        assertEquals(DataType.OTHER, LineStructParser.getDataType(null));
        assertEquals(DataType.OTHER, LineStructParser.getDataType("not json {invalid}"));
        assertEquals(DataType.OTHER, LineStructParser.getDataType("EntityName:Test\nno schema"));
    }
    
    @Test
    void testGetDataType_InvalidJsonButLooksLikeJson() {
        String invalidJson = "{invalid json: no quotes}";
        assertEquals(DataType.OTHER, LineStructParser.getDataType(invalidJson));
    }
    
    // ===== isValidJson() Tests =====
    
    @Test
    void testIsValidJson_Valid() {
        assertTrue(LineStructParser.isValidJson("{}"));
        assertTrue(LineStructParser.isValidJson("[]"));
        assertTrue(LineStructParser.isValidJson("{\"key\": \"value\"}"));
        assertTrue(LineStructParser.isValidJson("[1, 2, 3]"));
        assertTrue(LineStructParser.isValidJson("{\"nested\": {\"key\": \"value\"}}"));
    }
    
    @Test
    void testIsValidJson_Invalid() {
        assertFalse(LineStructParser.isValidJson("{invalid}"));
        assertFalse(LineStructParser.isValidJson("not json"));
        assertFalse(LineStructParser.isValidJson(""));
        assertFalse(LineStructParser.isValidJson(null));
        assertFalse(LineStructParser.isValidJson("{\"key\": }"));
        assertFalse(LineStructParser.isValidJson("[1, 2,]"));
    }
    
    // ===== isValidLineStruct() Tests =====
    
    @Test
    void testIsValidLineStruct_SimpleValid() {
        String valid = """
            EntityName:Person
            schema:id:int¦name:string¦age:int¦active:bool
            1¦John Doe¦30¦true
            2¦Jane Smith¦25¦false
            """;
        
        assertTrue(LineStructParser.isValidLineStruct(valid));
    }
    
    @Test
    void testIsValidLineStruct_WithOptionalFields() {
        String valid = """
            EntityName:Product
            schema:id:int¦name:string¦description:<string>?¦price:float
            1¦Laptop¦High-performance laptop¦999.99
            2¦Mouse¦¦29.99
            """;
        
        assertTrue(LineStructParser.isValidLineStruct(valid));
    }
    
    @Test
    void testIsValidLineStruct_WithNestedObjects() {
        String valid = """
            EntityName:Employee
            schema:id:int¦name:string¦address:‹street:string¦city:string¦zip:string›
            1¦John Doe¦‹123 Main St¦New York¦10001›
            2¦Jane Smith¦‹456 Oak Ave¦Los Angeles¦90001›
            """;
        
        assertTrue(LineStructParser.isValidLineStruct(valid));
    }
    
    @Test
    void testIsValidLineStruct_WithArrays() {
        String valid = """
            EntityName:Student
            schema:id:int¦name:string¦grades:«int»¦subjects:«string»
            1¦Alice¦«90¦85¦92»¦«Math¦Science¦English»
            2¦Bob¦«88¦91¦87»¦«History¦Math¦Art»
            """;
        
        assertTrue(LineStructParser.isValidLineStruct(valid));
    }
    
    @Test
    void testIsValidLineStruct_WithEnums() {
        String valid = """
            EntityName:Task
            schema:id:int¦title:string¦status:{TODO|IN_PROGRESS|DONE}¦priority:{LOW|MEDIUM|HIGH}
            1¦Fix bug¦TODO¦HIGH
            2¦Write tests¦IN_PROGRESS¦MEDIUM
            """;
        
        assertTrue(LineStructParser.isValidLineStruct(valid));
    }
    
    @Test
    void testIsValidLineStruct_InvalidSchema() {
        String invalid = """
            EntityName:Person
            schema:invalid schema format
            1¦John Doe¦30
            """;
        
        assertFalse(LineStructParser.isValidLineStruct(invalid));
    }
    
    @Test
    void testIsValidLineStruct_DataDoesNotMatchSchema() {
        String invalid = """
            EntityName:Person
            schema:id:int¦name:string¦age:int
            1¦John Doe¦not_a_number
            """;
        
        assertFalse(LineStructParser.isValidLineStruct(invalid));
    }
    
    @Test
    void testIsValidLineStruct_MissingRequiredField() {
        String invalid = """
            EntityName:Person
            schema:id:int¦name:string¦age:int
            1¦John Doe
            """;
        
        assertFalse(LineStructParser.isValidLineStruct(invalid));
    }
    
    @Test
    void testIsValidLineStruct_InvalidEnumValue() {
        String invalid = """
            EntityName:Task
            schema:id:int¦status:{TODO|DONE}
            1¦INVALID_STATUS
            """;
        
        assertFalse(LineStructParser.isValidLineStruct(invalid));
    }
    
    @Test
    void testIsValidLineStruct_EscapedCharacters() {
        String valid = """
            EntityName:Message
            schema:id:int¦content:string¦tags:«string»
            1¦This has ⁊¦ pipe and ⁊‹ bracket¦«tag⁊¦1¦tag⁊›2»
            """;
        
        assertTrue(LineStructParser.isValidLineStruct(valid));
    }
    
    @Test
    void testIsValidLineStruct_EmptyDataLines() {
        String valid = """
            EntityName:Person
            schema:id:int¦name:string
            1¦John
            
            2¦Jane
            """;
        
        assertTrue(LineStructParser.isValidLineStruct(valid));
    }
    
    @Test
    void testIsValidLineStruct_MinimumRequirements() {
        // Less than 2 lines
        assertFalse(LineStructParser.isValidLineStruct("EntityName:Test"));
        assertTrue(LineStructParser.isValidLineStruct("EntityName:Test\nschema:id:int"));
        
        // Missing EntityName
        assertFalse(LineStructParser.isValidLineStruct("NotEntity:Test\nschema:id:int\n1"));
        
        // Missing schema
        assertFalse(LineStructParser.isValidLineStruct("EntityName:Test\nnotschema:id:int\n1"));
    }
    
    // ===== isValid(yaml, lineStruct) Tests - Modified for String return =====
    
    @Test
    void testIsValid_SimpleYamlSchema() {
        String yaml = """
            id: int
            name: string
            age: int
            active: bool
            """;
        
        String lineStruct = """
            EntityName:Person
            schema:id:int¦name:string¦age:int¦active:bool
            1¦John Doe¦30¦true
            """;
        
        assertNull(LineStructParser.isValid(yaml, lineStruct));
    }
    
    @Test
    void testIsValid_YamlWithOptionalFields() {
        String yaml = """
            id: int
            name: string
            email: string
            phone: string
            """;
        
        String lineStruct = """
            EntityName:Contact
            schema:id:int¦name:string¦email:string¦phone:string¦notes:<string>?
            1¦John¦john@email.com¦555-1234¦Some notes
            """;
        
        // LineStruct has extra optional field 'notes' - should be valid
        assertNull(LineStructParser.isValid(yaml, lineStruct));
    }
    
    @Test
    void testIsValid_YamlWithTypeAliases() {
        String yaml = """
            id: integer
            name: str
            price: float
            active: boolean
            """;
        
        String lineStruct = """
            EntityName:Product
            schema:id:int¦name:string¦price:float¦active:bool
            1¦Product A¦99.99¦true
            """;
        
        assertNull(LineStructParser.isValid(yaml, lineStruct));
    }
    
    @Test
    void testIsValid_YamlWithComplexTypes() {
        String yaml = """
            id: int
            tags: array
            metadata: object
            status: enum
            """;
        
        String lineStruct = """
            EntityName:Item
            schema:id:int¦tags:«string»¦metadata:‹key:string¦value:string›¦status:{ACTIVE|INACTIVE}
            1¦«tag1¦tag2»¦‹color¦blue›¦ACTIVE
            """;
        
        assertNull(LineStructParser.isValid(yaml, lineStruct));
    }
    
    @Test
    void testIsValid_YamlStructuredFormat() {
        String yaml = """
            fields:
              id:
                type: int
                required: true
              name:
                type: string
                required: true
              description:
                type: string
                optional: true
            """;
        
        String lineStruct = """
            EntityName:Item
            schema:id:int¦name:string¦description:<string>?
            1¦Item A¦A description
            """;
        
        assertNull(LineStructParser.isValid(yaml, lineStruct));
    }
    
    @Test
    void testIsValid_MismatchedSchema() {
        String yaml = """
            id: int
            name: string
            price: float
            """;
        
        String lineStruct = """
            EntityName:Product
            schema:id:int¦name:string¦cost:float
            1¦Product¦99.99
            """;
        
        // Field name mismatch: 'price' vs 'cost'
        String result = LineStructParser.isValid(yaml, lineStruct);
        assertNotNull(result);
        assertTrue(result.contains("price") || result.contains("missing") || result.contains("Required"));
    }
    
    @Test
    void testIsValid_MissingRequiredField() {
        String yaml = """
            id: int
            name: string
            email: string
            """;
        
        String lineStruct = """
            EntityName:Person
            schema:id:int¦name:string
            1¦John Doe
            """;
        
        // Missing required field 'email'
        String result = LineStructParser.isValid(yaml, lineStruct);
        assertNotNull(result);
        assertTrue(result.contains("email") && (result.contains("missing") || result.contains("Required")));
    }
    
    @Test
    void testIsValid_IncompatibleTypes() {
        String yaml = """
            id: int
            name: string
            age: string
            """;
        
        String lineStruct = """
            EntityName:Person
            schema:id:int¦name:string¦age:int
            1¦John¦30
            """;
        
        // Type mismatch: age is string in YAML but int in LineStruct
        String result = LineStructParser.isValid(yaml, lineStruct);
        assertNotNull(result);
        assertTrue(result.contains("age") && (result.contains("mismatch") || result.contains("compatible")));
    }
    
    @Test
    void testIsValid_EmptyInputs() {
        String validLineStruct = """
            EntityName:Test
            schema:id:int
            1
            """;
        
        String result1 = LineStructParser.isValid(null, validLineStruct);
        assertNotNull(result1);
        assertTrue(result1.contains("YAML") && (result1.contains("null") || result1.contains("empty")));
        
        String result2 = LineStructParser.isValid("", validLineStruct);
        assertNotNull(result2);
        assertTrue(result2.contains("YAML") && result2.contains("empty"));
        
        String result3 = LineStructParser.isValid("id: int", null);
        assertNotNull(result3);
        assertTrue(result3.contains("LineStruct") && (result3.contains("null") || result3.contains("empty")));
        
        String result4 = LineStructParser.isValid("id: int", "");
        assertNotNull(result4);
        assertTrue(result4.contains("LineStruct") && result4.contains("empty"));
    }
    
    @Test
    void testIsValid_InvalidYaml() {
        String invalidYaml = """
            id: int
            name string  # Missing colon
            age: 
            """;
        
        String lineStruct = """
            EntityName:Person
            schema:id:intÂ¦name:string
            1Â¦John
            """;
        
        String result = LineStructParser.isValid(invalidYaml, lineStruct);
        assertNotNull(result);
        assertTrue(result.contains("YAML") && result.contains("parsing"));
    }
    
    @Test
    void testIsValid_InvalidLineStructFormat() {
        String yaml = """
            id: int
            name: string
            """;
        
        String invalidLineStruct = """
            EntityName:Person
            not_a_schema_line
            1¦John
            """;
        
        String result = LineStructParser.isValid(yaml, invalidLineStruct);
        assertNotNull(result);
        assertTrue(result.contains("schema") || result.contains("format"));
    }
    
    @Test
    void testIsValid_DataValidationErrors() {
        String yaml = """
            id: int
            name: string
            age: int
            """;
        
        String lineStruct = """
            EntityName:Person
            schema:id:int¦name:string¦age:int
            1¦John¦thirty
            """;
        
        String result = LineStructParser.isValid(yaml, lineStruct);
        assertNotNull(result);
        assertTrue(result.contains("Line") && result.contains("age") && result.contains("integer"));
    }
    
    @Test
    void testIsValid_DetailedErrorMessages() {
        String yaml = """
            id: int
            name: string
            email: string
            """;
        
        String lineStruct = """
            EntityName:Person
            schema:id:int¦name:string
            notAnInteger¦John
            """;
        
        String result = LineStructParser.isValid(yaml, lineStruct);
        assertNotNull(result);
        
        // Should contain line number and field information
        assertTrue(result.contains("Line") || result.contains("Field") || result.contains("missing"));
    }
    
    // ===== Integration Tests =====
    
    @Test
    void testCompleteWorkflow() {
        // Create LineStruct data
        String lineStruct = """
            EntityName:Employee
            schema:id:int¦name:string¦department:string¦salary:float¦active:bool
            1¦John Doe¦Engineering¦75000.50¦true
            2¦Jane Smith¦Marketing¦65000.00¦false
            """;
        
        // Verify it's detected as LineStruct
        assertEquals(DataType.LINESTRUCT, LineStructParser.getDataType(lineStruct));
        
        // Verify it's valid
        assertTrue(LineStructParser.isValidLineStruct(lineStruct));
        
        // Convert to JSON
        String json = LineStructParser.toJson(lineStruct);
        
        // Verify JSON is detected correctly
        assertEquals(DataType.JSON, LineStructParser.getDataType(json));
        assertTrue(LineStructParser.isValidJson(json));
        
        // Convert back to LineStruct
        String convertedBack = LineStructParser.fromJson(json);
        
        // Verify it's still valid LineStruct
        assertTrue(LineStructParser.isValidLineStruct(convertedBack));
        
        // Create YAML schema that matches
        String yaml = """
            id: int
            name: string
            department: string
            salary: float
            active: bool
            """;
        
        // Verify LineStruct validates against YAML - should return null (valid)
        assertNull(LineStructParser.isValid(yaml, lineStruct));
        assertNull(LineStructParser.isValid(yaml, convertedBack));
    }
    
    
    @Test
    void testInvalidLineInTheMiddle() throws Exception {
        // Test data
        String lineStruct = """
                EntityName:Person
                schema:id:int¦name:string¦age:int¦active:bool
                1¦John Doe¦30¦true
                2¦John Doe¦sdf¦false 
                3Doe¦sdf¦false
                4¦Jane Smith¦25¦false
                5¦Jane Smith
                """;
        // Line 2, 3 and 5 are invalid

        String json = LineStructParser.toJson(lineStruct);
        assertNotNull(json);
        System.out.println(json);
        // Verify JSON structure
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);
        assertEquals("Person", root.get("entityName").asText());
        assertEquals(2, root.get("data").size());

        // Convert back to LineStruct
        String result = LineStructParser.fromJson(json);
        assertNotNull(result);
        assertTrue(result.contains("EntityName:Person"));
        assertTrue(result.contains("John Doe"));
    }




}