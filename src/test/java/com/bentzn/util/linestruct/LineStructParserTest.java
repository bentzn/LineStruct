package com.bentzn.util.linestruct;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.*;

 class LineStructParserTest {

    @Test
    void testBasicConversion() throws Exception {
        // Test data
        String lineStruct = """
                EntityName:Person
                schema:id:int¦name:string¦age:int¦active:bool
                1¦John Doe¦30¦true
                2¦Jane Smith¦25¦false
                """;

        // Convert to JSON
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



    @Test
    void testNestedObjects() throws Exception {
        String lineStruct = """
                EntityName:Employee
                schema:id:int¦name:string¦address:‹street:string¦city:string›
                1¦John Doe¦‹123 Main St¦New York›
                """;

        String json = LineStructParser.toJson(lineStruct);
        assertNotNull(json);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);
        JsonNode address = root.get("data").get(0).get("address");
        assertEquals("123 Main St", address.get("street").asText());
        assertEquals("New York", address.get("city").asText());
    }



    @Test
    void testArrays() throws Exception {
        String lineStruct = """
                EntityName:Student
                schema:id:int¦name:string¦subjects:«string»
                1¦Alice¦«Math¦Science¦English»
                """;

        String json = LineStructParser.toJson(lineStruct);
        assertNotNull(json);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);
        JsonNode subjects = root.get("data").get(0).get("subjects");
        assertTrue(subjects.isArray());
        assertEquals(3, subjects.size());
        assertEquals("Math", subjects.get(0).asText());
    }



    @Test
    void testGetPrompt() {
        String prompt = LineStructParser.getPrompt();
        assertNotNull(prompt);
        assertTrue(prompt.contains("LineStruct Format Specification"));
    }



    @Test
    void testToJsonSample() throws Exception {
        // Test data with more than 3 records
        String lineStruct = """
                EntityName:Person
                schema:id:int¦name:string¦age:int¦active:bool
                1¦John Doe¦30¦true
                2¦Jane Smith¦25¦false
                3¦Bob Wilson¦45¦true
                4¦Alice Brown¦28¦false
                5¦Charlie Davis¦35¦true
                6¦Diana Evans¦32¦false
                """;

        // Convert to JSON sample (should only include first 3 records)
        String jsonSample = LineStructParser.toJsonSample(lineStruct);
        assertNotNull(jsonSample);

        // Convert to full JSON (should include all records)
        String jsonFull = LineStructParser.toJson(lineStruct);
        assertNotNull(jsonFull);

        // Parse both results
        ObjectMapper mapper = new ObjectMapper();
        JsonNode sampleRoot = mapper.readTree(jsonSample);
        JsonNode fullRoot = mapper.readTree(jsonFull);

        // Verify entity names are the same
        assertEquals("Person", sampleRoot.get("entityName").asText());
        assertEquals("Person", fullRoot.get("entityName").asText());

        // Verify sample has exactly 3 records
        assertEquals(3, sampleRoot.get("data").size(), "Sample should have exactly 3 records");

        // Verify full has all 6 records
        assertEquals(6, fullRoot.get("data").size(), "Full should have all 6 records");

        // Verify the first 3 records are identical in both
        for (int i = 0; i < 3; i++) {
            JsonNode sampleRecord = sampleRoot.get("data").get(i);
            JsonNode fullRecord = fullRoot.get("data").get(i);

            assertEquals(sampleRecord.get("id").asInt(), fullRecord.get("id").asInt());
            assertEquals(sampleRecord.get("name").asText(), fullRecord.get("name").asText());
            assertEquals(sampleRecord.get("age").asInt(), fullRecord.get("age").asInt());
            assertEquals(sampleRecord.get("active").asBoolean(), fullRecord.get("active").asBoolean());
        }

        // Verify sample contains the expected names
        assertEquals("John Doe", sampleRoot.get("data").get(0).get("name").asText());
        assertEquals("Jane Smith", sampleRoot.get("data").get(1).get("name").asText());
        assertEquals("Bob Wilson", sampleRoot.get("data").get(2).get("name").asText());

        // Verify sample does NOT contain records 4-6
        String sampleJson = jsonSample;
        assertFalse(sampleJson.contains("Alice Brown"), "Sample should not contain 4th record");
        assertFalse(sampleJson.contains("Charlie Davis"), "Sample should not contain 5th record");
        assertFalse(sampleJson.contains("Diana Evans"), "Sample should not contain 6th record");

        System.out.println("Sample JSON (3 records):");
        System.out.println(jsonSample);
        System.out.println("\nFull JSON (6 records):");
        System.out.println(jsonFull);
    }



    @Test
    void testToJsonSampleWithFewerThan3Records() throws Exception {
        // Test data with only 2 records
        String lineStruct = """
                EntityName:Product
                schema:id:int¦name:string¦price:float
                1¦Widget¦19.99
                2¦Gadget¦29.99
                """;

        // Convert to JSON sample
        String jsonSample = LineStructParser.toJsonSample(lineStruct);
        assertNotNull(jsonSample);

        // Parse result
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonSample);

        // Verify it returns all available records (2 in this case)
        assertEquals(2, root.get("data").size(), "Should return all records when fewer than 3 exist");

        // Verify the records
        assertEquals("Widget", root.get("data").get(0).get("name").asText());
        assertEquals("Gadget", root.get("data").get(1).get("name").asText());
    }



    @Test
    void testToJsonSampleWithExactly3Records() throws Exception {
        // Test data with exactly 3 records
        String lineStruct = """
                EntityName:Order
                schema:id:int¦customer:string¦total:float
                1¦Alice¦100.50
                2¦Bob¦200.75
                3¦Charlie¦150.25
                """;

        // Convert to JSON sample
        String jsonSample = LineStructParser.toJsonSample(lineStruct);
        assertNotNull(jsonSample);

        // Parse result
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonSample);

        // Verify it returns exactly 3 records
        assertEquals(3, root.get("data").size(), "Should return exactly 3 records");

        // Verify all records are present
        assertEquals("Alice", root.get("data").get(0).get("customer").asText());
        assertEquals("Bob", root.get("data").get(1).get("customer").asText());
        assertEquals("Charlie", root.get("data").get(2).get("customer").asText());
    }



    @Test
    void testToJsonSampleWithComplexSchema() throws Exception {
        // Test with complex schema including nested objects and arrays
        String lineStruct = """
                EntityName:Company
                schema:id:int¦name:string¦employees:«‹name:string¦role:string›»¦location:‹city:string¦country:string›
                1¦TechCorp¦«‹Alice¦CEO›¦‹Bob¦CTO›»¦‹New York¦USA›
                2¦DataInc¦«‹Charlie¦Manager›»¦‹London¦UK›
                3¦WebCo¦«‹David¦Developer›¦‹Eve¦Designer›»¦‹Paris¦France›
                4¦CloudNet¦«‹Frank¦Admin›»¦‹Berlin¦Germany›
                5¦AILabs¦«‹Grace¦Researcher›»¦‹Tokyo¦Japan›
                """;

        // Convert to JSON sample
        String jsonSample = LineStructParser.toJsonSample(lineStruct);
        assertNotNull(jsonSample);

        // Parse result
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonSample);

        // Verify only 3 records are included
        assertEquals(3, root.get("data").size(), "Sample should have exactly 3 records");

        // Verify the companies included
        assertEquals("TechCorp", root.get("data").get(0).get("name").asText());
        assertEquals("DataInc", root.get("data").get(1).get("name").asText());
        assertEquals("WebCo", root.get("data").get(2).get("name").asText());

        // Verify complex nested structures are preserved
        JsonNode firstEmployees = root.get("data").get(0).get("employees");
        assertTrue(firstEmployees.isArray());
        assertEquals(2, firstEmployees.size());
        assertEquals("Alice", firstEmployees.get(0).get("name").asText());
        assertEquals("CEO", firstEmployees.get(0).get("role").asText());

        JsonNode firstLocation = root.get("data").get(0).get("location");
        assertEquals("New York", firstLocation.get("city").asText());
        assertEquals("USA", firstLocation.get("country").asText());

        // Verify CloudNet and AILabs are not in the sample
        String sampleJson = jsonSample;
        assertFalse(sampleJson.contains("CloudNet"), "Sample should not contain 4th record");
        assertFalse(sampleJson.contains("AILabs"), "Sample should not contain 5th record");
    }



    @Test
    void testToJsonSampleWithEmptyData() throws Exception {
        // Test with no data records
        String lineStruct = """
                EntityName:Empty
                schema:id:int¦name:string
                """;

        // Convert to JSON sample
        String jsonSample = LineStructParser.toJsonSample(lineStruct);
        assertNotNull(jsonSample);

        // Parse result
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonSample);

        // Verify empty data array
        assertEquals("Empty", root.get("entityName").asText());
        assertEquals(0, root.get("data").size(), "Should have empty data array");
    }
}