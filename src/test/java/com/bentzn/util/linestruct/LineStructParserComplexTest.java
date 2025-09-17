package com.bentzn.util.linestruct;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.*;

 class LineStructParserComplexTest {
    
    @Test
    void testDeepNestedObjectsWithArrays() throws Exception {
        String lineStruct = """
            EntityName:Company
            schema:id:int¦name:string¦departments:«‹deptId:int¦deptName:string¦employees:«‹empId:int¦empName:string¦skills:«string»›»›»
            1¦TechCorp¦«‹101¦Engineering¦«‹1001¦Alice¦«Java¦Python¦SQL»›¦‹1002¦Bob¦«JavaScript¦React¦Node»›»›¦‹102¦Marketing¦«‹2001¦Charlie¦«SEO¦Analytics¦Content»›»›»
            """;
        
        String json = LineStructParser.toJson(lineStruct);
        assertNotNull(json);
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);
        
        // Verify structure
        JsonNode company = root.get("data").get(0);
        assertEquals(1, company.get("id").asInt());
        assertEquals("TechCorp", company.get("name").asText());
        
        // Check departments array
        JsonNode departments = company.get("departments");
        assertTrue(departments.isArray());
        assertEquals(2, departments.size());
        
        // Check first department
        JsonNode engineering = departments.get(0);
        assertEquals(101, engineering.get("deptId").asInt());
        assertEquals("Engineering", engineering.get("deptName").asText());
        
        // Check employees in first department
        JsonNode employees = engineering.get("employees");
        assertTrue(employees.isArray());
        assertEquals(2, employees.size());
        
        // Check Alice's skills
        JsonNode alice = employees.get(0);
        assertEquals("Alice", alice.get("empName").asText());
        JsonNode aliceSkills = alice.get("skills");
        assertTrue(aliceSkills.isArray());
        assertEquals(3, aliceSkills.size());
        assertEquals("Java", aliceSkills.get(0).asText());
    }
    
    @Test
    void testMixedOptionalFieldsAndEnums() throws Exception {
        String lineStruct = """
            EntityName:Product
            schema:id:int¦name:string¦category:{ELECTRONICS|CLOTHING|FOOD}¦description:<string>?¦tags:«string»¦pricing:‹regular:float¦discount:<float>?¦currency:{USD|EUR|GBP}›
            1¦Laptop¦ELECTRONICS¦High-performance laptop¦«portable¦powerful¦business»¦‹999.99¦100.00¦USD›
            2¦T-Shirt¦CLOTHING¦¦«casual¦cotton»¦‹29.99¦¦EUR›
            3¦Coffee¦FOOD¦Premium blend¦«organic¦fair-trade¦arabica»¦‹12.50¦2.50¦GBP›
            """;
        
        String json = LineStructParser.toJson(lineStruct);
        assertNotNull(json);
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);
        JsonNode data = root.get("data");
        
        // Check first product with all fields
        JsonNode laptop = data.get(0);
        assertEquals("ELECTRONICS", laptop.get("category").asText());
        assertEquals("High-performance laptop", laptop.get("description").asText());
        assertEquals(100.00, laptop.get("pricing").get("discount").asDouble(), 0.01);
        
        // Check second product with optional fields empty
        JsonNode tshirt = data.get(1);
        assertEquals("CLOTHING", tshirt.get("category").asText());
        JsonNode description = tshirt.get("description");
        assertTrue(description.isNull() || description.asText().isEmpty());
        JsonNode discount = tshirt.get("pricing").get("discount");
        assertTrue(discount.isNull() || discount.asText().isEmpty());
        
        // Verify enum values
        assertEquals("USD", laptop.get("pricing").get("currency").asText());
        assertEquals("EUR", tshirt.get("pricing").get("currency").asText());
    }
    
    @Test
    void testEscapedCharactersInData() throws Exception {
        String lineStruct = """
            EntityName:Message
            schema:id:int¦content:string¦metadata:‹tags:«string»¦notes:string›
            1¦This has ⁊¦ pipe and ⁊‹ bracket¦‹«tag⁊¦1¦tag⁊›2»¦Notes with ⁊» array end›
            """;
        
        String json = LineStructParser.toJson(lineStruct);
        assertNotNull(json);
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);
        JsonNode message = root.get("data").get(0);
        
        // Check escaped characters are properly handled
        String content = message.get("content").asText();
        assertTrue(content.contains("¦"));
        assertTrue(content.contains("‹"));
        
        JsonNode tags = message.get("metadata").get("tags");
        assertEquals("tag¦1", tags.get(0).asText());
        assertEquals("tag›2", tags.get(1).asText());
        
        String notes = message.get("metadata").get("notes").asText();
        assertTrue(notes.contains("»"));
    }
    
    @Test
    void testArrayOfComplexObjects() throws Exception {
        String lineStruct = """
            EntityName:School
            schema:id:int¦name:string¦classes:«‹classId:int¦subject:string¦students:«‹studentId:int¦name:string¦grades:«int»›»¦schedule:‹day:{MON|TUE|WED|THU|FRI}¦time:string››»
            1¦Central High¦«‹101¦Math¦«‹1¦John¦«95¦87¦92»›¦‹2¦Jane¦«88¦91¦94»›»¦‹MON¦09:00››¦‹102¦Science¦«‹3¦Bob¦«78¦82¦85»›»¦‹WED¦14:00››»
            """;
        
        String json = LineStructParser.toJson(lineStruct);
        assertNotNull(json);
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);
        JsonNode school = root.get("data").get(0);
        
        JsonNode classes = school.get("classes");
        assertTrue(classes.isArray());
        assertEquals(2, classes.size());
        
        // Check first class
        JsonNode mathClass = classes.get(0);
        assertEquals("Math", mathClass.get("subject").asText());
        
        // Check students in math class
        JsonNode students = mathClass.get("students");
        assertEquals(2, students.size());
        
        // Check John's grades
        JsonNode john = students.get(0);
        JsonNode grades = john.get("grades");
        assertEquals(95, grades.get(0).asInt());
        assertEquals(87, grades.get(1).asInt());
        
        // Check schedule
        JsonNode schedule = mathClass.get("schedule");
        assertEquals("MON", schedule.get("day").asText());
        assertEquals("09:00", schedule.get("time").asText());
    }
    
    @Test
    void testRoundTripConversion() throws Exception {
        String original = """
            EntityName:ComplexEntity
            schema:id:int¦data:‹values:«int»¦nested:‹deep:«‹key:string¦value:float›»››¦status:{ACTIVE|PENDING|DONE}
            1¦‹«10¦20¦30»¦‹«‹alpha¦1.5›¦‹beta¦2.7›»››¦ACTIVE
            2¦‹«40¦50»¦‹«‹gamma¦3.14›»››¦PENDING
            """;
        
        // Convert to JSON
        String json = LineStructParser.toJson(original);
        assertNotNull(json);
        
        // Convert back to LineStruct
        String converted = LineStructParser.fromJson(json);
        assertNotNull(converted);
        
        // Convert to JSON again to verify consistency
        String json2 = LineStructParser.toJson(converted);
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode originalJson = mapper.readTree(json);
        JsonNode convertedJson = mapper.readTree(json2);
        
        // Verify the data is the same
        assertEquals(originalJson.get("entityName"), convertedJson.get("entityName"));
        assertEquals(originalJson.get("data").size(), convertedJson.get("data").size());
        
        // Check specific values
        JsonNode origFirst = originalJson.get("data").get(0);
        JsonNode convFirst = convertedJson.get("data").get(0);
        assertEquals(origFirst.get("id"), convFirst.get("id"));
        assertEquals(origFirst.get("status"), convFirst.get("status"));
    }
}