package com.bentzn.util.linestruct;

import static com.bentzn.util.linestruct.CharacterUtils.ARRAY_END;
import static com.bentzn.util.linestruct.CharacterUtils.ARRAY_START;
import static com.bentzn.util.linestruct.CharacterUtils.FIELD_DELIMITER;
import static com.bentzn.util.linestruct.CharacterUtils.NESTED_END;
import static com.bentzn.util.linestruct.CharacterUtils.NESTED_START;
import static com.bentzn.util.linestruct.CharacterUtils.restoreEscapedChars;

import java.util.List;

import com.bentzn.util.linestruct.LineStructParser.ASTNode;
import com.bentzn.util.linestruct.LineStructParser.NodeType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Parser for LineStruct data lines.
 * 
 * This class handles parsing of data lines according to schema definitions,
 * converting them into JSON representations.
 * 
 * @author bentzn/Grok/Claude
 */
class DataParser {

    /**
     * Parse a data line according to schema
     * 
     * @param dataLine The data line to parse
     * @param schema The schema defining the structure
     * @param mapper ObjectMapper for creating JSON nodes
     * @return ObjectNode representing the parsed data
     * @throws IllegalArgumentException if required fields are missing
     */
    static ObjectNode parseDataLine(String dataLine, ASTNode schema, ObjectMapper mapper) {
        List<String> values = FieldSplitter.splitDataValues(dataLine);

        ObjectNode result = mapper.createObjectNode();

        // Schema root is always an object with fields as children
        for (int i = 0; i < schema.children.size(); i++) {
            ASTNode field = schema.children.get(i);

            // Check if we have a value for this field
            if (i >= values.size()) {
                // No value provided - field was omitted at end of line
                if (field.isOptional) {
                    result.set(field.name, mapper.nullNode());
                }
                else {
                    // Required field missing
                    throw new IllegalArgumentException("Missing required field: " + field.name);
                }
                continue;
            }

            String value = values.get(i);

            // Handle empty values for optional fields
            if (value.isEmpty() && field.isOptional) {
                result.set(field.name, mapper.nullNode());
                continue;
            }

            if (field.type == NodeType.OBJECT) {
                // Parse nested object
                String objContent = value;
                if (objContent.startsWith(String.valueOf(NESTED_START))
                        && objContent.endsWith(String.valueOf(NESTED_END))) {
                    objContent = objContent.substring(1, objContent.length() - 1);
                }
                ObjectNode nested = parseObjectContent(objContent, field, mapper);
                result.set(field.name, nested);

            }
            else if (field.type == NodeType.ARRAY) {
                // Parse array
                ArrayNode arr = parseArrayValue(value, field.elementType, mapper);
                result.set(field.name, arr);

            }
            else {
                // Parse primitive or enum
                JsonNode nodeValue = parsePrimitiveValue(value, field, mapper);
                result.set(field.name, nodeValue);
            }
        }

        return result;
    }



    /**
     * Parse object content
     * 
     * @param content The object content string
     * @param objectType The object type definition
     * @param mapper ObjectMapper for creating JSON nodes
     * @return ObjectNode representing the parsed object
     */
    static ObjectNode parseObjectContent(String content, ASTNode objectType, ObjectMapper mapper) {
        ObjectNode result = mapper.createObjectNode();
        List<String> fieldValues = FieldSplitter.splitFields(content);

        // Ensure we have enough values for all fields in the schema
        for (int i = 0; i < objectType.children.size(); i++) {
            ASTNode field = objectType.children.get(i);
            String fieldValue = "";

            if (i < fieldValues.size()) {
                fieldValue = fieldValues.get(i);
            }

            if (field.type == NodeType.OBJECT) {
                String objContent = fieldValue;
                if (objContent.startsWith(String.valueOf(NESTED_START))
                        && objContent.endsWith(String.valueOf(NESTED_END))) {
                    objContent = objContent.substring(1, objContent.length() - 1);
                }
                ObjectNode nested = parseObjectContent(objContent, field, mapper);
                result.set(field.name, nested);
            }
            else if (field.type == NodeType.ARRAY) {
                ArrayNode arr = parseArrayValue(fieldValue, field.elementType, mapper);
                result.set(field.name, arr);
            }
            else {
                JsonNode nodeValue = parsePrimitiveValue(fieldValue, field, mapper);
                if (nodeValue != null || !field.isOptional) {
                    result.set(field.name, nodeValue);
                }
            }
        }

        return result;
    }



    /**
     * Parse array value
     * 
     * @param value The array value string
     * @param elementType The type of array elements
     * @param mapper ObjectMapper for creating JSON nodes
     * @return ArrayNode representing the parsed array
     */
    static ArrayNode parseArrayValue(String value, ASTNode elementType, ObjectMapper mapper) {
        ArrayNode array = mapper.createArrayNode();

        if (value.startsWith(String.valueOf(ARRAY_START)) && value.endsWith(String.valueOf(ARRAY_END))) {
            value = value.substring(1, value.length() - 1);
        }

        if (!value.isEmpty()) {
            if (elementType.type == NodeType.OBJECT) {
                // For arrays of objects
                List<String> objectElements = FieldSplitter.splitArrayOfObjects(value);
                for (String element : objectElements) {
                    String objContent = element;
                    if (objContent.startsWith(String.valueOf(NESTED_START))
                            && objContent.endsWith(String.valueOf(NESTED_END))) {
                        objContent = objContent.substring(1, objContent.length() - 1);
                    }
                    ObjectNode objNode = parseObjectContent(objContent, elementType, mapper);
                    array.add(objNode);
                }
            }
            else if (elementType.type == NodeType.ARRAY) {
                // For arrays of arrays
                List<String> arrayElements = FieldSplitter.splitArrayOfArrays(value);
                for (String element : arrayElements) {
                    ArrayNode nestedArray = parseArrayValue(element, elementType.elementType, mapper);
                    array.add(nestedArray);
                }
            }
            else {
                // For arrays of primitives
                String[] elements = value.split(String.valueOf(FIELD_DELIMITER));
                for (String element : elements) {
                    JsonNode elemNode = parsePrimitiveValue(element, elementType, mapper);
                    if (elemNode != null) {
                        array.add(elemNode);
                    }
                }
            }
        }

        return array;
    }



    /**
     * Parse primitive value
     * 
     * @param value The primitive value string
     * @param type The type definition
     * @param mapper ObjectMapper for creating JSON nodes
     * @return JsonNode representing the parsed value
     */
    static JsonNode parsePrimitiveValue(String value, ASTNode type, ObjectMapper mapper) {
        // Handle optional fields with empty values
        if (value == null || value.isEmpty()) {
            if (type.isOptional) {
                return mapper.nullNode();
            }
            // For non-optional empty values:
            if (type.dataType != null && type.dataType.equals("string")) {
                return mapper.getNodeFactory().textNode("");
            }
        }

        if (type.type == NodeType.ENUM) {
            // Restore escaped characters for enum comparison
            String restoredValue = restoreEscapedChars(value);
            if (type.enumValues.contains(restoredValue)) {
                return mapper.getNodeFactory().textNode(restoredValue);
            }
            throw new IllegalArgumentException("Invalid enum value: " + restoredValue);
        }

        // Restore escaped characters for non-enum values
        value = restoreEscapedChars(value);

        String dataType = type.dataType;
        if (dataType == null)
            return mapper.getNodeFactory().textNode(value);

        try {
            switch (dataType) {
                case "int":
                    if (value.isEmpty()) {
                        return mapper.getNodeFactory().numberNode(0);
                    }
                    return mapper.getNodeFactory().numberNode(Integer.parseInt(value));
                case "float":
                    if (value.isEmpty()) {
                        return mapper.getNodeFactory().numberNode(0.0f);
                    }
                    return mapper.getNodeFactory().numberNode(Float.parseFloat(value));
                case "bool":
                    if (value.isEmpty()) {
                        return mapper.getNodeFactory().booleanNode(false);
                    }
                    return mapper.getNodeFactory().booleanNode(Boolean.parseBoolean(value));
                case "date":
                    // Validate ISO 8601 date format: YYYY-MM-DD
                    if (!value.matches("\\d{4}-\\d{2}-\\d{2}")) {
                        throw new IllegalArgumentException("Invalid date format. Expected ISO 8601: YYYY-MM-DD");
                    }
                    return mapper.getNodeFactory().textNode(value);
                case "datetime":
                    // Validate ISO 8601 datetime format
                    if (!value.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{3})?(Z|[+-]\\d{2}:\\d{2})")) {
                        throw new IllegalArgumentException(
                                "Invalid datetime format. Expected ISO 8601: YYYY-MM-DDTHH:MM:SSZ");
                    }
                    return mapper.getNodeFactory().textNode(value);
                case "string":
                default:
                    return mapper.getNodeFactory().textNode(value);
            }
        }
        catch (NumberFormatException e) {
            // If parsing fails and field is optional, return null
            if (type.isOptional) {
                return mapper.nullNode();
            }
            throw e;
        }
    }
}