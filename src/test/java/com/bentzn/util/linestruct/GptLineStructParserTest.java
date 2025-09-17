package com.bentzn.util.linestruct;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

 class GptLineStructParserTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonNode parseJson(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON in test", e);
        }
    }

    @Test
    @DisplayName("Basic LineStruct ↔ JSON roundtrip")
    void testBasicRoundtrip() {
        String lineStruct = String.join("\n",
                "EntityName:Person",
                "schema:id:int¦name:string¦active:bool",
                "1¦John Doe¦true",
                "2¦Jane Smith¦false"
        );

        // Convert LS → JSON
        String json = LineStructParser.toJson(lineStruct);
        JsonNode root = parseJson(json);

        assertEquals("Person", root.get("entityName").asText());
        assertEquals(2, root.get("data").size());
        assertEquals("John Doe", root.get("data").get(0).get("name").asText());
        assertTrue(root.get("data").get(0).get("active").asBoolean());

        // Roundtrip JSON → LS
        String lsRoundtrip = LineStructParser.fromJson(json);
        assertEquals(normalize(lineStruct), normalize(lsRoundtrip));
    }

    @Test
    @DisplayName("Nested object is parsed correctly")
    void testNestedObject() {
        String lineStruct = String.join("\n",
                "EntityName:Order",
                "schema:id:int¦customer:‹name:string¦email:string›",
                "1¦‹Alice¦alice@example.com›"
        );

        JsonNode json = parseJson(LineStructParser.toJson(lineStruct));
        assertEquals("Order", json.get("entityName").asText());
        assertEquals("Alice", json.get("data").get(0).get("customer").get("name").asText());
        assertEquals("alice@example.com", json.get("data").get(0).get("customer").get("email").asText());
    }

    @Test
    @DisplayName("Array of primitives works")
    void testArrayOfPrimitives() {
        String lineStruct = String.join("\n",
                "EntityName:TagCollection",
                "schema:id:int¦tags:«string»",
                "1¦«red¦green¦blue»"
        );

        JsonNode json = parseJson(LineStructParser.toJson(lineStruct));
        JsonNode tags = json.get("data").get(0).get("tags");

        assertEquals(3, tags.size());
        assertEquals("red", tags.get(0).asText());
    }

    @Test
    @DisplayName("Enums and optionals work")
    void testEnumAndOptional() {
        String lineStruct = String.join("\n",
                "EntityName:Vehicle",
                "schema:id:int¦type:{car|bike|bus}¦owner:string?",
                "1¦car¦John",
                "2¦bike¦"
        );

        JsonNode json = parseJson(LineStructParser.toJson(lineStruct));
        JsonNode vehicle1 = json.get("data").get(0);
        JsonNode vehicle2 = json.get("data").get(1);

        assertEquals("car", vehicle1.get("type").asText());
        assertEquals("John", vehicle1.get("owner").asText());
        assertEquals("bike", vehicle2.get("type").asText());
        assertTrue(vehicle2.get("owner").isNull()); // optional empty → null
    }

    @Test
    @DisplayName("Array of objects works")
    void testArrayOfObjects() {
        String lineStruct = String.join("\n",
                "EntityName:Company",
                "schema:id:int¦contacts:«‹name:string¦phone:string›»",
                "1¦«‹Alice¦123›¦‹Bob¦456›»"
        );

        JsonNode json = parseJson(LineStructParser.toJson(lineStruct));
        JsonNode contacts = json.get("data").get(0).get("contacts");

        assertEquals(2, contacts.size());
        assertEquals("Alice", contacts.get(0).get("name").asText());
        assertEquals("456", contacts.get(1).get("phone").asText());
    }


    @Test
    @DisplayName("Invalid JSON input should throw")
    void testInvalidJson() {
        String invalidJson = "{ not-valid-json }";
        assertThrows(RuntimeException.class, () -> LineStructParser.fromJson(invalidJson));
    }

    private static String normalize(String s) {
        return s.replace("\r\n", "\n").trim();
    }
}
