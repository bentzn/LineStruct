package com.bentzn.util.linestruct;

import com.bentzn.util.linestruct.LineStructParser.Schema;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Generator for converting LineStruct format to JSON.
 * 
 * This class handles the conversion of LineStruct formatted data to JSON,
 * including both full conversion and sample conversion with limited records.
 * 
 * @author bentzn/Grok/Claude
 */
class JsonGenerator {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Convert LineStruct format to JSON with a sample of up to 3 records.
     * 
     * This method performs the same conversion as toJson() but limits the
     * output to a maximum of 3 data records. Additionally, all nested arrays
     * within the data are also limited to 3 elements maximum.
     * 
     * @param lineStruct The LineStruct formatted string
     * @return JSON string with structure: {"entityName":"...", "data":[...]}
     *         containing up to 3 records with nested arrays also limited to 3 elements
     * @throws RuntimeException if the LineStruct format is invalid
     */
    static String toJsonSample(String lineStruct) {
        return toJson(lineStruct, 3, false, true);
    }



    /**
     * Convert LineStruct format to JSON.
     * 
     * LineStruct is a UTF-8 encoded format with the following structure: - Line
     * 1: EntityName:<name> - Line 2: schema:<field definitions> - Lines 3+:
     * Data rows
     * 
     * @param lineStruct The LineStruct formatted string
     * @return JSON string with structure: {"entityName":"...", "data":[...]}
     * @throws RuntimeException if the LineStruct format is invalid
     */
    static String toJson(String lineStruct) {
        return toJson(lineStruct, -1, false, false);
    }
    
    
    
    /**
     * Convert LineStruct format to JSON with optional trimming.
     * 
     * @param lineStruct The LineStruct formatted string
     * @param trim If true, removes incomplete lines from beginning and end
     * @return JSON string with structure: {"entityName":"...", "data":[...]}
     * @throws RuntimeException if the LineStruct format is invalid
     */
    public static String toJson(String lineStruct, boolean trim) {
        return toJson(lineStruct, -1, trim, false);
    }



    /**
     * Convert LineStruct format to JSON with a specified record limit.
     * 
     * This internal method performs the LineStruct to JSON conversion with a
     * configurable limit on the number of data records to process. It's used by
     * both toJson() and toJsonSample() methods.
     * 
     * @param lineStruct The LineStruct formatted string
     * @param maxRecords Maximum number of data records to include (use -1 for
     *            no limit)
     * @param trim If true, removes incomplete lines from beginning and end
     * @param limitNestedArrays If true, limits nested arrays to same size as maxRecords
     * @return JSON string with structure: {"entityName":"...", "data":[...]}
     * @throws RuntimeException if the LineStruct format is invalid
     */
    private static String toJson(String lineStruct, int maxRecords, boolean trim, boolean limitNestedArrays) {
        if(trim)
            lineStruct = trimLineStruct(lineStruct);

        try {
            String[] lines = lineStruct.trim().split("\n");

            if (lines.length < 2) {
                throw new IllegalArgumentException("Invalid LineStruct format: minimum 2 lines required");
            }

            // Parse entity name
            if (!lines[0].startsWith("EntityName:")) {
                throw new IllegalArgumentException("First line must start with 'EntityName:'");
            }
            String entityName = lines[0].substring("EntityName:".length());

            // Parse schema
            if (!lines[1].startsWith("schema:")) {
                throw new IllegalArgumentException("Second line must start with 'schema:'");
            }
            Schema schema = SchemaParser.parseSchema(lines[1]);

            // Parse data lines with limit
            ArrayNode dataArray = mapper.createArrayNode();
            int recordCount = 0;

            for (int i = 2; i < lines.length; i++) {
                // Check if we've reached the limit
                if (maxRecords > 0 && recordCount >= maxRecords) {
                    break;
                }

                if (!lines[i].trim().isEmpty()) {
                    try {
                        // Replace escaped characters before parsing
                        String processedLine = CharacterUtils.replaceEscapedChars(lines[i]);
                        ObjectNode dataObject = DataParser.parseDataLine(processedLine, schema.root, mapper);
                        
                        // Apply nested array limiting if requested
                        if (limitNestedArrays && maxRecords > 0) {
                            limitNestedArraysInObject(dataObject, maxRecords);
                        }
                        
                        dataArray.add(dataObject);
                        recordCount++;
                    }
                    catch (IllegalArgumentException e) {
                        // Skip invalid records (same behavior as original)
                        // Uncomment below to make it stricter:
                        // if(!e.getMessage().contains("Missing required
                        // field"))
                        // throw e;
                    }
                }
            }

            // Create final JSON
            ObjectNode result = mapper.createObjectNode();
            result.put("entityName", entityName);
            result.set("data", dataArray);

            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);

        }
        catch (Exception e) {
            throw new RuntimeException("Error converting LineStruct to JSON: " + e.getMessage(), e);
        }
    }
    
    /**
     * Recursively limit the size of all nested arrays in a JSON object.
     * 
     * @param node The JSON node to process
     * @param maxSize The maximum size for arrays
     */
    private static void limitNestedArraysInObject(ObjectNode node, int maxSize) {
        node.fields().forEachRemaining(entry -> {
            if (entry.getValue().isArray()) {
                ArrayNode arrayNode = (ArrayNode) entry.getValue();
                
                // Create a new limited array
                ArrayNode limitedArray = mapper.createArrayNode();
                int count = 0;
                for (int i = 0; i < arrayNode.size() && count < maxSize; i++) {
                    limitedArray.add(arrayNode.get(i));
                    count++;
                }
                
                // Replace the original array with the limited one
                node.set(entry.getKey(), limitedArray);
                
                // Recursively process elements in the limited array
                limitedArray.forEach(element -> {
                    if (element.isObject()) {
                        limitNestedArraysInObject((ObjectNode) element, maxSize);
                    }
                });
            } else if (entry.getValue().isObject()) {
                // Recursively process nested objects
                limitNestedArraysInObject((ObjectNode) entry.getValue(), maxSize);
            }
        });
    }
    
    

    /**
     * Trim LineStruct data by removing incomplete lines from beginning and end.
     * 
     * @param lineStruct The LineStruct formatted string to trim
     * @return Trimmed LineStruct string
     */
    private static String trimLineStruct(String lineStruct) {
        if (lineStruct == null || lineStruct.isEmpty()) {
            return lineStruct;
        }
        
        String[] lines = lineStruct.split("\n", -1); // Use -1 to preserve trailing empty strings
        
        // Find the first line that starts with 'EntityName:'
        int startIndex = -1;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].startsWith("EntityName:")) {
                startIndex = i;
                break;
            }
        }
        
        // If no EntityName found, return empty string
        if (startIndex == -1) {
            return "";
        }
        
        // Find the last complete line
        int endIndex = startIndex; // At minimum, include the EntityName line
        
        for (int i = startIndex; i < lines.length; i++) {
            String line = lines[i];
            
            if (i == startIndex) {
                // EntityName line - always complete
                endIndex = i;
            } else if (i == startIndex + 1 && line.startsWith("schema:")) {
                // Schema line - always complete
                endIndex = i;
            } else if (i > startIndex + 1) {
                // Data lines - check if complete
                if (isCompleteLine(line)) {
                    endIndex = i;
                } else {
                    // Stop at the first incomplete line
                    break;
                }
            }
        }
        
        // Build the trimmed result
        StringBuilder result = new StringBuilder();
        for (int i = startIndex; i <= endIndex; i++) {
            result.append(lines[i]);
            if (i < endIndex) {
                result.append("\n");
            }
        }
        
        // Ensure trailing newline for consistency with original format
        if (result.length() > 0 && !result.toString().endsWith("\n")) {
            result.append("\n");
        }
        
        return result.toString();
    }

    /**
     * Check if a data line is complete by verifying balanced delimiters.
     * 
     * @param line The line to check
     * @return true if the line appears complete, false otherwise
     */
    private static boolean isCompleteLine(String line) {
        if (line == null) {
            return false;
        }
        
        // Empty lines are considered complete (could be all optional fields)
        if (line.isEmpty()) {
            return true;
        }
        
        int arrayDepth = 0;
        int nestedDepth = 0;
        boolean inEscape = false;
        
        char[] chars = line.toCharArray();
        
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            
            // Handle escape sequences
            if (inEscape) {
                inEscape = false;
                continue;
            }
            
            if (c == '\\') {
                inEscape = true;
                continue;
            }
            
            // Check for array brackets
            if (c == CharacterUtils.ARRAY_START) {
                arrayDepth++;
            } else if (c == CharacterUtils.ARRAY_END) {
                arrayDepth--;
                if (arrayDepth < 0) {
                    // More closing than opening brackets
                    return false;
                }
            } else if (c == CharacterUtils.NESTED_START) {
                nestedDepth++;
            } else if (c == CharacterUtils.NESTED_END) {
                nestedDepth--;
                if (nestedDepth < 0) {
                    // More closing than opening brackets
                    return false;
                }
            }
        }
        
        // Line is complete if:
        // - All brackets are balanced
        // - Not in the middle of an escape sequence
        return arrayDepth == 0 && nestedDepth == 0 && !inEscape;
    }
}