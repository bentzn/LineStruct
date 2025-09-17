package com.bentzn.util.linestruct;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class GenerateSchemaTest {

    @Test
    @DisplayName("Test simple primitive fields conversion")
    void testSimplePrimitiveFields() {
        String yaml = """
            entityName: Person
            fields:
              id:
                type: int
                required: true
              name:
                type: string
                required: true
              age:
                type: int
                required: false
              active:
                type: bool
                required: true
            """;
        
        String schema = LineStructParser.getSchema(yaml);
        
        assertNotNull(schema);
        assertTrue(schema.startsWith("schema:"));
        assertTrue(schema.contains("id:int"));
        assertTrue(schema.contains("name:string"));
        assertTrue(schema.contains("age:int?"));  // Fixed: was age:<int>?
        assertTrue(schema.contains("active:bool"));
        
        System.out.println("Generated schema: " + schema);
    }

    @Test
    @DisplayName("Test enum field conversion")
    void testEnumField() {
        String yaml = """
            fields:
              status:
                type: enum
                values: [ACTIVE, INACTIVE, PENDING]
                required: true
              priority:
                type: enum
                values: [LOW, MEDIUM, HIGH]
                required: false
            """;
        
        String schema = LineStructParser.getSchema(yaml);
        
        assertNotNull(schema);
        assertTrue(schema.contains("status:{ACTIVE|INACTIVE|PENDING}"));
        assertTrue(schema.contains("priority:{LOW|MEDIUM|HIGH}?"));  // Fixed: was priority:<{LOW|MEDIUM|HIGH}>?
        
        System.out.println("Generated schema: " + schema);
    }

    @Test
    @DisplayName("Test array field conversion")
    void testArrayField() {
        String yaml = """
            fields:
              tags:
                type: array
                elementType:
                  type: string
                required: true
              scores:
                type: array
                elementType:
                  type: int
                required: false
            """;
        
        String schema = LineStructParser.getSchema(yaml);
        
        assertNotNull(schema);
        assertTrue(schema.contains("tags:«string»"));
        assertTrue(schema.contains("scores:«int»?"));  // Fixed: was scores:<«int»>?
        
        System.out.println("Generated schema: " + schema);
    }

    @Test
    @DisplayName("Test nested object conversion")
    void testNestedObject() {
        String yaml = """
            fields:
              address:
                type: object
                required: true
                properties:
                  street:
                    type: string
                    required: true
                  city:
                    type: string
                    required: true
                  zip:
                    type: int
                    required: false
            """;
        
        String schema = LineStructParser.getSchema(yaml);
        
        assertNotNull(schema);
        assertTrue(schema.contains("address:‹street:string¦city:string¦zip:int?›"));  // Fixed: was zip:<int>?
        
        System.out.println("Generated schema: " + schema);
    }

    @Test
    @DisplayName("Test complex nested structure")
    void testComplexNestedStructure() {
        String yaml = """
            entityName: Order
            fields:
              id:
                type: int
                required: true
              customer:
                type: object
                required: true
                properties:
                  name:
                    type: string
                    required: true
                  email:
                    type: string
                    required: true
              items:
                type: array
                required: true
                elementType:
                  type: object
                  properties:
                    product:
                      type: string
                      required: true
                    quantity:
                      type: int
                      required: true
                    price:
                      type: float
                      required: true
              status:
                type: enum
                values: [pending, shipped, delivered]
                required: true
            """;
        
        String schema = LineStructParser.getSchema(yaml);
        
        assertNotNull(schema);
        assertTrue(schema.startsWith("schema:"));
        assertTrue(schema.contains("id:int"));
        assertTrue(schema.contains("customer:‹name:string¦email:string›"));
        assertTrue(schema.contains("items:«‹product:string¦quantity:int¦price:float›»"));
        assertTrue(schema.contains("status:{pending|shipped|delivered}"));
        
        System.out.println("Generated schema: " + schema);
    }

    @Test
    @DisplayName("Test date and datetime types")
    void testDateTimeTypes() {
        String yaml = """
            fields:
              birthDate:
                type: date
                required: true
              lastLogin:
                type: datetime
                required: false
              created:
                type: datetime
                required: true
            """;
        
        String schema = LineStructParser.getSchema(yaml);
        
        assertNotNull(schema);
        assertTrue(schema.contains("birthDate:date"));
        assertTrue(schema.contains("lastLogin:datetime?"));  // Fixed: was lastLogin:<datetime>?
        assertTrue(schema.contains("created:datetime"));
        
        System.out.println("Generated schema: " + schema);
    }

    @Test
    @DisplayName("Test optional nested object")
    void testOptionalNestedObject() {
        String yaml = """
            fields:
              metadata:
                type: object
                required: false
                properties:
                  key:
                    type: string
                    required: true
                  value:
                    type: string
                    required: false
            """;
        
        String schema = LineStructParser.getSchema(yaml);
        
        assertNotNull(schema);
        assertTrue(schema.contains("metadata:‹key:string¦value:string?›?"));  // Fixed: was metadata:<‹key:string¦value:<string>?›>?
        
        System.out.println("Generated schema: " + schema);
    }

    @Test
    @DisplayName("Test array of objects")
    void testArrayOfObjects() {
        String yaml = """
            fields:
              departments:
                type: array
                required: true
                elementType:
                  type: object
                  properties:
                    name:
                      type: string
                      required: true
                    employees:
                      type: array
                      required: true
                      elementType:
                        type: object
                        properties:
                          id:
                            type: int
                            required: true
                          name:
                            type: string
                            required: true
            """;
        
        String schema = LineStructParser.getSchema(yaml);
        
        assertNotNull(schema);
        assertTrue(schema.contains("departments:«‹name:string¦employees:«‹id:int¦name:string›»›»"));
        
        System.out.println("Generated schema: " + schema);
    }

    @Test
    @DisplayName("Test round-trip conversion")
    void testRoundTripConversion() {
        // Start with a LineStruct schema
        String originalLineStruct = """
            EntityName:TestEntity
            schema:id:int¦name:string¦age:int?¦tags:«string»¦address:‹street:string¦city:string›¦status:{ACTIVE|INACTIVE}
            1¦Test¦30¦«tag1¦tag2»¦‹123 Main¦NYC›¦ACTIVE
            """;  // Fixed: was age:<int>?
        
        // Convert to YAML
        String yaml = LineStructParser.generateYaml(originalLineStruct);
        assertNotNull(yaml);
        System.out.println("Generated YAML:\n" + yaml);
        
        // Convert back to schema line
        String schema = LineStructParser.getSchema(yaml);
        assertNotNull(schema);
        System.out.println("Regenerated schema: " + schema);
        
        // Verify the schema contains all expected elements
        assertTrue(schema.startsWith("schema:"));
        assertTrue(schema.contains("id:int"));
        assertTrue(schema.contains("name:string"));
        assertTrue(schema.contains("age:int?"));  // Fixed: was age:<int>?
        assertTrue(schema.contains("tags:«string»"));
        assertTrue(schema.contains("address:‹street:string¦city:string›"));
        assertTrue(schema.contains("status:{ACTIVE|INACTIVE}"));
    }

    @Test
    @DisplayName("Test all primitive types")
    void testAllPrimitiveTypes() {
        String yaml = """
            fields:
              intField:
                type: int
                required: true
              floatField:
                type: float
                required: true
              boolField:
                type: bool
                required: true
              stringField:
                type: string
                required: true
              dateField:
                type: date
                required: true
              datetimeField:
                type: datetime
                required: true
              optInt:
                type: int
                required: false
              optFloat:
                type: float
                required: false
              optBool:
                type: bool
                required: false
              optString:
                type: string
                required: false
              optDate:
                type: date
                required: false
              optDatetime:
                type: datetime
                required: false
            """;
        
        String schema = LineStructParser.getSchema(yaml);
        
        assertNotNull(schema);
        
        // Required fields
        assertTrue(schema.contains("intField:int"));
        assertTrue(schema.contains("floatField:float"));
        assertTrue(schema.contains("boolField:bool"));
        assertTrue(schema.contains("stringField:string"));
        assertTrue(schema.contains("dateField:date"));
        assertTrue(schema.contains("datetimeField:datetime"));
        
        // Optional fields - Fixed: removed angle brackets
        assertTrue(schema.contains("optInt:int?"));
        assertTrue(schema.contains("optFloat:float?"));
        assertTrue(schema.contains("optBool:bool?"));
        assertTrue(schema.contains("optString:string?"));
        assertTrue(schema.contains("optDate:date?"));
        assertTrue(schema.contains("optDatetime:datetime?"));
        
        System.out.println("Generated schema: " + schema);
    }

    @Test
    @DisplayName("Test flat YAML structure without fields key")
    void testFlatYamlStructure() {
        String yaml = """
            id:
              type: int
              required: true
            name:
              type: string
              required: true
            active:
              type: bool
              required: false
            """;
        
        String schema = LineStructParser.getSchema(yaml);
        
        assertNotNull(schema);
        assertTrue(schema.startsWith("schema:"));
        assertTrue(schema.contains("id:int"));
        assertTrue(schema.contains("name:string"));
        assertTrue(schema.contains("active:bool?"));  // Fixed: was active:<bool>?
        
        System.out.println("Generated schema: " + schema);
    }

    @Test
    @DisplayName("Test error handling for invalid YAML")
    void testErrorHandling() {
        // Test null input
        assertThrows(RuntimeException.class, () -> {
            LineStructParser.getSchema(null);
        });
        
        // Test empty input
        assertThrows(RuntimeException.class, () -> {
            LineStructParser.getSchema("");
        });
        
        // Test invalid YAML
        assertThrows(RuntimeException.class, () -> {
            LineStructParser.getSchema("invalid: yaml: structure:");
        });
    }
}