package com.bentzn.util.linestruct;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;

/**
 * Unit tests for LineStructParser.
 *
 * @author Grok/bentzn
 * @generated 2025-08-17T12:59:00Z
 */
 class Grok3LineStructParserTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testToJsonSimple() throws Exception {
        String input = """
            EntityName:Person
            schema:id:int¦name:string¦active:bool
            1¦John Doe¦true
            2¦Jane Smith¦false
            """;
        
        String json = LineStructParser.toJson(input);
        ObjectNode root = (ObjectNode) mapper.readTree(json);
        
        assertEquals("Person", root.get("entityName").asText());
        ArrayNode data = (ArrayNode) root.get("data");
        assertEquals(2, data.size());
        
        ObjectNode first = (ObjectNode) data.get(0);
        assertEquals(1, first.get("id").asInt());
        assertEquals("John Doe", first.get("name").asText());
        assertTrue(first.get("active").asBoolean());
        
        ObjectNode second = (ObjectNode) data.get(1);
        assertEquals(2, second.get("id").asInt());
        assertEquals("Jane Smith", second.get("name").asText());
        assertFalse(second.get("active").asBoolean());
    }

    @Test
    void testFromJsonSimple() throws Exception {
        String json = """
            {
                "entityName": "Person",
                "data": [
                    {"id": 1, "name": "John Doe", "active": true},
                    {"id": 2, "name": "Jane Smith", "active": false}
                ]
            }
            """;
        
        String result = LineStructParser.fromJson(json);
        String[] lines = result.split("\n");
        
        assertEquals("EntityName:Person", lines[0]);
        assertEquals("schema:id:int¦name:string¦active:bool", lines[1]);
        assertEquals("1¦John Doe¦true", lines[2]);
        assertEquals("2¦Jane Smith¦false", lines[3]);
    }

    @Test
    void testToJsonComplex() throws Exception {
        String input = """
            EntityName:Employee
            schema:id:int¦details:‹name:string¦age:int?›¦roles:«string»¦status:{active|inactive}
            1¦‹Alice¦30›¦«admin¦user»¦active
            2¦‹Bob›¦«user»¦inactive
            """;
        
        String json = LineStructParser.toJson(input);
        ObjectNode root = (ObjectNode) mapper.readTree(json);
        
        assertEquals("Employee", root.get("entityName").asText());
        ArrayNode data = (ArrayNode) root.get("data");
        assertEquals(2, data.size());
        
        ObjectNode first = (ObjectNode) data.get(0);
        assertEquals(1, first.get("id").asInt());
        ObjectNode details1 = (ObjectNode) first.get("details");
        assertEquals("Alice", details1.get("name").asText());
        assertEquals(30, details1.get("age").asInt());
        ArrayNode roles1 = (ArrayNode) first.get("roles");
        assertEquals(List.of("admin", "user"), mapper.convertValue(roles1, List.class));
        assertEquals("active", first.get("status").asText());
        
        ObjectNode second = (ObjectNode) data.get(1);
        assertEquals(2, second.get("id").asInt());
        ObjectNode details2 = (ObjectNode) second.get("details");
        assertEquals("Bob", details2.get("name").asText());
        assertTrue(details2.get("age").isNull());
        ArrayNode roles2 = (ArrayNode) second.get("roles");
        assertEquals(List.of("user"), mapper.convertValue(roles2, List.class));
        assertEquals("inactive", second.get("status").asText());
    }

    @Test
    void testFromJsonComplex() throws Exception {
        String json = """
            {
                "entityName": "Employee",
                "data": [
                    {
                        "id": 1,
                        "details": {"name": "Alice", "age": 30},
                        "roles": ["admin", "user"],
                        "status": "active"
                    },
                    {
                        "id": 2,
                        "details": {"name": "Bob", "age": null},
                        "roles": ["user"],
                        "status": "inactive"
                    }
                ]
            }
            """;
        
        String result = LineStructParser.fromJson(json);
        String[] lines = result.split("\n");
        
        assertEquals("EntityName:Employee", lines[0]);
        assertEquals("schema:id:int¦details:‹name:string¦age:int›¦roles:«string»¦status:string", lines[1]);
        assertEquals("1¦‹Alice¦30›¦«admin¦user»¦active", lines[2]);
        assertEquals("2¦‹Bob¦›¦«user»¦inactive", lines[3]);
    }

    @Test
    void testToJsonEscapedCharacters() throws Exception {
        String input = """
            EntityName:Test
            schema:name:string
            Alice⁊¦Bob
            """;
        
        String json = LineStructParser.toJson(input);
        ObjectNode root = (ObjectNode) mapper.readTree(json);
        
        assertEquals("Test", root.get("entityName").asText());
        ArrayNode data = (ArrayNode) root.get("data");
        assertEquals(1, data.size());
        assertEquals("Alice¦Bob", data.get(0).get("name").asText());
    }



    @Test
    void testFromJsonInvalidJson() {
        String json = "{invalid}";
        assertThrows(RuntimeException.class, () -> LineStructParser.fromJson(json));
    }
}