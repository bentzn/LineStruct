package com.bentzn.util.linestruct;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.bentzn.util.linestruct.LineStructParser.ASTNode;
import com.bentzn.util.linestruct.LineStructParser.Schema;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Converter for JSON to LineStruct format.
 * 
 * This class handles conversion from JSON objects to LineStruct format,
 * including schema inference and data formatting.
 * 
 * @author bentzn/Grok/Claude
 */
class JsonConverter {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Convert JSON to LineStruct format.
     * 
     * Takes a JSON object with entityName and data array and converts it to
     * LineStruct format.
     * 
     * @param json JSON string with entityName and data array
     * @return LineStruct formatted string
     * @throws RuntimeException if JSON parsing fails or required fields are
     *             missing
     */
    static String fromJson(String json) {
        try {
            JsonNode root = mapper.readTree(json);

            // Validate required fields
            if (!root.has("entityName")) {
                throw new IllegalArgumentException("Missing required field: entityName");
            }
            if (!root.has("data")) {
                throw new IllegalArgumentException("Missing required field: data");
            }

            StringBuilder result = new StringBuilder();

            // Get entity name
            String entityName = root.get("entityName").asText("Entity");
            result.append("EntityName:").append(entityName).append("\n");

            // Build schema from ALL data objects
            JsonNode dataArray = root.get("data");
            if (!dataArray.isArray()) {
                throw new IllegalArgumentException("Field 'data' must be an array");
            }

            if (dataArray.size() > 0) {
                String schemaLine = buildSchemaFromJsonArray(dataArray);
                result.append("schema:").append(schemaLine).append("\n");

                // Convert each data object
                for (JsonNode dataObject : dataArray) {
                    String dataLine = buildDataLineFromJson(dataObject, schemaLine);
                    result.append(dataLine).append("\n");
                }
            }
            else {
                // Empty data array
                result.append("schema:\n");
            }

            return result.toString();

        }
        catch (IllegalArgumentException e) {
            throw new RuntimeException("Error converting JSON to LineStruct: " + e.getMessage(), e);
        }
        catch (Exception e) {
            throw new RuntimeException("Error converting JSON to LineStruct: " + e.getMessage(), e);
        }
    }



    /**
     * Build schema from multiple JSON records to better detect optional fields
     */
    static String buildSchemaFromJsonArray(JsonNode dataArray) {
        if (!dataArray.isArray() || dataArray.size() == 0) {
            return "";
        }

        // Collect field info from all records
        Map<String, String> fieldTypes = new LinkedHashMap<>();
        Map<String, Boolean> fieldOptional = new LinkedHashMap<>();

        // Get field order from first record
        JsonNode firstRecord = dataArray.get(0);
        firstRecord.fieldNames().forEachRemaining(name -> {
            fieldTypes.put(name, null);
            fieldOptional.put(name, false);
        });

        // Analyze all records
        for (JsonNode record : dataArray) {
            for (String fieldName : fieldTypes.keySet()) {
                JsonNode value = record.get(fieldName);

                if (value == null || value.isNull()) {
                    // Field is null in at least one record - mark as optional
                    fieldOptional.put(fieldName, true);
                }
                else if (fieldTypes.get(fieldName) == null) {
                    // Infer type from first non-null value
                    fieldTypes.put(fieldName, inferType(value));
                }
            }
        }

        // Build schema string
        StringBuilder schema = new StringBuilder();
        boolean first = true;

        for (Map.Entry<String, String> entry : fieldTypes.entrySet()) {
            if (!first)
                schema.append(CharacterUtils.FIELD_DELIMITER);
            first = false;

            String fieldName = entry.getKey();
            String fieldType = entry.getValue();
            boolean isOptional = fieldOptional.get(fieldName);

            schema.append(fieldName).append(":");

            if (fieldType == null) {
                fieldType = "string"; // Default type for always-null fields
            }

            if (isOptional) {
                schema.append("<").append(fieldType).append(">?");
            }
            else {
                schema.append(fieldType);
            }
        }

        return schema.toString();
    }



    /**
     * Build data line from JSON with schema awareness
     */
    static String buildDataLineFromJson(JsonNode node, String schemaLine) {
        // Parse schema to know field types and which are optional
        List<String> fieldNames = new ArrayList<>();
        List<Boolean> fieldOptional = new ArrayList<>();
        List<ASTNode> fieldNodes = new ArrayList<>();

        // Parse the schema properly to get field types
        Schema schema = SchemaParser.parseSchema("schema:" + schemaLine);
        for (ASTNode field : schema.root.children) {
            fieldNames.add(field.name);
            fieldOptional.add(field.isOptional);
            fieldNodes.add(field);
        }

        List<String> values = new ArrayList<>();
        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            JsonNode fieldValue = node.get(fieldName);
            ASTNode fieldNode = fieldNodes.get(i);
            values.add(formatValueWithType(fieldValue, fieldNode));
        }

        // Find last non-empty value
        int lastRequired = -1;
        for (int i = values.size() - 1; i >= 0; i--) {
            if (!fieldOptional.get(i) || (values.get(i) != null && !values.get(i).isEmpty())) {
                lastRequired = i;
                break;
            }
        }

        // Build line up to last required or non-empty field
        StringBuilder line = new StringBuilder();
        for (int i = 0; i <= lastRequired; i++) {
            if (i > 0) {
                line.append(CharacterUtils.FIELD_DELIMITER);
            }
            line.append(values.get(i));
        }

        return line.toString();
    }



    /**
     * Format value for LineStruct with type awareness
     */
    private static String formatValueWithType(JsonNode value, ASTNode fieldNode) {
        if (value == null || value.isNull()) {
            return "";
        }

        // Handle different field types
        switch (fieldNode.type) {
            case ARRAY:
                StringBuilder arr = new StringBuilder();
                arr.append(CharacterUtils.ARRAY_START);
                boolean first = true;
                for (JsonNode elem : value) {
                    if (!first)
                        arr.append(CharacterUtils.FIELD_DELIMITER);
                    first = false;
                    if (fieldNode.elementType != null) {
                        arr.append(formatValueWithType(elem, fieldNode.elementType));
                    }
                    else {
                        arr.append(formatValue(elem));
                    }
                }
                arr.append(CharacterUtils.ARRAY_END);
                return arr.toString();

            case OBJECT:
                StringBuilder obj = new StringBuilder();
                obj.append(CharacterUtils.NESTED_START);
                boolean firstField = true;
                for (int i = 0; i < fieldNode.children.size(); i++) {
                    ASTNode childField = fieldNode.children.get(i);
                    JsonNode childValue = value.get(childField.name);

                    if (!firstField)
                        obj.append(CharacterUtils.FIELD_DELIMITER);
                    firstField = false;
                    obj.append(formatValueWithType(childValue, childField));
                }
                obj.append(CharacterUtils.NESTED_END);
                return obj.toString();

            case ENUM:
                String enumValue = value.asText();
                if (fieldNode.enumValues.contains(enumValue)) {
                    return enumValue;
                }
                return enumValue;

            case PRIMITIVE:
                String text = value.asText();
                if (CharacterUtils.needsEscaping(text)) {
                    return CharacterUtils.escapeValue(text);
                }
                return text;

            default:
                return formatValue(value);
        }
    }



    /**
     * Format value for LineStruct (generic)
     */
    private static String formatValue(JsonNode value) {
        if (value == null || value.isNull()) {
            return "";
        }
        if (value.isArray()) {
            StringBuilder arr = new StringBuilder();
            arr.append(CharacterUtils.ARRAY_START);
            boolean first = true;
            for (JsonNode elem : value) {
                if (!first)
                    arr.append(CharacterUtils.FIELD_DELIMITER);
                first = false;
                arr.append(formatValue(elem));
            }
            arr.append(CharacterUtils.ARRAY_END);
            return arr.toString();
        }
        if (value.isObject()) {
            StringBuilder obj = new StringBuilder();
            obj.append(CharacterUtils.NESTED_START);
            Iterator<JsonNode> elements = value.elements();
            boolean first = true;
            while (elements.hasNext()) {
                if (!first)
                    obj.append(CharacterUtils.FIELD_DELIMITER);
                first = false;
                obj.append(formatValue(elements.next()));
            }
            obj.append(CharacterUtils.NESTED_END);
            return obj.toString();
        }

        String text = value.asText();
        if (CharacterUtils.needsEscaping(text)) {
            return CharacterUtils.escapeValue(text);
        }
        return text;
    }



    /**
     * Infer type from JSON value
     */
    private static String inferType(JsonNode value) {
        if (value.isInt())
            return "int";
        if (value.isFloat() || value.isDouble())
            return "float";
        if (value.isBoolean())
            return "bool";
        if (value.isTextual())
            return "string";
        if (value.isArray()) {
            if (value.size() > 0) {
                return String.valueOf(CharacterUtils.ARRAY_START) + inferType(value.get(0)) + CharacterUtils.ARRAY_END;
            }
            return String.valueOf(CharacterUtils.ARRAY_START) + "string" + CharacterUtils.ARRAY_END;
        }
        if (value.isObject()) {
            return String.valueOf(CharacterUtils.NESTED_START) + buildSchemaFromJson(value) + CharacterUtils.NESTED_END;
        }
        return "string";
    }



    /**
     * Build schema string from JSON object
     */
    private static String buildSchemaFromJson(JsonNode node) {
        StringBuilder schema = new StringBuilder();
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        boolean first = true;

        while (fields.hasNext()) {
            if (!first)
                schema.append(CharacterUtils.FIELD_DELIMITER);
            first = false;

            Map.Entry<String, JsonNode> field = fields.next();
            schema.append(field.getKey()).append(":");

            JsonNode value = field.getValue();
            String type = inferType(value);

            // Check if this field appears to be optional
            if (value.isNull()) {
                if (!type.startsWith("<")) {
                    type = "<" + type + ">?";
                }
            }

            schema.append(type);
        }

        return schema.toString();
    }
}