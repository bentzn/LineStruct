package com.bentzn.util.linestruct;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class ClaudeSonnet4LineStructParserTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Test
    @DisplayName("Test basic primitive types conversion")
    void testBasicPrimitiveTypes() throws Exception {
        String lineStruct = """
            EntityName:Person
            schema:id:int¦name:string¦age:int¦active:bool¦height:float¦birthDate:date
            1¦John Doe¦30¦true¦5.9¦1993-05-15
            2¦Jane Smith¦25¦false¦5.4¦1998-12-03
            """;
        
        // Convert to JSON and verify structure
        String json = LineStructParser.toJson(lineStruct);
        JsonNode jsonNode = objectMapper.readTree(json);
        
        assertEquals("Person", jsonNode.get("entityName").asText());
        assertEquals(2, jsonNode.get("data").size());
        
        JsonNode firstPerson = jsonNode.get("data").get(0);
        assertEquals(1, firstPerson.get("id").asInt());
        assertEquals("John Doe", firstPerson.get("name").asText());
        assertEquals(30, firstPerson.get("age").asInt());
        assertTrue(firstPerson.get("active").asBoolean());
        assertEquals(5.9, firstPerson.get("height").asDouble(), 0.001);
        assertEquals("1993-05-15", firstPerson.get("birthDate").asText());
        
        // Convert back to LineStruct and verify round-trip
        String backToLineStruct = LineStructParser.fromJson(json);
        String backToJson = LineStructParser.toJson(backToLineStruct);
        JsonNode roundTripNode = objectMapper.readTree(backToJson);
        
        assertEquals(jsonNode, roundTripNode);
    }
    
    @Test
    @DisplayName("Test arrays of primitives")
    void testArraysOfPrimitives() throws Exception {
        String lineStruct = """
            EntityName:Student
            schema:id:int¦name:string¦grades:«int»¦subjects:«string»
            1¦Alice¦«85¦92¦78»¦«Math¦Science¦English»
            2¦Bob¦«90¦88»¦«History¦Art»
            3¦Charlie¦«»¦«»
            """;
        
        String json = LineStructParser.toJson(lineStruct);
        JsonNode jsonNode = objectMapper.readTree(json);
        
        assertEquals("Student", jsonNode.get("entityName").asText());
        
        JsonNode alice = jsonNode.get("data").get(0);
        assertEquals("Alice", alice.get("name").asText());
        assertEquals(3, alice.get("grades").size());
        assertEquals(85, alice.get("grades").get(0).asInt());
        assertEquals(92, alice.get("grades").get(1).asInt());
        assertEquals(78, alice.get("grades").get(2).asInt());
        assertEquals("Math", alice.get("subjects").get(0).asText());
        
        JsonNode charlie = jsonNode.get("data").get(2);
        assertEquals(0, charlie.get("grades").size());
        assertEquals(0, charlie.get("subjects").size());
        
        // Round-trip test
        String backToLineStruct = LineStructParser.fromJson(json);
        String backToJson = LineStructParser.toJson(backToLineStruct);
        JsonNode roundTripNode = objectMapper.readTree(backToJson);
        assertEquals(jsonNode, roundTripNode);
    }
    
    @Test
    @DisplayName("Test nested objects")
    void testNestedObjects() throws Exception {
        String lineStruct = """
            EntityName:Employee
            schema:id:int¦name:string¦address:‹street:string¦city:string¦zipCode:int›
            1¦John¦‹123 Main St¦Springfield¦12345›
            2¦Jane¦‹456 Oak Ave¦Shelbyville¦67890›
            """;
        
        String json = LineStructParser.toJson(lineStruct);
        JsonNode jsonNode = objectMapper.readTree(json);
        
        assertEquals("Employee", jsonNode.get("entityName").asText());
        
        JsonNode john = jsonNode.get("data").get(0);
        assertEquals("John", john.get("name").asText());
        JsonNode johnAddress = john.get("address");
        assertEquals("123 Main St", johnAddress.get("street").asText());
        assertEquals("Springfield", johnAddress.get("city").asText());
        assertEquals(12345, johnAddress.get("zipCode").asInt());
        
        // Round-trip test
        String backToLineStruct = LineStructParser.fromJson(json);
        String backToJson = LineStructParser.toJson(backToLineStruct);
        JsonNode roundTripNode = objectMapper.readTree(backToJson);
        assertEquals(jsonNode, roundTripNode);
    }
    
    @Test
    @DisplayName("Test array of objects")
    void testArrayOfObjects() throws Exception {
        String lineStruct = """
            EntityName:Company
            schema:id:int¦name:string¦contacts:«‹name:string¦phone:string›»
            1¦TechCorp¦«‹Alice¦123-456-7890›¦‹Bob¦987-654-3210›»
            2¦DesignCo¦«‹Charlie¦555-123-4567›»
            3¦StartupX¦«»
            """;
        
        String json = LineStructParser.toJson(lineStruct);
        JsonNode jsonNode = objectMapper.readTree(json);
        
        assertEquals("Company", jsonNode.get("entityName").asText());
        
        JsonNode techCorp = jsonNode.get("data").get(0);
        assertEquals("TechCorp", techCorp.get("name").asText());
        assertEquals(2, techCorp.get("contacts").size());
        assertEquals("Alice", techCorp.get("contacts").get(0).get("name").asText());
        assertEquals("123-456-7890", techCorp.get("contacts").get(0).get("phone").asText());
        
        JsonNode startupX = jsonNode.get("data").get(2);
        assertEquals(0, startupX.get("contacts").size());
        
        // Round-trip test
        String backToLineStruct = LineStructParser.fromJson(json);
        String backToJson = LineStructParser.toJson(backToLineStruct);
        JsonNode roundTripNode = objectMapper.readTree(backToJson);
        assertEquals(jsonNode, roundTripNode);
    }
    
    @Test
    @DisplayName("Test enums")
    void testEnums() throws Exception {
        String lineStruct = """
            EntityName:Task
            schema:id:int¦title:string¦status:{pending|in-progress|completed|cancelled}¦priority:{low|medium|high}
            1¦Fix bug¦in-progress¦high
            2¦Write docs¦pending¦medium
            3¦Review code¦completed¦low
            """;
        
        String json = LineStructParser.toJson(lineStruct);
        JsonNode jsonNode = objectMapper.readTree(json);
        
        assertEquals("Task", jsonNode.get("entityName").asText());
        
        JsonNode task1 = jsonNode.get("data").get(0);
        assertEquals("Fix bug", task1.get("title").asText());
        assertEquals("in-progress", task1.get("status").asText());
        assertEquals("high", task1.get("priority").asText());
        
        // Round-trip test
        String backToLineStruct = LineStructParser.fromJson(json);
        String backToJson = LineStructParser.toJson(backToLineStruct);
        JsonNode roundTripNode = objectMapper.readTree(backToJson);
        assertEquals(jsonNode, roundTripNode);
    }
    
    @Test
    @DisplayName("Test optional fields")
    void testOptionalFields() throws Exception {
        String lineStruct = """
            EntityName:User
            schema:id:int¦name:string¦email:<string>?¦profile:<‹bio:string¦age:int›>?
            1¦John¦john@example.com¦‹Software developer¦30›
            2¦Jane¦¦‹Designer¦25›
            3¦Bob¦bob@test.com¦
            4¦Alice¦¦
            """;
        
        String json = LineStructParser.toJson(lineStruct);
        JsonNode jsonNode = objectMapper.readTree(json);
        
        assertEquals("User", jsonNode.get("entityName").asText());
        
        JsonNode john = jsonNode.get("data").get(0);
        assertEquals("john@example.com", john.get("email").asText());
        assertFalse(john.get("profile").isNull());
        assertEquals("Software developer", john.get("profile").get("bio").asText());
        
        JsonNode jane = jsonNode.get("data").get(1);
        assertTrue(jane.get("email").isNull() || jane.get("email").asText().isEmpty());
        assertFalse(jane.get("profile").isNull());
        
        JsonNode bob = jsonNode.get("data").get(2);
        assertEquals("bob@test.com", bob.get("email").asText());
        assertTrue(bob.get("profile").isNull());
        
        JsonNode alice = jsonNode.get("data").get(3);
        assertTrue(alice.get("email").isNull() || alice.get("email").asText().isEmpty());
        assertTrue(alice.get("profile").isNull());
        
        // Round-trip test
        String backToLineStruct = LineStructParser.fromJson(json);
        String backToJson = LineStructParser.toJson(backToLineStruct);
        JsonNode roundTripNode = objectMapper.readTree(backToJson);
        assertEquals(jsonNode, roundTripNode);
    }
    
    @Test
    @DisplayName("Test escaped characters")
    void testEscapedCharacters() throws Exception {
        String lineStruct = """
            EntityName:Document
            schema:id:int¦title:string¦content:string
            1¦Special⁊¦Characters¦Content with ⁊‹brackets⁊› and ⁊«arrays⁊» and ⁊|pipes
            2¦Normal Title¦Normal content
            """;
        
        String json = LineStructParser.toJson(lineStruct);
        JsonNode jsonNode = objectMapper.readTree(json);
        
        JsonNode doc1 = jsonNode.get("data").get(0);
        assertEquals("Special¦Characters", doc1.get("title").asText());
        assertEquals("Content with ‹brackets› and «arrays» and |pipes", doc1.get("content").asText());
        
        // Round-trip test
        String backToLineStruct = LineStructParser.fromJson(json);
        String backToJson = LineStructParser.toJson(backToLineStruct);
        JsonNode roundTripNode = objectMapper.readTree(backToJson);
        assertEquals(jsonNode, roundTripNode);
    }
    
    @Test
    @DisplayName("Test datetime and date fields")
    void testDateTimeFields() throws Exception {
        String lineStruct = """
            EntityName:Event
            schema:id:int¦name:string¦date:date¦timestamp:datetime
            1¦Meeting¦2024-03-15¦2024-03-15T14:30:00Z
            2¦Conference¦2024-04-20¦2024-04-20T09:00:00Z
            """;
        
        String json = LineStructParser.toJson(lineStruct);
        JsonNode jsonNode = objectMapper.readTree(json);
        
        JsonNode meeting = jsonNode.get("data").get(0);
        assertEquals("2024-03-15", meeting.get("date").asText());
        assertEquals("2024-03-15T14:30:00Z", meeting.get("timestamp").asText());
        
        // Round-trip test
        String backToLineStruct = LineStructParser.fromJson(json);
        String backToJson = LineStructParser.toJson(backToLineStruct);
        JsonNode roundTripNode = objectMapper.readTree(backToJson);
        assertEquals(jsonNode, roundTripNode);
    }
    
    @Test
    @DisplayName("Test complex nested structure")
    void testComplexNestedStructure() throws Exception {
        String lineStruct = """
            EntityName:Order
            schema:id:int¦customer:‹name:string¦email:string›¦items:«‹product:string¦quantity:int¦price:float›»¦status:{pending|shipped|delivered}
            1¦‹John Doe¦john@example.com›¦«‹Laptop¦1¦999.99›¦‹Mouse¦2¦25.50›»¦pending
            2¦‹Jane Smith¦jane@test.com›¦«‹Book¦3¦15.99›»¦shipped
            """;
        
        String json = LineStructParser.toJson(lineStruct);
        JsonNode jsonNode = objectMapper.readTree(json);
        
        JsonNode order1 = jsonNode.get("data").get(0);
        assertEquals("John Doe", order1.get("customer").get("name").asText());
        assertEquals("john@example.com", order1.get("customer").get("email").asText());
        assertEquals(2, order1.get("items").size());
        assertEquals("Laptop", order1.get("items").get(0).get("product").asText());
        assertEquals(1, order1.get("items").get(0).get("quantity").asInt());
        assertEquals(999.99, order1.get("items").get(0).get("price").asDouble(), 0.001);
        assertEquals("pending", order1.get("status").asText());
        
        // Round-trip test
        String backToLineStruct = LineStructParser.fromJson(json);
        String backToJson = LineStructParser.toJson(backToLineStruct);
        JsonNode roundTripNode = objectMapper.readTree(backToJson);
        assertEquals(jsonNode, roundTripNode);
    }
    
    @Test
    @DisplayName("Test empty data")
    void testEmptyData() throws Exception {
        String lineStruct = """
            EntityName:EmptyEntity
            schema:id:int¦name:string
            """;
        
        String json = LineStructParser.toJson(lineStruct);
        JsonNode jsonNode = objectMapper.readTree(json);
        
        assertEquals("EmptyEntity", jsonNode.get("entityName").asText());
        assertEquals(0, jsonNode.get("data").size());
        
        // Round-trip test
        String backToLineStruct = LineStructParser.fromJson(json);
        String backToJson = LineStructParser.toJson(backToLineStruct);
        JsonNode roundTripNode = objectMapper.readTree(backToJson);
        assertEquals(jsonNode, roundTripNode);
    }
    
    @ParameterizedTest
    @DisplayName("Test invalid LineStruct formats")
    @ValueSource(strings = {
        "EntityName:Test",  // Missing schema line
        "InvalidFormat",  // No EntityName prefix
        "EntityName:Test\ninvalid_schema\n1¦data"  // Invalid schema format
    })
    void testInvalidLineStructFormats(String invalidLineStruct) {
        assertThrows(RuntimeException.class, () -> {
            LineStructParser.toJson(invalidLineStruct);
        });
    }
    
    @Test
    @DisplayName("Test invalid JSON format")
    void testInvalidJsonFormat() {
        String invalidJson = "{ invalid json }";
        assertThrows(RuntimeException.class, () -> {
            LineStructParser.fromJson(invalidJson);
        });
    }
    
    @Test
    @DisplayName("Test JSON without required fields")
    void testJsonWithoutRequiredFields() {
        String jsonWithoutEntityName = """
            {
                "data": [
                    {"id": 1, "name": "Test"}
                ]
            }
            """;
        
        String jsonWithoutData = """
            {
                "entityName": "Test"
            }
            """;
        
        assertThrows(RuntimeException.class, () -> {
            LineStructParser.fromJson(jsonWithoutEntityName);
        });
        
        assertThrows(RuntimeException.class, () -> {
            LineStructParser.fromJson(jsonWithoutData);
        });
    }
}