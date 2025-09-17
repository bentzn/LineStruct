package com.bentzn.util.linestruct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

 class LineStructPrettyPrintTest {
    
    @Test
    @DisplayName("Should format simple flat schema")
    public void testSimpleFlatSchema() {
        String input = "schema:id:int¦name:string¦age:int¦active:bool";
        String expected = "schema:\n" +
                         "  id:int\n" +
                         "  name:string\n" +
                         "  age:int\n" +
                         "  active:bool";
        
        String result = LineStructParser.prettyPrintSchema(input);
        assertEquals(expected, result);
    }
    
    @Test
    @DisplayName("Should format schema with nested object")
    public void testNestedObjectSchema() {
        String input = "schema:id:string¦person:‹name:string¦age:int›¦active:bool";
        String expected = "schema:\n" +
                         "  id:string\n" +
                         "  person:\n" +
                         "  ‹\n" +
                         "    name:string\n" +
                         "    age:int\n" +
                         "  ›\n" +
                         "  active:bool";
        
        String result = LineStructParser.prettyPrintSchema(input);
        assertEquals(expected, result);
    }
    
    @Test
    @DisplayName("Should format schema with array of primitives")
    public void testArrayOfPrimitivesSchema() {
        String input = "schema:id:string¦tags:«string»¦count:int";
        String expected = "schema:\n" +
                         "  id:string\n" +
                         "  tags:\n" +
                         "  «\n" +
                         "    string\n" +
                         "  »\n" +
                         "  count:int";
        
        String result = LineStructParser.prettyPrintSchema(input);
        assertEquals(expected, result);
    }
    
    @Test
    @DisplayName("Should format schema with array of objects")
    public void testArrayOfObjectsSchema() {
        String input = "schema:contacts:«‹name:string¦phone:string›»";
        String expected = "schema:\n" +
                         "  contacts:\n" +
                         "  «\n" +
                         "    ‹\n" +
                         "      name:string\n" +
                         "      phone:string\n" +
                         "    ›\n" +
                         "  »";
        
        String result = LineStructParser.prettyPrintSchema(input);
        assertEquals(expected, result);
    }
    
    @Test
    @DisplayName("Should preserve enum definitions inline")
    public void testEnumSchema() {
        String input = "schema:status:{active|inactive|pending}¦role:{admin|user|guest}";
        String expected = "schema:\n" +
                         "  status:{active|inactive|pending}\n" +
                         "  role:{admin|user|guest}";
        
        String result = LineStructParser.prettyPrintSchema(input);
        assertEquals(expected, result);
    }
    
    @Test
    @DisplayName("Should handle enum inside nested object")
    public void testEnumInNestedObject() {
        String input = "schema:entity:‹type:{national|international}¦status:{active|inactive}›";
        String expected = "schema:\n" +
                         "  entity:\n" +
                         "  ‹\n" +
                         "    type:{national|international}\n" +
                         "    status:{active|inactive}\n" +
                         "  ›";
        
        String result = LineStructParser.prettyPrintSchema(input);
        assertEquals(expected, result);
    }
    
    @Test
    @DisplayName("Should handle complex nested schema")
    public void testComplexNestedSchema() {
        String input = "schema:id:string¦data:‹items:«‹name:string¦value:float›»¦total:float›";
        String expected = "schema:\n" +
                         "  id:string\n" +
                         "  data:\n" +
                         "  ‹\n" +
                         "    items:\n" +
                         "    «\n" +
                         "      ‹\n" +
                         "        name:string\n" +
                         "        value:float\n" +
                         "      ›\n" +
                         "    »\n" +
                         "    total:float\n" +
                         "  ›";
        
        String result = LineStructParser.prettyPrintSchema(input);
        assertEquals(expected, result);
    }
    
    @Test
    @DisplayName("Should handle escaped delimiters")
    public void testEscapedDelimiters() {
        String inputWithEscape = "schema:text⁊¦label:string¦note:string";
        String expected = "schema:\n" +
                         "  text⁊¦label:string\n" +
                         "  note:string";
        
        String result = LineStructParser.prettyPrintSchema(inputWithEscape);
        assertEquals(expected, result);
    }
    
    @Test
    @DisplayName("Should handle schema without prefix")
    public void testSchemaWithoutPrefix() {
        String input = "id:string¦name:string";
        String expected = "schema:\n" +
                         "  id:string\n" +
                         "  name:string";
        
        String result = LineStructParser.prettyPrintSchema(input);
        assertEquals(expected, result);
    }
    
    @Test
    @DisplayName("Should use custom indent size")
    public void testCustomIndentSize() {
        String input = "schema:id:string¦nested:‹field:string›";
        String expected = "schema:\n" +
                         "    id:string\n" +
                         "    nested:\n" +
                         "    ‹\n" +
                         "        field:string\n" +
                         "    ›";
        
        String result = LineStructFormatter.prettyPrintSchema(input, 4);
        assertEquals(expected, result);
    }
    
    @Test
    @DisplayName("Should compact pretty schema back to single line")
    public void testCompactSchema() {
        String pretty = "schema:\n" +
                       "  id:string\n" +
                       "  person:\n" +
                       "  ‹\n" +
                       "    name:string\n" +
                       "    age:int\n" +
                       "  ›\n" +
                       "  active:bool";
        
        String expected = "schema:id:string¦person:‹name:string¦age:int›¦active:bool";
        
        String result = LineStructFormatter.compactSchema(pretty);
        assertEquals(expected, result);
    }
    
    @Test
    @DisplayName("Should handle round-trip formatting")
    public void testRoundTrip() {
        String original = "schema:id:string¦data:‹items:«‹name:string¦value:float›»¦total:float›¦status:{active|inactive}";
        
        String pretty = LineStructParser.prettyPrintSchema(original);
        String compact = LineStructFormatter.compactSchema(pretty);
        
        assertEquals(original, compact, "Round-trip should preserve original schema");
    }
    
    @Test
    @DisplayName("Should format the complex example schema")
    public void testComplexExampleSchema() {
        String input = "schema:id:string¦name:string¦name_en:string¦issuingEntity:‹name:string¦name_en:string¦acronym:string¦type:{national_regulator|european_authority|international_organization|government_body|self_regulatory_organization}¦country:string›¦pillar:{legal|ethical|secure|operational}";
        
        String result = LineStructParser.prettyPrintSchema(input);
        
        // Verify it contains expected structure
        assertTrue(result.contains("schema:"));
        assertTrue(result.contains("id:string"));
        assertTrue(result.contains("issuingEntity:"));
        assertTrue(result.contains("‹"));
        assertTrue(result.contains("›"));
        assertTrue(result.contains("type:{national_regulator|european_authority|international_organization|government_body|self_regulatory_organization}"));
        
        // Verify round-trip works
        String compact = LineStructFormatter.compactSchema(result);
        assertEquals(input, compact);
    }
    
    @Test
    @DisplayName("Should handle empty schema")
    public void testEmptySchema() {
        String input = "schema:";
        String expected = "schema:";
        
        String result = LineStructParser.prettyPrintSchema(input);
        assertEquals(expected, result);
    }
    
    @Test
    @DisplayName("Should handle deeply nested structures")
    public void testDeeplyNestedStructure() {
        String input = "schema:level1:‹level2:‹level3:‹level4:string›››";
        String expected = "schema:\n" +
                         "  level1:\n" +
                         "  ‹\n" +
                         "    level2:\n" +
                         "    ‹\n" +
                         "      level3:\n" +
                         "      ‹\n" +
                         "        level4:string\n" +
                         "      ›\n" +
                         "    ›\n" +
                         "  ›";
        
        String result = LineStructParser.prettyPrintSchema(input);
        assertEquals(expected, result);
    }
    
    @Test
    @DisplayName("Should handle optional fields notation")
    public void testOptionalFields() {
        String input = "schema:required:string¦optional:string?¦nested:‹field:int?›";
        String expected = "schema:\n" +
                         "  required:string\n" +
                         "  optional:string?\n" +
                         "  nested:\n" +
                         "  ‹\n" +
                         "    field:int?\n" +
                         "  ›";
        
        String result = LineStructParser.prettyPrintSchema(input);
        assertEquals(expected, result);
    }
    
    @Test
    @DisplayName("Should handle field descriptions with @ notation")
    public void testFieldDescriptions() {
        String input = "schema:id:string@desc=\"Unique identifier\"¦name:string@desc=\"User name\"";
        String expected = "schema:\n" +
                         "  id:string@desc=\"Unique identifier\"\n" +
                         "  name:string@desc=\"User name\"";
        
        String result = LineStructParser.prettyPrintSchema(input);
        assertEquals(expected, result);
    }
}