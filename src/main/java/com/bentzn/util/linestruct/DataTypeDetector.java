package com.bentzn.util.linestruct;

import com.bentzn.util.linestruct.LineStructParser.DataType;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class for detecting data types (JSON, LineStruct, or other).
 * 
 * This class provides methods to identify whether a string represents valid
 * JSON, valid LineStruct format, or neither.
 * 
 * @author bentzn/Grok/Claude
 */
class DataTypeDetector {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Determine the data type of a string.
     * 
     * @param data The string to analyze
     * @return DataType.JSON if valid JSON, DataType.LINESTRUCT if valid
     *         LineStruct, DataType.OTHER otherwise
     */
    static DataType getDataType(String data) {
        if (data == null || data.trim().isEmpty()) {
            return DataType.OTHER;
        }

        String trimmed = data.trim();

        // Check for JSON first (starts with { or [ and is valid JSON)
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            if (isValidJson(trimmed)) {
                return DataType.JSON;
            }
        }

        // Check for LineStruct (starts with EntityName: and has schema:)
        if (trimmed.startsWith("EntityName:")) {
            if (isValidLineStruct(trimmed)) {
                return DataType.LINESTRUCT;
            }
        }

        return DataType.OTHER;
    }



    /**
     * Validate if a string is valid JSON.
     * 
     * @param json The string to validate
     * @return true if the string is valid JSON, false otherwise
     */
    static boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }

        try {
            mapper.readTree(json);
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }



    /**
     * Validate if a string is valid LineStruct format.
     * 
     * Checks structure and validates all data rows against the schema. This
     * method delegates to the Validator class for the actual validation.
     * 
     * @param lineStruct The string to validate
     * @return true if valid LineStruct format with all data matching schema,
     *         false otherwise
     */
    static boolean isValidLineStruct(String lineStruct) {
        if (lineStruct == null || lineStruct.trim().isEmpty()) {
            return false;
        }

        try {
            String[] lines = lineStruct.trim().split("\n");

            // Minimum 2 lines required
            if (lines.length < 2) {
                return false;
            }

            // Check EntityName line
            if (!lines[0].startsWith("EntityName:")) {
                return false;
            }

            // Check schema line
            if (!lines[1].startsWith("schema:")) {
                return false;
            }

            // Delegate to Validator for full validation
            return Validator.isValidLineStructFormat(lineStruct);

        }
        catch (Exception e) {
            return false;
        }
    }
}