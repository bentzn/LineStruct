package com.bentzn.util.linestruct;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Grok/bentzn
 * @generated 2025-08-17
 */
 class Grok4LineStructParserTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testSimple() throws IOException {
        String structLine = """
EntityName:Person
schema:id:int¦name:string¦age:int¦active:bool
1¦John Doe¦30¦true
2¦Jane Smith¦25¦false
""";
        String jsonExpected = "{\"entityName\":\"Person\",\"data\":[{\"id\":1,\"name\":\"John Doe\",\"age\":30,\"active\":true},{\"id\":2,\"name\":\"Jane Smith\",\"age\":25,\"active\":false}]}";
        String jsonActual = LineStructParser.toJson(structLine);
        JsonNode nodeExpected = mapper.readTree(jsonExpected);
        JsonNode nodeActual = mapper.readTree(jsonActual);
        assertEquals(nodeExpected, nodeActual);
    }

    @Test
    void testNested() throws IOException {
        String structLine = """
EntityName:Contact
schema:id:int¦name:string¦address:‹street:string¦city:string›¦phones:«string»¦friends:«‹name:string¦relation:string›»
1¦Alice¦‹123 Main St¦Anytown›¦«555-1234¦555-5678»¦«‹Bob¦best›¦‹Charlie¦colleague›»
2¦Bob¦‹¦›¦«»¦«»
""";
        String jsonExpected = "{\"entityName\":\"Contact\",\"data\":[{\"id\":1,\"name\":\"Alice\",\"address\":{\"street\":\"123 Main St\",\"city\":\"Anytown\"},\"phones\":[\"555-1234\",\"555-5678\"],\"friends\":[{\"name\":\"Bob\",\"relation\":\"best\"},{\"name\":\"Charlie\",\"relation\":\"colleague\"}]},{\"id\":2,\"name\":\"Bob\",\"address\":{\"street\":\"\",\"city\":\"\"},\"phones\":[],\"friends\":[]}]}";
        String jsonActual = LineStructParser.toJson(structLine);
        JsonNode nodeExpected = mapper.readTree(jsonExpected);
        JsonNode nodeActual = mapper.readTree(jsonActual);
        assertEquals(nodeExpected, nodeActual);
    }

    @Test
    void testOptionalsAndTypes() throws IOException {
        String structLine = """
EntityName:Profile
schema:id:int¦score:float?¦birth:date?¦member:bool?¦status:{active|inactive|suspended}¦extra:‹note:string?›?
1¦3.14¦1990-01-01¦true¦active¦‹Hello›
2¦¦¦¦inactive¦
""";
        String jsonExpected = "{\"entityName\":\"Profile\",\"data\":[{\"id\":1,\"score\":3.14,\"birth\":\"1990-01-01\",\"member\":true,\"status\":\"active\",\"extra\":{\"note\":\"Hello\"}},{\"id\":2,\"score\":null,\"birth\":null,\"member\":null,\"status\":\"inactive\",\"extra\":null}]}";
        String jsonActual = LineStructParser.toJson(structLine);
        JsonNode nodeExpected = mapper.readTree(jsonExpected);
        JsonNode nodeActual = mapper.readTree(jsonActual);
        assertEquals(nodeExpected, nodeActual);
    }

    @Test
    void testEscapes() throws IOException {
        String structLine = """
EntityName:EscapeTest
schema:id:int¦desc:string¦options:{a⁊|b|c⁊¦d}
1¦Note⁊¦ with delimiters ⁊‹⁊›⁊«⁊»¦a⁊|b
""";
        String jsonExpected = "{\"entityName\":\"EscapeTest\",\"data\":[{\"id\":1,\"desc\":\"Note¦ with delimiters ‹›«»\",\"options\":\"a|b\"}]}";
        String jsonActual = LineStructParser.toJson(structLine);
        JsonNode nodeExpected = mapper.readTree(jsonExpected);
        JsonNode nodeActual = mapper.readTree(jsonActual);
        assertEquals(nodeExpected, nodeActual);
    }

    @Test
    void testEmptyAndNull() throws IOException {
        String structLine = """
EntityName:OptionalTest
schema:id:int¦optStr:string?¦optArr:«int»?¦optObj:‹val:string›?
1¦value¦«1¦2»¦‹test›
2¦¦¦
""";
        String jsonExpected = "{\"entityName\":\"OptionalTest\",\"data\":[{\"id\":1,\"optStr\":\"value\",\"optArr\":[1,2],\"optObj\":{\"val\":\"test\"}},{\"id\":2,\"optStr\":null,\"optArr\":null,\"optObj\":null}]}";
        String jsonActual = LineStructParser.toJson(structLine);
        JsonNode nodeExpected = mapper.readTree(jsonExpected);
        JsonNode nodeActual = mapper.readTree(jsonActual);
        assertEquals(nodeExpected, nodeActual);
    }
}