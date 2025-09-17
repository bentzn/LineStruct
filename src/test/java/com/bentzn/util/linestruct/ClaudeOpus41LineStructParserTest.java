package com.bentzn.util.linestruct;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ClaudeOpus41LineStructParserTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("Test basic primitive types conversion")
    void testBasicPrimitiveTypes() throws Exception {
        String lineStruct = """
                EntityName:Person
                schema:id:int¦name:string¦age:int¦active:bool¦score:float
                1¦John Doe¦30¦true¦95.5
                2¦Jane Smith¦25¦false¦87.3
                """;

        // Convert to JSON
        String json = LineStructParser.toJson(lineStruct);
        assertNotNull(json);

        // Parse JSON to verify structure
        JsonNode root = mapper.readTree(json);
        assertEquals("Person", root.get("entityName").asText());
        
        JsonNode data = root.get("data");
        assertTrue(data.isArray());
        assertEquals(2, data.size());

        // Verify first record
        JsonNode first = data.get(0);
        assertEquals(1, first.get("id").asInt());
        assertEquals("John Doe", first.get("name").asText());
        assertEquals(30, first.get("age").asInt());
        assertTrue(first.get("active").asBoolean());
        assertEquals(95.5, first.get("score").asDouble(), 0.001);

        // Convert back to LineStruct
        String backToLineStruct = LineStructParser.fromJson(json);
        assertNotNull(backToLineStruct);
        
        // Verify round-trip by converting back to JSON again
        String jsonAgain = LineStructParser.toJson(backToLineStruct);
        JsonNode rootAgain = mapper.readTree(jsonAgain);
        assertEquals(root, rootAgain);
    }

    @Test
    @DisplayName("Test date and datetime types")
    void testDateTimeTypes() throws Exception {
        String lineStruct = """
                EntityName:Event
                schema:id:int¦name:string¦date:date¦timestamp:datetime
                1¦Conference¦2024-03-15¦2024-03-15T10:30:00Z
                2¦Meeting¦2024-04-20¦2024-04-20T14:45:30Z
                """;

        String json = LineStructParser.toJson(lineStruct);
        JsonNode root = mapper.readTree(json);
        
        JsonNode first = root.get("data").get(0);
        assertEquals("2024-03-15", first.get("date").asText());
        assertEquals("2024-03-15T10:30:00Z", first.get("timestamp").asText());

        // Round-trip test
        String backToLineStruct = LineStructParser.fromJson(json);
        String jsonAgain = LineStructParser.toJson(backToLineStruct);
        assertEquals(root, mapper.readTree(jsonAgain));
    }

    @Test
    @DisplayName("Test arrays of primitives")
    void testArraysOfPrimitives() throws Exception {
        String lineStruct = """
                EntityName:Student
                schema:id:int¦name:string¦grades:«int»¦subjects:«string»
                1¦Alice¦«90¦85¦92»¦«Math¦Science¦English»
                2¦Bob¦«78¦82¦88¦91»¦«History¦Art¦Music¦PE»
                3¦Charlie¦«»¦«»
                """;

        String json = LineStructParser.toJson(lineStruct);
        JsonNode root = mapper.readTree(json);
        
        JsonNode alice = root.get("data").get(0);
        assertTrue(alice.get("grades").isArray());
        assertEquals(3, alice.get("grades").size());
        assertEquals(90, alice.get("grades").get(0).asInt());
        
        assertTrue(alice.get("subjects").isArray());
        assertEquals("Math", alice.get("subjects").get(0).asText());

        // Test empty arrays
        JsonNode charlie = root.get("data").get(2);
        assertEquals(0, charlie.get("grades").size());
        assertEquals(0, charlie.get("subjects").size());

        // Round-trip test
        String backToLineStruct = LineStructParser.fromJson(json);
        String jsonAgain = LineStructParser.toJson(backToLineStruct);
        assertEquals(root, mapper.readTree(jsonAgain));
    }

    @Test
    @DisplayName("Test nested objects")
    void testNestedObjects() throws Exception {
        String lineStruct = """
                EntityName:Employee
                schema:id:int¦name:string¦address:‹street:string¦city:string¦zip:string›
                1¦John¦‹123 Main St¦New York¦10001›
                2¦Jane¦‹456 Oak Ave¦Los Angeles¦90001›
                """;

        String json = LineStructParser.toJson(lineStruct);
        JsonNode root = mapper.readTree(json);
        
        JsonNode john = root.get("data").get(0);
        JsonNode address = john.get("address");
        assertNotNull(address);
        assertEquals("123 Main St", address.get("street").asText());
        assertEquals("New York", address.get("city").asText());
        assertEquals("10001", address.get("zip").asText());

        // Round-trip test
        String backToLineStruct = LineStructParser.fromJson(json);
        String jsonAgain = LineStructParser.toJson(backToLineStruct);
        assertEquals(root, mapper.readTree(jsonAgain));
    }

    @Test
    @DisplayName("Test arrays of objects")
    void testArraysOfObjects() throws Exception {
        String lineStruct = """
                EntityName:Company
                schema:id:int¦name:string¦contacts:«‹name:string¦phone:string›»
                1¦TechCorp¦«‹Alice¦123-4567›¦‹Bob¦234-5678›»
                2¦DataInc¦«‹Charlie¦345-6789›»
                3¦EmptyCo¦«»
                """;

        String json = LineStructParser.toJson(lineStruct);
        JsonNode root = mapper.readTree(json);
        
        JsonNode techCorp = root.get("data").get(0);
        JsonNode contacts = techCorp.get("contacts");
        assertTrue(contacts.isArray());
        assertEquals(2, contacts.size());
        assertEquals("Alice", contacts.get(0).get("name").asText());
        assertEquals("123-4567", contacts.get(0).get("phone").asText());

        // Test empty array
        JsonNode emptyCo = root.get("data").get(2);
        assertEquals(0, emptyCo.get("contacts").size());

        // Round-trip test
        String backToLineStruct = LineStructParser.fromJson(json);
        String jsonAgain = LineStructParser.toJson(backToLineStruct);
        assertEquals(root, mapper.readTree(jsonAgain));
    }

    @Test
    @DisplayName("Test optional fields")
    void testOptionalFields() throws Exception {
        String lineStruct = """
                EntityName:User
                schema:id:int¦name:string¦email:string?¦phone:string?¦age:int?
                1¦Alice¦alice@example.com¦555-1234¦25
                2¦Bob¦¦555-5678¦
                3¦Charlie¦charlie@example.com¦¦30
                4¦David¦¦¦
                """;

        String json = LineStructParser.toJson(lineStruct);
        JsonNode root = mapper.readTree(json);
        
        // Alice has all fields
        JsonNode alice = root.get("data").get(0);
        assertEquals("alice@example.com", alice.get("email").asText());
        assertEquals("555-1234", alice.get("phone").asText());
        assertEquals(25, alice.get("age").asInt());

        // Bob has no email, no age
        JsonNode bob = root.get("data").get(1);
        assertTrue(bob.get("email").isNull() || bob.get("email").asText().isEmpty());
        assertEquals("555-5678", bob.get("phone").asText());
        assertTrue(bob.get("age").isNull() || bob.get("age").asText().isEmpty());

        // David has all optional fields empty
        JsonNode david = root.get("data").get(3);
        assertTrue(david.get("email").isNull() || david.get("email").asText().isEmpty());
        assertTrue(david.get("phone").isNull() || david.get("phone").asText().isEmpty());
        assertTrue(david.get("age").isNull() || david.get("age").asText().isEmpty());

        // Round-trip test
        String backToLineStruct = LineStructParser.fromJson(json);
        String jsonAgain = LineStructParser.toJson(backToLineStruct);
        assertEquals(root, mapper.readTree(jsonAgain));
    }

    @Test
    @DisplayName("Test optional nested objects and arrays")
    void testOptionalNestedStructures() throws Exception {
        String lineStruct = """
                EntityName:Product
                schema:id:int¦name:string¦details:‹desc:string¦price:float›?¦tags:«string»?
                1¦Widget¦‹A useful widget¦19.99›¦«new¦featured»
                2¦Gadget¦¦«sale»
                3¦Thing¦‹Just a thing¦5.00›¦
                4¦Item¦¦
                """;

        String json = LineStructParser.toJson(lineStruct);
        JsonNode root = mapper.readTree(json);
        
        // Widget has both optional fields
        JsonNode widget = root.get("data").get(0);
        assertNotNull(widget.get("details"));
        assertEquals("A useful widget", widget.get("details").get("desc").asText());
        assertEquals(2, widget.get("tags").size());

        // Gadget has no details but has tags
        JsonNode gadget = root.get("data").get(1);
        assertTrue(gadget.get("details").isNull() || gadget.get("details").size() == 0);
        assertEquals(1, gadget.get("tags").size());

        // Thing has details but no tags
        JsonNode thing = root.get("data").get(2);
        assertNotNull(thing.get("details"));
        assertTrue(thing.get("tags").isNull() || thing.get("tags").size() == 0);

        // Item has neither
        JsonNode item = root.get("data").get(3);
        assertTrue(item.get("details").isNull() || item.get("details").size() == 0);
        assertTrue(item.get("tags").isNull() || item.get("tags").size() == 0);

        // Round-trip test
        String backToLineStruct = LineStructParser.fromJson(json);
        String jsonAgain = LineStructParser.toJson(backToLineStruct);
        assertEquals(root, mapper.readTree(jsonAgain));
    }

    @Test
    @DisplayName("Test enum fields")
    void testEnumFields() throws Exception {
        String lineStruct = """
                EntityName:Task
                schema:id:int¦title:string¦status:{pending|in_progress|completed}¦priority:{low|medium|high}
                1¦Fix bug¦in_progress¦high
                2¦Add feature¦pending¦medium
                3¦Update docs¦completed¦low
                """;

        String json = LineStructParser.toJson(lineStruct);
        JsonNode root = mapper.readTree(json);
        
        JsonNode task1 = root.get("data").get(0);
        assertEquals("in_progress", task1.get("status").asText());
        assertEquals("high", task1.get("priority").asText());

        // Round-trip test
        String backToLineStruct = LineStructParser.fromJson(json);
        String jsonAgain = LineStructParser.toJson(backToLineStruct);
        assertEquals(root, mapper.readTree(jsonAgain));
    }

    @Test
    @DisplayName("Test escape sequences")
    void testEscapeSequences() throws Exception {
        String lineStruct = """
                EntityName:Message
                schema:id:int¦text:string¦code:string
                1¦Hello⁊¦World¦function⁊‹param⁊›
                2¦Array⁊«test⁊»¦Pipe⁊|symbol
                3¦Mixed⁊¦⁊‹⁊›⁊«⁊»⁊|¦All⁊¦escapes
                """;

        String json = LineStructParser.toJson(lineStruct);
        JsonNode root = mapper.readTree(json);
        
        JsonNode msg1 = root.get("data").get(0);
        assertEquals("Hello¦World", msg1.get("text").asText());
        assertEquals("function‹param›", msg1.get("code").asText());

        JsonNode msg2 = root.get("data").get(1);
        assertEquals("Array«test»", msg2.get("text").asText());
        assertEquals("Pipe|symbol", msg2.get("code").asText());

        JsonNode msg3 = root.get("data").get(2);
        assertEquals("Mixed¦‹›«»|", msg3.get("text").asText());
        assertEquals("All¦escapes", msg3.get("code").asText());

        // Round-trip test
        String backToLineStruct = LineStructParser.fromJson(json);
        String jsonAgain = LineStructParser.toJson(backToLineStruct);
        assertEquals(root, mapper.readTree(jsonAgain));
    }

    @Test
    @DisplayName("Test deeply nested structures")
    void testDeeplyNestedStructures() throws Exception {
        String lineStruct = """
                EntityName:Organization
                schema:id:int¦name:string¦departments:«‹name:string¦employees:«‹id:int¦name:string¦skills:«string»›»›»
                1¦TechCo¦«‹Engineering¦«‹101¦Alice¦«Java¦Python»›¦‹102¦Bob¦«JavaScript¦TypeScript¦React»›»›¦‹Marketing¦«‹201¦Charlie¦«SEO¦Content»›»›»
                """;

        String json = LineStructParser.toJson(lineStruct);
        JsonNode root = mapper.readTree(json);
        
        JsonNode org = root.get("data").get(0);
        JsonNode departments = org.get("departments");
        assertEquals(2, departments.size());

        // Check Engineering department
        JsonNode engineering = departments.get(0);
        assertEquals("Engineering", engineering.get("name").asText());
        JsonNode employees = engineering.get("employees");
        assertEquals(2, employees.size());

        // Check Alice
        JsonNode alice = employees.get(0);
        assertEquals(101, alice.get("id").asInt());
        assertEquals("Alice", alice.get("name").asText());
        JsonNode aliceSkills = alice.get("skills");
        assertEquals(2, aliceSkills.size());
        assertEquals("Java", aliceSkills.get(0).asText());
        assertEquals("Python", aliceSkills.get(1).asText());

        // Round-trip test
        String backToLineStruct = LineStructParser.fromJson(json);
        String jsonAgain = LineStructParser.toJson(backToLineStruct);
        assertEquals(root, mapper.readTree(jsonAgain));
    }

    @Test
    @DisplayName("Test empty data rows")
    void testEmptyDataRows() throws Exception {
        String lineStruct = """
                EntityName:Empty
                schema:id:int¦name:string
                """;

        String json = LineStructParser.toJson(lineStruct);
        JsonNode root = mapper.readTree(json);
        
        assertEquals("Empty", root.get("entityName").asText());
        JsonNode data = root.get("data");
        assertTrue(data.isArray());
        assertEquals(0, data.size());
    }

    @Test
    @DisplayName("Test field descriptions in schema")
    void testFieldDescriptions() throws Exception {
        String lineStruct = """
                EntityName:Product
                schema:id:int@desc="Product ID"¦name:string@desc="Product name"¦price:float@desc="Price in USD"
                1¦Laptop¦999.99
                2¦Mouse¦29.99
                """;

        String json = LineStructParser.toJson(lineStruct);
        JsonNode root = mapper.readTree(json);
        
        // Verify data is parsed correctly despite descriptions
        JsonNode laptop = root.get("data").get(0);
        assertEquals(1, laptop.get("id").asInt());
        assertEquals("Laptop", laptop.get("name").asText());
        assertEquals(999.99, laptop.get("price").asDouble(), 0.001);

        // Round-trip test
        String backToLineStruct = LineStructParser.fromJson(json);
        String jsonAgain = LineStructParser.toJson(backToLineStruct);
        assertEquals(root, mapper.readTree(jsonAgain));
    }

    @Test
    @DisplayName("Test special characters in strings")
    void testSpecialCharactersInStrings() throws Exception {
        String lineStruct = """
                EntityName:Text
                schema:id:int¦content:string
                1¦Hello, "world"!
                2¦Line 1\\nLine 2
                3¦Tab\there
                4¦Unicode: 你好 🌟
                """;

        String json = LineStructParser.toJson(lineStruct);
        JsonNode root = mapper.readTree(json);
        
        JsonNode text1 = root.get("data").get(0);
        assertEquals("Hello, \"world\"!", text1.get("content").asText());

        JsonNode text4 = root.get("data").get(3);
        assertEquals("Unicode: 你好 🌟", text4.get("content").asText());

        // Round-trip test
        String backToLineStruct = LineStructParser.fromJson(json);
        String jsonAgain = LineStructParser.toJson(backToLineStruct);
        assertEquals(root, mapper.readTree(jsonAgain));
    }

    @Test
    @DisplayName("Test boolean values case sensitivity")
    void testBooleanValues() throws Exception {
        String lineStruct = """
                EntityName:Flags
                schema:id:int¦flag1:bool¦flag2:bool
                1¦true¦false
                2¦false¦true
                """;

        String json = LineStructParser.toJson(lineStruct);
        JsonNode root = mapper.readTree(json);
        
        JsonNode row1 = root.get("data").get(0);
        assertTrue(row1.get("flag1").asBoolean());
        assertFalse(row1.get("flag2").asBoolean());

        JsonNode row2 = root.get("data").get(1);
        assertFalse(row2.get("flag1").asBoolean());
        assertTrue(row2.get("flag2").asBoolean());

        // Round-trip test
        String backToLineStruct = LineStructParser.fromJson(json);
        String jsonAgain = LineStructParser.toJson(backToLineStruct);
        assertEquals(root, mapper.readTree(jsonAgain));
    }

    @Test
    @DisplayName("Test mixed complex types")
    void testMixedComplexTypes() throws Exception {
        String lineStruct = """
                EntityName:ComplexEntity
                schema:id:int¦data:‹values:«int»¦meta:‹count:int¦active:bool›¦tags:«string»?›¦status:{draft|published|archived}?
                1¦‹«1¦2¦3»¦‹3¦true›¦«tag1¦tag2»›¦published
                2¦‹«»¦‹0¦false›¦›¦
                3¦‹«10¦20»¦‹2¦true›¦«important»›¦draft
                """;

        String json = LineStructParser.toJson(lineStruct);
        JsonNode root = mapper.readTree(json);
        
        // Check first entity
        JsonNode entity1 = root.get("data").get(0);
        JsonNode data1 = entity1.get("data");
        assertEquals(3, data1.get("values").size());
        assertEquals(3, data1.get("meta").get("count").asInt());
        assertTrue(data1.get("meta").get("active").asBoolean());
        assertEquals(2, data1.get("tags").size());
        assertEquals("published", entity1.get("status").asText());

        // Check second entity with empty values
        JsonNode entity2 = root.get("data").get(1);
        JsonNode data2 = entity2.get("data");
        assertEquals(0, data2.get("values").size());
        assertEquals(0, data2.get("meta").get("count").asInt());
        assertFalse(data2.get("meta").get("active").asBoolean());
        assertTrue(data2.get("tags").isNull() || data2.get("tags").size() == 0);
        assertTrue(entity2.get("status").isNull() || entity2.get("status").asText().isEmpty());

        // Round-trip test
        String backToLineStruct = LineStructParser.fromJson(json);
        String jsonAgain = LineStructParser.toJson(backToLineStruct);
        assertEquals(root, mapper.readTree(jsonAgain));
    }

    @Test
    @DisplayName("Test invalid format - should throw exception")
    void testInvalidFormat() {
        // Missing schema line
        String invalidLineStruct1 = """
                EntityName:Test
                1¦data
                """;
        assertThrows(RuntimeException.class, () -> LineStructParser.toJson(invalidLineStruct1));

        // Missing EntityName
        String invalidLineStruct2 = """
                schema:id:int¦name:string
                1¦test
                """;
        assertThrows(RuntimeException.class, () -> LineStructParser.toJson(invalidLineStruct2));

        // Invalid JSON for fromJson
        String invalidJson = "not a valid json";
        assertThrows(RuntimeException.class, () -> LineStructParser.fromJson(invalidJson));
    }

    @Test
    @DisplayName("Test fromJson with various input formats")
    void testFromJsonVariousFormats() throws Exception {
        // Test with pretty-printed JSON
        String prettyJson = """
                {
                    "entityName": "Person",
                    "data": [
                        {
                            "id": 1,
                            "name": "Alice",
                            "active": true
                        },
                        {
                            "id": 2,
                            "name": "Bob",
                            "active": false
                        }
                    ]
                }
                """;

        String lineStruct = LineStructParser.fromJson(prettyJson);
        assertNotNull(lineStruct);
        assertTrue(lineStruct.startsWith("EntityName:Person"));
        assertTrue(lineStruct.contains("schema:"));
        
        // Convert back and verify
        String json = LineStructParser.toJson(lineStruct);
        JsonNode original = mapper.readTree(prettyJson);
        JsonNode converted = mapper.readTree(json);
        assertEquals(original, converted);

        // Test with minified JSON
        String minifiedJson = "{\"entityName\":\"Person\",\"data\":[{\"id\":1,\"name\":\"Alice\",\"active\":true}]}";
        lineStruct = LineStructParser.fromJson(minifiedJson);
        assertNotNull(lineStruct);
        
        json = LineStructParser.toJson(lineStruct);
        original = mapper.readTree(minifiedJson);
        converted = mapper.readTree(json);
        assertEquals(original, converted);
    }
}