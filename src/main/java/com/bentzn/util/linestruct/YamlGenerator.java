package com.bentzn.util.linestruct;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import com.bentzn.util.linestruct.LineStructParser.ASTNode;
import com.bentzn.util.linestruct.LineStructParser.Schema;

/**
 * Generator for YAML schema operations with LineStruct format.
 * 
 * This class handles bidirectional conversion between LineStruct schemas and
 * YAML schema definitions.
 * 
 * @author bentzn/Grok/Claude
 */
public class YamlGenerator {

    /**
     * Generate YAML schema from LineStruct format.
     * 
     * Takes a LineStruct string and generates a YAML representation of its
     * schema.
     * 
     * @param lineStruct The LineStruct formatted string
     * @return YAML string representing the schema
     * @throws RuntimeException if the LineStruct format is invalid
     */
    public static String generateYaml(String lineStruct) {
        try {
            if (lineStruct == null || lineStruct.trim().isEmpty()) {
                throw new IllegalArgumentException("LineStruct input is null or empty");
            }

            String[] lines = lineStruct.trim().split("\n");

            if (lines.length < 2) {
                throw new IllegalArgumentException(
                        "Invalid LineStruct format: minimum 2 lines required (EntityName and schema)");
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

            // Build YAML
            StringBuilder yaml = new StringBuilder();
            yaml.append("entityName: ").append(entityName).append("\n");
            yaml.append("fields:\n");

            // Generate YAML for each field
            for (ASTNode field : schema.root.children) {
                generateYamlField(yaml, field, 1);
            }

            return yaml.toString();

        }
        catch (Exception e) {
            throw new RuntimeException("Error generating YAML from LineStruct: " + e.getMessage(), e);
        }
    }



    /**
     * Generate LineStruct schema line from YAML schema definition.
     * 
     * Takes a YAML schema definition and converts it to the LineStruct schema
     * line format.
     * 
     * @param yaml YAML schema definition
     * @return LineStruct schema line starting with "schema:"
     * @throws RuntimeException if YAML parsing fails or schema is invalid
     */
    @SuppressWarnings("unchecked")
    public static String generateSchema(String yaml) {
        try {
            if (yaml == null || yaml.trim().isEmpty()) {
                throw new IllegalArgumentException("YAML input is null or empty");
            }

            // Parse YAML
            Yaml yamlParser = new Yaml();
            Object yamlData = yamlParser.load(yaml);

            if (yamlData == null) {
                throw new IllegalArgumentException("YAML schema is empty or invalid");
            }

            Map<String, Object> yamlMap;
            if (yamlData instanceof Map) {
                yamlMap = (Map<String, Object>) yamlData;
            }
            else {
                throw new IllegalArgumentException("YAML must be a Map structure");
            }

            // Extract fields
            Map<String, Object> fields = extractFields(yamlMap);

            // Build schema line
            StringBuilder schema = new StringBuilder("schema:");
            boolean first = true;

            for (Map.Entry<String, Object> entry : fields.entrySet()) {
                if (!first) {
                    schema.append(CharacterUtils.FIELD_DELIMITER);
                }
                first = false;

                String fieldName = entry.getKey();
                Object fieldDef = entry.getValue();

                schema.append(fieldName).append(":");
                schema.append(convertFieldToLineStruct(fieldDef));
            }

            return schema.toString();

        }
        catch (Exception e) {
            throw new RuntimeException("Error converting YAML to LineStruct schema: " + e.getMessage(), e);
        }
    }



    /**
     * Generate YAML for a single field with proper indentation
     */
    private static void generateYamlField(StringBuilder yaml, ASTNode field, int indentLevel) {
        String indent = getIndent(indentLevel);
        String fieldIndent = getIndent(indentLevel + 1);

        yaml.append(indent).append(field.name).append(":\n");

        switch (field.type) {
            case PRIMITIVE:
                yaml.append(fieldIndent).append("type: ").append(field.dataType).append("\n");
                yaml.append(fieldIndent).append("required: ").append(!field.isOptional).append("\n");
                break;

            case ARRAY:
                yaml.append(fieldIndent).append("type: array\n");
                yaml.append(fieldIndent).append("required: ").append(!field.isOptional).append("\n");
                if (field.elementType != null) {
                    yaml.append(fieldIndent).append("elementType:\n");
                    generateYamlType(yaml, field.elementType, indentLevel + 2);
                }
                break;

            case OBJECT:
                yaml.append(fieldIndent).append("type: object\n");
                yaml.append(fieldIndent).append("required: ").append(!field.isOptional).append("\n");
                if (!field.children.isEmpty()) {
                    yaml.append(fieldIndent).append("properties:\n");
                    for (ASTNode childField : field.children) {
                        generateYamlField(yaml, childField, indentLevel + 2);
                    }
                }
                break;

            case ENUM:
                yaml.append(fieldIndent).append("type: enum\n");
                yaml.append(fieldIndent).append("values: [");
                boolean first = true;
                for (String value : field.enumValues) {
                    if (!first)
                        yaml.append(", ");
                    first = false;
                    if (needsQuoting(value)) {
                        yaml.append("\"").append(escapeYamlString(value)).append("\"");
                    }
                    else {
                        yaml.append(value);
                    }
                }
                yaml.append("]\n");
                yaml.append(fieldIndent).append("required: ").append(!field.isOptional).append("\n");
                break;
            case OPTIONAL:
                throw new IllegalArgumentException();
        }

        // Add description if present
        if (field.description != null && !field.description.isEmpty()) {
            yaml.append(fieldIndent).append("description: \"").append(escapeYamlString(field.description))
                    .append("\"\n");
        }
    }



    /**
     * Generate YAML for a type (used for array element types)
     */
    private static void generateYamlType(StringBuilder yaml, ASTNode type, int indentLevel) {
        String indent = getIndent(indentLevel);

        switch (type.type) {
            case PRIMITIVE:
                yaml.append(indent).append("type: ").append(type.dataType).append("\n");
                break;

            case ARRAY:
                yaml.append(indent).append("type: array\n");
                if (type.elementType != null) {
                    yaml.append(indent).append("elementType:\n");
                    generateYamlType(yaml, type.elementType, indentLevel + 1);
                }
                break;

            case OBJECT:
                yaml.append(indent).append("type: object\n");
                if (!type.children.isEmpty()) {
                    yaml.append(indent).append("properties:\n");
                    for (ASTNode childField : type.children) {
                        generateYamlField(yaml, childField, indentLevel + 1);
                    }
                }
                break;

            case ENUM:
                yaml.append(indent).append("type: enum\n");
                yaml.append(indent).append("values: [");
                boolean first = true;
                for (String value : type.enumValues) {
                    if (!first)
                        yaml.append(", ");
                    first = false;
                    if (needsQuoting(value)) {
                        yaml.append("\"").append(escapeYamlString(value)).append("\"");
                    }
                    else {
                        yaml.append(value);
                    }
                }
                yaml.append("]\n");
                break;
            case OPTIONAL:
                throw new IllegalArgumentException();
        }
    }



    /**
     * Extract fields from YAML structure
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractFields(Map<String, Object> yamlMap) {
        // Check for 'fields' or 'properties' key
        if (yamlMap.containsKey("fields")) {
            Object fieldsObj = yamlMap.get("fields");
            if (fieldsObj instanceof Map) {
                return (Map<String, Object>) fieldsObj;
            }
        }

        if (yamlMap.containsKey("properties")) {
            Object propsObj = yamlMap.get("properties");
            if (propsObj instanceof Map) {
                return (Map<String, Object>) propsObj;
            }
        }

        // If no fields/properties key, check if the map itself contains field
        // definitions
        Map<String, Object> fields = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : yamlMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Skip known metadata keys
            if ((key.equals("entityName") || key.equals("entity")) && !(value instanceof Map)) {
                continue;
            }

            // Special handling for "name" - only skip if it's a simple string
            // (metadata)
            if (key.equals("name") && value instanceof String) {
                continue;
            }

            // Check if this looks like a field definition
            if (value instanceof Map) {
                Map<String, Object> potentialField = (Map<String, Object>) value;
                if (potentialField.containsKey("type") || potentialField.containsKey("dataType")
                        || potentialField.containsKey("required") || potentialField.containsKey("optional")
                        || potentialField.containsKey("properties") || potentialField.containsKey("values")
                        || potentialField.containsKey("elementType")) {
                    fields.put(key, value);
                }
            }
            else if (value instanceof String && !key.equals("entityName") && !key.equals("entity")) {
                // Simple type definition
                fields.put(key, value);
            }
        }

        return fields;
    }



    /**
     * Convert a field definition from YAML to LineStruct format
     */
    @SuppressWarnings("unchecked")
    private static String convertFieldToLineStruct(Object fieldDef) {
        if (fieldDef == null) {
            return "string";
        }

        // Simple string type
        if (fieldDef instanceof String) {
            String typeStr = (String) fieldDef;
            return typeStr.replace("?", "");
        }

        // Complex field definition
        if (fieldDef instanceof Map) {
            Map<String, Object> fieldMap = (Map<String, Object>) fieldDef;

            String type = extractType(fieldMap);
            boolean required = extractRequired(fieldMap);

            // Handle different types
            if ("enum".equals(type)) {
                return convertEnumToLineStruct(fieldMap, required);
            }
            else if ("array".equals(type) || "list".equals(type)) {
                return convertArrayToLineStruct(fieldMap, required);
            }
            else if ("object".equals(type) || "map".equals(type)) {
                return convertObjectToLineStruct(fieldMap, required);
            }
            else {
                return convertPrimitiveToLineStruct(type, required);
            }
        }

        return "string";
    }



    /**
     * Extract type from field map
     */
    private static String extractType(Map<String, Object> fieldMap) {
        if (fieldMap.containsKey("type")) {
            return String.valueOf(fieldMap.get("type")).toLowerCase();
        }
        if (fieldMap.containsKey("dataType")) {
            return String.valueOf(fieldMap.get("dataType")).toLowerCase();
        }
        return "string";
    }



    /**
     * Extract required flag from field map
     */
    private static boolean extractRequired(Map<String, Object> fieldMap) {
        if (fieldMap.containsKey("required")) {
            Object required = fieldMap.get("required");
            if (required instanceof Boolean) {
                return (Boolean) required;
            }
            if (required instanceof String) {
                return Boolean.parseBoolean((String) required);
            }
        }
        if (fieldMap.containsKey("optional")) {
            Object optional = fieldMap.get("optional");
            if (optional instanceof Boolean) {
                return !(Boolean) optional;
            }
            if (optional instanceof String) {
                return !Boolean.parseBoolean((String) optional);
            }
        }
        return true;
    }



    /**
     * Convert primitive type to LineStruct format
     */
    private static String convertPrimitiveToLineStruct(String type, boolean required) {
        String baseType = type;

        // Normalize type names
        if (type.contains("str") || type.contains("text")) {
            baseType = "string";
        }
        else if (type.contains("int") && !type.contains("float")) {
            baseType = "int";
        }
        else if (type.contains("float") || type.contains("double") || type.contains("decimal")) {
            baseType = "float";
        }
        else if (type.contains("bool")) {
            baseType = "bool";
        }
        else if (type.equals("date")) {
            baseType = "date";
        }
        else if (type.equals("datetime") || type.contains("timestamp")) {
            baseType = "datetime";
        }

        // If not required, append ?
        if (!required) {
            return baseType + "?";
        }
        return baseType;
    }



    /**
     * Convert enum to LineStruct format
     */
    private static String convertEnumToLineStruct(Map<String, Object> fieldMap, boolean required) {
        StringBuilder result = new StringBuilder("{");

        if (fieldMap.containsKey("values")) {
            Object valuesObj = fieldMap.get("values");
            List<String> values = new ArrayList<>();

            if (valuesObj instanceof List) {
                for (Object value : (List<?>) valuesObj) {
                    values.add(String.valueOf(value));
                }
            }
            else if (valuesObj instanceof String) {
                String valStr = (String) valuesObj;
                values.addAll(Arrays.asList(valStr.split("\\|")));
            }

            result.append(String.join("|", values));
        }

        result.append("}");

        if (!required) {
            result.append("?");
        }
        return result.toString();
    }



    /**
     * Convert array to LineStruct format
     */
    private static String convertArrayToLineStruct(Map<String, Object> fieldMap, boolean required) {
        StringBuilder result = new StringBuilder();
        result.append(CharacterUtils.ARRAY_START);

        if (fieldMap.containsKey("elementType")) {
            Object elementTypeObj = fieldMap.get("elementType");
            result.append(convertFieldToLineStruct(elementTypeObj));
        }
        else if (fieldMap.containsKey("items")) {
            Object itemsObj = fieldMap.get("items");
            result.append(convertFieldToLineStruct(itemsObj));
        }
        else {
            result.append("string");
        }

        result.append(CharacterUtils.ARRAY_END);

        if (!required) {
            result.append("?");
        }
        return result.toString();
    }



    /**
     * Convert object to LineStruct format
     */
    @SuppressWarnings("unchecked")
    private static String convertObjectToLineStruct(Map<String, Object> fieldMap, boolean required) {
        StringBuilder result = new StringBuilder();
        result.append(CharacterUtils.NESTED_START);

        Map<String, Object> properties = null;

        if (fieldMap.containsKey("properties")) {
            Object propsObj = fieldMap.get("properties");
            if (propsObj instanceof Map) {
                properties = (Map<String, Object>) propsObj;
            }
        }
        else if (fieldMap.containsKey("fields")) {
            Object fieldsObj = fieldMap.get("fields");
            if (fieldsObj instanceof Map) {
                properties = (Map<String, Object>) fieldsObj;
            }
        }

        if (properties != null && !properties.isEmpty()) {
            boolean first = true;
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                if (!first) {
                    result.append(CharacterUtils.FIELD_DELIMITER);
                }
                first = false;

                result.append(entry.getKey()).append(":");
                result.append(convertFieldToLineStruct(entry.getValue()));
            }
        }

        result.append(CharacterUtils.NESTED_END);

        if (!required) {
            result.append("?");
        }
        return result.toString();
    }



    /**
     * Generate indentation string for YAML
     */
    private static String getIndent(int level) {
        StringBuilder indent = new StringBuilder();
        for (int i = 0; i < level; i++) {
            indent.append("  ");
        }
        return indent.toString();
    }



    /**
     * Check if a string needs quoting in YAML
     */
    private static boolean needsQuoting(String value) {
        if (value.isEmpty())
            return true;
        if (value.startsWith(" ") || value.endsWith(" "))
            return true;
        if (value.contains(":") || value.contains("#") || value.contains("|") || value.contains(">")
                || value.contains("\"") || value.contains("'") || value.contains("\n") || value.contains("\r")
                || value.contains("\t"))
            return true;
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false") || value.equalsIgnoreCase("yes")
                || value.equalsIgnoreCase("no") || value.equalsIgnoreCase("null"))
            return true;
        try {
            Double.parseDouble(value);
            return true;
        }
        catch (NumberFormatException e) {
            // Not a number
        }
        return false;
    }



    /**
     * Escape special characters in YAML strings
     */
    private static String escapeYamlString(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t",
                "\\t");
    }
}