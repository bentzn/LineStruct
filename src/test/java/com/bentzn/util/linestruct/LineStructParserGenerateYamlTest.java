package com.bentzn.util.linestruct;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.yaml.snakeyaml.Yaml;
import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LineStructParserGenerateYamlTest {

    @Test
    @DisplayName("Generate YAML from simple LineStruct with primitives")
    public void testGenerateYamlSimplePrimitives() {
        String lineStruct = "EntityName:Person\n" +
                          "schema:id:int¦name:string¦active:bool\n" +
                          "1¦John Doe¦true\n" +
                          "2¦Jane Smith¦false";
        
        String yaml = LineStructParser.generateYaml(lineStruct);
        
        assertNotNull(yaml);
        assertTrue(yaml.contains("entityName: Person"));
        assertTrue(yaml.contains("fields:"));
        assertTrue(yaml.contains("id:"));
        assertTrue(yaml.contains("type: int"));
        assertTrue(yaml.contains("required: true"));
        assertTrue(yaml.contains("name:"));
        assertTrue(yaml.contains("type: string"));
        assertTrue(yaml.contains("active:"));
        assertTrue(yaml.contains("type: bool"));
        
        // Validate the generated YAML is parseable
        Yaml yamlParser = new Yaml();
        Map<String, Object> parsed = yamlParser.load(yaml);
        assertNotNull(parsed);
        assertEquals("Person", parsed.get("entityName"));
        assertTrue(parsed.containsKey("fields"));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Generate YAML with optional fields")
    public void testGenerateYamlOptionalFields() {
        String lineStruct = "EntityName:Employee\n" +
                          "schema:id:int¦name:string¦age:<int>?¦email:<string>?\n" +
                          "1¦John¦30¦john@example.com\n" +
                          "2¦Jane¦¦";
        
        String yaml = LineStructParser.generateYaml(lineStruct);
        
        assertNotNull(yaml);
        
        // Parse and validate
        Yaml yamlParser = new Yaml();
        Map<String, Object> parsed = yamlParser.load(yaml);
        Map<String, Object> fields = (Map<String, Object>) parsed.get("fields");
        
        // Check required fields
        Map<String, Object> idField = (Map<String, Object>) fields.get("id");
        assertTrue((Boolean) idField.get("required"));
        
        Map<String, Object> nameField = (Map<String, Object>) fields.get("name");
        assertTrue((Boolean) nameField.get("required"));
        
        // Check optional fields
        Map<String, Object> ageField = (Map<String, Object>) fields.get("age");
        assertFalse((Boolean) ageField.get("required"));
        assertEquals("int", ageField.get("type"));
        
        Map<String, Object> emailField = (Map<String, Object>) fields.get("email");
        assertFalse((Boolean) emailField.get("required"));
        assertEquals("string", emailField.get("type"));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Generate YAML with enum fields")
    public void testGenerateYamlEnumFields() {
        String lineStruct = "EntityName:Order\n" +
                          "schema:id:int¦status:{PENDING|PROCESSING|SHIPPED|DELIVERED}¦priority:<{LOW|MEDIUM|HIGH}>?\n" +
                          "1¦PENDING¦HIGH\n" +
                          "2¦SHIPPED¦";
        
        String yaml = LineStructParser.generateYaml(lineStruct);
        
        assertNotNull(yaml);
        
        // Parse and validate
        Yaml yamlParser = new Yaml();
        Map<String, Object> parsed = yamlParser.load(yaml);
        Map<String, Object> fields = (Map<String, Object>) parsed.get("fields");
        
        // Check enum field
        Map<String, Object> statusField = (Map<String, Object>) fields.get("status");
        assertEquals("enum", statusField.get("type"));
        List<String> statusValues = (List<String>) statusField.get("values");
        assertEquals(4, statusValues.size());
        assertTrue(statusValues.contains("PENDING"));
        assertTrue(statusValues.contains("PROCESSING"));
        assertTrue(statusValues.contains("SHIPPED"));
        assertTrue(statusValues.contains("DELIVERED"));
        assertTrue((Boolean) statusField.get("required"));
        
        // Check optional enum field
        Map<String, Object> priorityField = (Map<String, Object>) fields.get("priority");
        assertEquals("enum", priorityField.get("type"));
        List<String> priorityValues = (List<String>) priorityField.get("values");
        assertEquals(3, priorityValues.size());
        assertTrue(priorityValues.contains("LOW"));
        assertTrue(priorityValues.contains("MEDIUM"));
        assertTrue(priorityValues.contains("HIGH"));
        assertFalse((Boolean) priorityField.get("required"));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Generate YAML with nested objects")
    public void testGenerateYamlNestedObjects() {
        String lineStruct = "EntityName:User\n" +
                          "schema:id:int¦name:string¦address:‹street:string¦city:string¦zip:int›\n" +
                          "1¦John¦‹123 Main St¦New York¦10001›";
        
        String yaml = LineStructParser.generateYaml(lineStruct);
        
        assertNotNull(yaml);
        
        // Parse and validate
        Yaml yamlParser = new Yaml();
        Map<String, Object> parsed = yamlParser.load(yaml);
        Map<String, Object> fields = (Map<String, Object>) parsed.get("fields");
        
        // Check nested object
        Map<String, Object> addressField = (Map<String, Object>) fields.get("address");
        assertEquals("object", addressField.get("type"));
        assertTrue((Boolean) addressField.get("required"));
        
        Map<String, Object> addressProps = (Map<String, Object>) addressField.get("properties");
        assertNotNull(addressProps);
        
        Map<String, Object> streetField = (Map<String, Object>) addressProps.get("street");
        assertEquals("string", streetField.get("type"));
        assertTrue((Boolean) streetField.get("required"));
        
        Map<String, Object> cityField = (Map<String, Object>) addressProps.get("city");
        assertEquals("string", cityField.get("type"));
        assertTrue((Boolean) cityField.get("required"));
        
        Map<String, Object> zipField = (Map<String, Object>) addressProps.get("zip");
        assertEquals("int", zipField.get("type"));
        assertTrue((Boolean) zipField.get("required"));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Generate YAML with arrays")
    public void testGenerateYamlArrays() {
        String lineStruct = "EntityName:Product\n" +
                          "schema:id:int¦name:string¦tags:«string»¦prices:«float»\n" +
                          "1¦Widget¦«tag1¦tag2¦tag3»¦«10.5¦20.0¦15.75»";
        
        String yaml = LineStructParser.generateYaml(lineStruct);
        
        assertNotNull(yaml);
        
        // Parse and validate
        Yaml yamlParser = new Yaml();
        Map<String, Object> parsed = yamlParser.load(yaml);
        Map<String, Object> fields = (Map<String, Object>) parsed.get("fields");
        
        // Check array of strings
        Map<String, Object> tagsField = (Map<String, Object>) fields.get("tags");
        assertEquals("array", tagsField.get("type"));
        assertTrue((Boolean) tagsField.get("required"));
        Map<String, Object> tagsElementType = (Map<String, Object>) tagsField.get("elementType");
        assertEquals("string", tagsElementType.get("type"));
        
        // Check array of floats
        Map<String, Object> pricesField = (Map<String, Object>) fields.get("prices");
        assertEquals("array", pricesField.get("type"));
        assertTrue((Boolean) pricesField.get("required"));
        Map<String, Object> pricesElementType = (Map<String, Object>) pricesField.get("elementType");
        assertEquals("float", pricesElementType.get("type"));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Generate YAML with array of objects")
    public void testGenerateYamlArrayOfObjects() {
        String lineStruct = "EntityName:Order\n" +
                          "schema:id:int¦items:«‹product:string¦quantity:int¦price:float›»\n" +
                          "1¦«‹Widget¦2¦10.5›¦‹Gadget¦1¦25.0›»";
        
        String yaml = LineStructParser.generateYaml(lineStruct);
        
        assertNotNull(yaml);
        
        // Parse and validate
        Yaml yamlParser = new Yaml();
        Map<String, Object> parsed = yamlParser.load(yaml);
        Map<String, Object> fields = (Map<String, Object>) parsed.get("fields");
        
        // Check array of objects
        Map<String, Object> itemsField = (Map<String, Object>) fields.get("items");
        assertEquals("array", itemsField.get("type"));
        
        Map<String, Object> elementType = (Map<String, Object>) itemsField.get("elementType");
        assertEquals("object", elementType.get("type"));
        
        Map<String, Object> elementProps = (Map<String, Object>) elementType.get("properties");
        assertNotNull(elementProps);
        
        Map<String, Object> productField = (Map<String, Object>) elementProps.get("product");
        assertEquals("string", productField.get("type"));
        
        Map<String, Object> quantityField = (Map<String, Object>) elementProps.get("quantity");
        assertEquals("int", quantityField.get("type"));
        
        Map<String, Object> priceField = (Map<String, Object>) elementProps.get("price");
        assertEquals("float", priceField.get("type"));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Generate YAML with date and datetime fields")
    public void testGenerateYamlDateTimeFields() {
        String lineStruct = "EntityName:Event\n" +
                          "schema:id:int¦name:string¦date:date¦timestamp:datetime\n" +
                          "1¦Meeting¦2024-03-15¦2024-03-15T14:30:00Z";
        
        String yaml = LineStructParser.generateYaml(lineStruct);
        
        assertNotNull(yaml);
        
        // Parse and validate
        Yaml yamlParser = new Yaml();
        Map<String, Object> parsed = yamlParser.load(yaml);
        Map<String, Object> fields = (Map<String, Object>) parsed.get("fields");
        
        Map<String, Object> dateField = (Map<String, Object>) fields.get("date");
        assertEquals("date", dateField.get("type"));
        
        Map<String, Object> timestampField = (Map<String, Object>) fields.get("timestamp");
        assertEquals("datetime", timestampField.get("type"));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Generate YAML with complex nested structure")
    public void testGenerateYamlComplexNested() {
        String lineStruct = "EntityName:Company\n" +
                          "schema:id:int¦name:string¦departments:«‹name:string¦manager:‹name:string¦email:string›¦employees:«‹id:int¦name:string›»›»\n" +
                          "1¦TechCorp¦«‹Engineering¦‹Alice¦alice@tech.com›¦«‹101¦Bob›¦‹102¦Charlie›»›»";
        
        String yaml = LineStructParser.generateYaml(lineStruct);
        
        assertNotNull(yaml);
        
        // Parse and validate
        Yaml yamlParser = new Yaml();
        Map<String, Object> parsed = yamlParser.load(yaml);
        assertEquals("Company", parsed.get("entityName"));
        
        Map<String, Object> fields = (Map<String, Object>) parsed.get("fields");
        Map<String, Object> departmentsField = (Map<String, Object>) fields.get("departments");
        assertEquals("array", departmentsField.get("type"));
        
        Map<String, Object> deptElementType = (Map<String, Object>) departmentsField.get("elementType");
        assertEquals("object", deptElementType.get("type"));
        
        Map<String, Object> deptProps = (Map<String, Object>) deptElementType.get("properties");
        
        // Check manager object within department
        Map<String, Object> managerField = (Map<String, Object>) deptProps.get("manager");
        assertEquals("object", managerField.get("type"));
        
        // Check employees array within department
        Map<String, Object> employeesField = (Map<String, Object>) deptProps.get("employees");
        assertEquals("array", employeesField.get("type"));
    }

    @Test
    @DisplayName("Generate YAML then validate LineStruct against it")
    public void testGenerateYamlAndValidate() {
        String lineStruct = "EntityName:Product\n" +
                          "schema:id:int¦name:string¦price:float¦inStock:bool¦category:{ELECTRONICS|CLOTHING|FOOD}\n" +
                          "1¦Laptop¦999.99¦true¦ELECTRONICS\n" +
                          "2¦T-Shirt¦19.99¦false¦CLOTHING";
        
        // Generate YAML from LineStruct
        String yaml = LineStructParser.generateYaml(lineStruct);
        assertNotNull(yaml);
        
        // Validate the same LineStruct against the generated YAML
        String validationResult = LineStructParser.isValid(yaml, lineStruct);
        assertNull(validationResult, "LineStruct should be valid against its own generated YAML schema");
        
        // Test with invalid data
        String invalidLineStruct = "EntityName:Product\n" +
                                  "schema:id:int¦name:string¦price:float¦inStock:bool¦category:{ELECTRONICS|CLOTHING|FOOD}\n" +
                                  "1¦Laptop¦not-a-float¦true¦ELECTRONICS";
        
        String invalidResult = LineStructParser.isValid(yaml, invalidLineStruct);
        assertNotNull(invalidResult, "Invalid LineStruct should fail validation");
        assertTrue(invalidResult.contains("float"), "Error message should mention float type issue");
    }

    @Test
    @DisplayName("Generate YAML with empty schema")
    public void testGenerateYamlEmptySchema() {
        String lineStruct = "EntityName:Empty\n" +
                          "schema:";
        
        String yaml = LineStructParser.generateYaml(lineStruct);
        
        assertNotNull(yaml);
        assertTrue(yaml.contains("entityName: Empty"));
        assertTrue(yaml.contains("fields:"));
        
        // Parse and validate
        Yaml yamlParser = new Yaml();
        Map<String, Object> parsed = yamlParser.load(yaml);
        assertEquals("Empty", parsed.get("entityName"));
        assertTrue(parsed.containsKey("fields"));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Generate YAML with special characters in enum values")
    public void testGenerateYamlEnumSpecialChars() {
        String lineStruct = "EntityName:Status\n" +
                          "schema:id:int¦state:{ACTIVE|INACTIVE|PENDING-REVIEW|ON-HOLD}\n" +
                          "1¦PENDING-REVIEW";
        
        String yaml = LineStructParser.generateYaml(lineStruct);
        
        assertNotNull(yaml);
        
        // Parse and validate
        Yaml yamlParser = new Yaml();
        Map<String, Object> parsed = yamlParser.load(yaml);
        Map<String, Object> fields = (Map<String, Object>) parsed.get("fields");
        Map<String, Object> stateField = (Map<String, Object>) fields.get("state");
        List<String> values = (List<String>) stateField.get("values");
        
        assertTrue(values.contains("PENDING-REVIEW"));
        assertTrue(values.contains("ON-HOLD"));
    }

    @Test
    @DisplayName("Test null and empty input handling")
    public void testGenerateYamlErrorHandling() {
        // Test null input
        assertThrows(RuntimeException.class, () -> {
            LineStructParser.generateYaml(null);
        });
        
        // Test empty input
        assertThrows(RuntimeException.class, () -> {
            LineStructParser.generateYaml("");
        });
        
        // Test invalid format - no schema line
        assertThrows(RuntimeException.class, () -> {
            LineStructParser.generateYaml("EntityName:Test");
        });
        
        // Test invalid format - no EntityName
        assertThrows(RuntimeException.class, () -> {
            LineStructParser.generateYaml("schema:id:int");
        });
        
        // Test invalid format - wrong prefix
        assertThrows(RuntimeException.class, () -> {
            LineStructParser.generateYaml("Name:Test\nschema:id:int");
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Generate YAML with all primitive types")
    public void testGenerateYamlAllPrimitiveTypes() {
        String lineStruct = "EntityName:AllTypes\n" +
                          "schema:id:int¦name:string¦price:float¦active:bool¦birthDate:date¦lastLogin:datetime\n" +
                          "1¦Test¦99.99¦true¦2024-01-15¦2024-01-15T10:30:00Z";
        
        String yaml = LineStructParser.generateYaml(lineStruct);
        
        assertNotNull(yaml);
        
        // Parse and validate all types are correct
        Yaml yamlParser = new Yaml();
        Map<String, Object> parsed = yamlParser.load(yaml);
        Map<String, Object> fields = (Map<String, Object>) parsed.get("fields");
        
        assertEquals("int", ((Map<String, Object>) fields.get("id")).get("type"));
        assertEquals("string", ((Map<String, Object>) fields.get("name")).get("type"));
        assertEquals("float", ((Map<String, Object>) fields.get("price")).get("type"));
        assertEquals("bool", ((Map<String, Object>) fields.get("active")).get("type"));
        assertEquals("date", ((Map<String, Object>) fields.get("birthDate")).get("type"));
        assertEquals("datetime", ((Map<String, Object>) fields.get("lastLogin")).get("type"));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Generate YAML with deeply nested optional objects")
    public void testGenerateYamlDeepNestingOptional() {
        String lineStruct = "EntityName:DeepNest\n" +
                          "schema:id:int¦data:<‹level1:‹level2:‹level3:string›››>?\n" +
                          "1¦‹‹‹deep›››\n" +
                          "2¦";
        
        String yaml = LineStructParser.generateYaml(lineStruct);
        
        assertNotNull(yaml);
        System.out.println("Generated YAML:\n" + yaml); // Debug output
        
        // Parse and validate deep nesting
        Yaml yamlParser = new Yaml();
        Map<String, Object> parsed = yamlParser.load(yaml);
        Map<String, Object> fields = (Map<String, Object>) parsed.get("fields");
        
        Map<String, Object> dataField = (Map<String, Object>) fields.get("data");
        assertEquals("object", dataField.get("type"));
        assertFalse((Boolean) dataField.get("required"));
        
        Map<String, Object> dataProps = (Map<String, Object>) dataField.get("properties");
        Map<String, Object> level1Field = (Map<String, Object>) dataProps.get("level1");
        assertEquals("object", level1Field.get("type"));
        
        Map<String, Object> level1Props = (Map<String, Object>) level1Field.get("properties");
        Map<String, Object> level2Field = (Map<String, Object>) level1Props.get("level2");
        assertEquals("object", level2Field.get("type"));
        
        Map<String, Object> level2Props = (Map<String, Object>) level2Field.get("properties");
        Map<String, Object> level3Field = (Map<String, Object>) level2Props.get("level3");
        assertEquals("string", level3Field.get("type"));
    }
}