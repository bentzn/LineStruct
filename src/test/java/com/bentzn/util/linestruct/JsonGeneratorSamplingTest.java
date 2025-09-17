package com.bentzn.util.linestruct;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for JsonGenerator sampling functionality.
 * Tests that toJsonSample() samples at all levels of nested data structures.
 */
class JsonGeneratorSamplingTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

    @Test
    void testTopLevelSampling() throws Exception {
        String lineStruct = buildLineStructWithMultipleRecords(10);
        
        String jsonResult = JsonGenerator.toJsonSample(lineStruct);
        JsonNode root = mapper.readTree(jsonResult);
        
        assertEquals("TestEntity", root.get("entityName").asText());
        JsonNode dataArray = root.get("data");
        assertTrue(dataArray.isArray());
        assertEquals(3, dataArray.size(), "Should sample maximum 3 top-level records");
    }

    @Test
    void testNestedArraySampling() throws Exception {
        String lineStruct = buildLineStructWithNestedArrays();
        
        String jsonResult = JsonGenerator.toJsonSample(lineStruct);
        JsonNode root = mapper.readTree(jsonResult);
        
        JsonNode dataArray = root.get("data");
        assertTrue(dataArray.size() <= 3, "Top level should be sampled to max 3");
        
        // Check that nested arrays are also sampled
        for (JsonNode record : dataArray) {
            JsonNode nestedArray = record.get("items");
            if (nestedArray != null && nestedArray.isArray()) {
                assertTrue(nestedArray.size() <= 3, 
                    "Nested arrays should also be sampled to max 3 items");
            }
        }
    }

    @Test
    void testDeeplyNestedSampling() throws Exception {
        String lineStruct = buildLineStructWithDeeplyNestedData();
        
        String jsonResult = JsonGenerator.toJsonSample(lineStruct);
        JsonNode root = mapper.readTree(jsonResult);
        
        JsonNode dataArray = root.get("data");
        assertTrue(dataArray.size() <= 3, "Top level should be sampled");
        
        // Verify deep nesting is also sampled
        for (JsonNode record : dataArray) {
            JsonNode level1 = record.get("nested");
            if (level1 != null) {
                JsonNode level2Array = level1.get("subItems");
                if (level2Array != null && level2Array.isArray()) {
                    assertTrue(level2Array.size() <= 3, 
                        "Level 2 nested arrays should be sampled");
                    
                    for (JsonNode level2Item : level2Array) {
                        JsonNode level3Array = level2Item.get("deepItems");
                        if (level3Array != null && level3Array.isArray()) {
                            assertTrue(level3Array.size() <= 3, 
                                "Level 3 nested arrays should be sampled");
                        }
                    }
                }
            }
        }
    }

    @Test
    void testMixedNestedStructures() throws Exception {
        String lineStruct = buildLineStructWithMixedNesting();
        
        String jsonResult = JsonGenerator.toJsonSample(lineStruct);
        JsonNode root = mapper.readTree(jsonResult);
        
        JsonNode dataArray = root.get("data");
        assertTrue(dataArray.size() <= 3, "Top level should be sampled");
        
        // Check various nested structures are sampled
        for (JsonNode record : dataArray) {
            // Check array fields
            JsonNode tags = record.get("tags");
            if (tags != null && tags.isArray()) {
                assertTrue(tags.size() <= 3, "Tags array should be sampled");
            }
            
            // Check nested object with arrays
            JsonNode metadata = record.get("metadata");
            if (metadata != null) {
                JsonNode categories = metadata.get("categories");
                if (categories != null && categories.isArray()) {
                    assertTrue(categories.size() <= 3, 
                        "Nested categories array should be sampled");
                }
            }
        }
    }

    @Test
    void testSamplingWithFewerThanThreeItems() throws Exception {
        String lineStruct = buildLineStructWithTwoRecords();
        
        String jsonResult = JsonGenerator.toJsonSample(lineStruct);
        JsonNode root = mapper.readTree(jsonResult);
        
        JsonNode dataArray = root.get("data");
        assertEquals(2, dataArray.size(), "Should preserve all records when fewer than 3");
    }

    @Test
    void testSamplingWithEmptyArrays() throws Exception {
        String lineStruct = buildLineStructWithEmptyArrays();
        
        String jsonResult = JsonGenerator.toJsonSample(lineStruct);
        JsonNode root = mapper.readTree(jsonResult);
        
        JsonNode dataArray = root.get("data");
        assertTrue(dataArray.size() <= 3, "Top level should be sampled");
        
        // Verify empty arrays remain empty
        for (JsonNode record : dataArray) {
            JsonNode emptyArray = record.get("emptyItems");
            if (emptyArray != null && emptyArray.isArray()) {
                assertEquals(0, emptyArray.size(), "Empty arrays should remain empty");
            }
        }
    }

    @Test
    void testComparisonWithFullConversion() throws Exception {
        String lineStruct = buildLineStructWithMultipleRecords(10);
        
        String fullJson = JsonGenerator.toJson(lineStruct);
        String sampleJson = JsonGenerator.toJsonSample(lineStruct);
        
        JsonNode fullRoot = mapper.readTree(fullJson);
        JsonNode sampleRoot = mapper.readTree(sampleJson);
        
        // Same entity name
        assertEquals(fullRoot.get("entityName").asText(), 
                    sampleRoot.get("entityName").asText());
        
        // Different data array sizes
        JsonNode fullData = fullRoot.get("data");
        JsonNode sampleData = sampleRoot.get("data");
        
        assertTrue(fullData.size() > sampleData.size(), 
            "Full conversion should have more records than sample");
        assertEquals(3, sampleData.size(), "Sample should have exactly 3 records");
    }

    // Helper methods to build test data

    private String buildLineStructWithMultipleRecords(int count) {
        StringBuilder sb = new StringBuilder();
        sb.append("EntityName:TestEntity\n");
        sb.append("schema:id:string,name:string,value:int\n");
        
        for (int i = 1; i <= count; i++) {
            sb.append(String.format("record_%d|Test Name %d|%d\n", i, i, i * 10));
        }
        
        return sb.toString();
    }

    private String buildLineStructWithNestedArrays() {
        StringBuilder sb = new StringBuilder();
        sb.append("EntityName:NestedEntity\n");
        sb.append("schema:id:string,name:string,items:array[string]\n");
        
        // Record 1 with 5 items in array
        sb.append("rec1|Record 1|[item1,item2,item3,item4,item5]\n");
        // Record 2 with 4 items in array  
        sb.append("rec2|Record 2|[alpha,beta,gamma,delta]\n");
        // Record 3 with 6 items in array
        sb.append("rec3|Record 3|[a,b,c,d,e,f]\n");
        // Record 4 with 3 items in array
        sb.append("rec4|Record 4|[x,y,z]\n");
        
        return sb.toString();
    }

    private String buildLineStructWithDeeplyNestedData() {
        StringBuilder sb = new StringBuilder();
        sb.append("EntityName:DeepEntity\n");
        sb.append("schema:id:string,nested:nested[subItems:array[nested[deepItems:array[string]]]]\n");
        
        // Record with deeply nested arrays that should be sampled
        sb.append("deep1|{subItems:[");
        sb.append("{deepItems:[d1,d2,d3,d4,d5]},");
        sb.append("{deepItems:[x1,x2,x3,x4]},");
        sb.append("{deepItems:[a,b,c,d,e,f,g]},");
        sb.append("{deepItems:[p,q,r]}");
        sb.append("]}\n");
        
        return sb.toString();
    }

    private String buildLineStructWithMixedNesting() {
        StringBuilder sb = new StringBuilder();
        sb.append("EntityName:MixedEntity\n");
        sb.append("schema:id:string,tags:array[string],metadata:nested[categories:array[string],count:int]\n");
        
        sb.append("mix1|[tag1,tag2,tag3,tag4,tag5]|{categories:[cat1,cat2,cat3,cat4],count:10}\n");
        sb.append("mix2|[alpha,beta,gamma]|{categories:[a,b,c,d,e],count:5}\n");
        sb.append("mix3|[x,y,z,w]|{categories:[type1,type2],count:2}\n");
        sb.append("mix4|[test1,test2,test3,test4,test5,test6]|{categories:[main,sub,other,extra],count:15}\n");
        
        return sb.toString();
    }

    private String buildLineStructWithTwoRecords() {
        StringBuilder sb = new StringBuilder();
        sb.append("EntityName:SmallEntity\n");
        sb.append("schema:id:string,name:string\n");
        sb.append("small1|First Record\n");
        sb.append("small2|Second Record\n");
        
        return sb.toString();
    }

    private String buildLineStructWithEmptyArrays() {
        StringBuilder sb = new StringBuilder();
        sb.append("EntityName:EmptyEntity\n");
        sb.append("schema:id:string,emptyItems:array[string],normalItems:array[string]\n");
        
        sb.append("empty1|[]|[a,b,c,d,e]\n");
        sb.append("empty2|[]|[x,y,z]\n");
        sb.append("empty3|[]|[]\n");
        sb.append("empty4|[]|[single]\n");
        
        return sb.toString();
    }
}