package com.bentzn.util.linestruct;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

import com.bentzn.util.linestruct.LineStructParser.ASTNode;
import com.bentzn.util.linestruct.LineStructParser.NodeType;
import com.bentzn.util.linestruct.LineStructParser.Schema;

/**
 * Validator for LineStruct format and data.
 * 
 * This class provides comprehensive validation of LineStruct data against
 * schemas, including validation against YAML schema definitions.
 * 
 * @author bentzn/Grok/Claude
 */
class Validator {

    /**
     * Validate LineStruct data against a YAML schema definition.
     * 
     * @param yaml YAML schema definition describing expected fields and types
     * @param lineStruct LineStruct data to validate
     * @return null if valid, otherwise a descriptive error message
     */
    static String isValid(String yaml, String lineStruct) {
        if (yaml == null || yaml.trim().isEmpty()) {
            return "YAML schema is null or empty";
        }

        if (lineStruct == null || lineStruct.trim().isEmpty()) {
            return "LineStruct data is null or empty";
        }

        try {
            // Parse the LineStruct to get its schema
            String[] lines = lineStruct.trim().split("\n");
            if (lines.length < 2) {
                return "LineStruct format invalid: minimum 2 lines required (EntityName and schema)";
            }

            if (!lines[1].startsWith("schema:")) {
                return "LineStruct format invalid: second line must start with 'schema:'";
            }

            Schema lineStructSchema;
            try {
                lineStructSchema = SchemaParser.parseSchema(lines[1]);
            }
            catch (Exception e) {
                return "LineStruct schema parsing failed: " + e.getMessage();
            }

            // Parse YAML schema
            Yaml yamlParser = new Yaml();
            Object yamlData;
            try {
                yamlData = yamlParser.load(yaml);
            }
            catch (Exception e) {
                return "YAML parsing failed: " + e.getMessage();
            }

            if (yamlData == null) {
                return "YAML schema is empty or invalid";
            }

            // Validate schema against YAML
            String validationResult = validateSchemaAgainstYamlWithDetails(lineStructSchema.root, yamlData);
            if (validationResult != null) {
                return validationResult;
            }

            // Validate each data line
            for (int i = 2; i < lines.length; i++) {
                if (!lines[i].trim().isEmpty()) {
                    String processedLine = CharacterUtils.replaceEscapedChars(lines[i]);
                    String dataValidationResult = validateDataLineWithDetails(processedLine,
                            lineStructSchema.root,
                            i + 1);
                    if (dataValidationResult != null) {
                        return dataValidationResult;
                    }
                }
            }

            return null; // Valid

        }
        catch (Exception e) {
            return "Validation failed with unexpected error: " + e.getMessage();
        }
    }



    /**
     * Validate if a string is valid LineStruct format.
     * 
     * @param lineStruct The string to validate
     * @return true if valid LineStruct format, false otherwise
     */
    static boolean isValidLineStructFormat(String lineStruct) {
        if (lineStruct == null || lineStruct.trim().isEmpty()) {
            return false;
        }

        try {
            String[] lines = lineStruct.trim().split("\n");

            if (lines.length < 2) {
                return false;
            }

            // Parse and validate schema
            Schema schema = SchemaParser.parseSchema(lines[1]);
            if (schema.root == null || schema.root.children.isEmpty()) {
                return false;
            }

            // Validate each data line
            for (int i = 2; i < lines.length; i++) {
                if (!lines[i].trim().isEmpty()) {
                    String processedLine = CharacterUtils.replaceEscapedChars(lines[i]);
                    if (!validateDataLine(processedLine, schema.root)) {
                        return false;
                    }
                }
            }

            return true;

        }
        catch (Exception e) {
            return false;
        }
    }



    /**
     * Validate data line against schema
     */
    private static boolean validateDataLine(String dataLine, ASTNode schema) {
        try {
            List<String> values = FieldSplitter.splitDataValues(dataLine);

            if (values.size() > schema.children.size()) {
                return false;
            }

            if (values.size() < schema.children.size()) {
                // Check if missing fields are all optional
                for (int i = values.size(); i < schema.children.size(); i++) {
                    if (!schema.children.get(i).isOptional) {
                        return false;
                    }
                }
            }

            // Validate each value
            for (int i = 0; i < values.size(); i++) {
                ASTNode field = schema.children.get(i);
                String value = values.get(i);

                if (!validateValue(value, field)) {
                    return false;
                }
            }

            return true;
        }
        catch (Exception e) {
            return false;
        }
    }



    /**
     * Validate a value against its field type
     */
    private static boolean validateValue(String value, ASTNode field) {
        // Restore escaped characters for validation
        value = CharacterUtils.restoreEscapedChars(value);

        if (value == null || value.isEmpty()) {
            return field.isOptional;
        }

        try {
            switch (field.type) {
                case PRIMITIVE:
                    return validatePrimitiveValue(value, field.dataType);

                case ENUM:
                    return field.enumValues.contains(value);

                case ARRAY:
                    if (value.isEmpty() && field.isOptional) {
                        return true;
                    }
                    return value.startsWith(String.valueOf(CharacterUtils.ARRAY_START))
                            && value.endsWith(String.valueOf(CharacterUtils.ARRAY_END));

                case OBJECT:
                    if (value.isEmpty() && field.isOptional) {
                        return true;
                    }
                    return value.startsWith(String.valueOf(CharacterUtils.NESTED_START))
                            && value.endsWith(String.valueOf(CharacterUtils.NESTED_END));

                default:
                    return false;
            }
        }
        catch (Exception e) {
            return false;
        }
    }



    /**
     * Validate primitive value against its type
     */
    private static boolean validatePrimitiveValue(String value, String dataType) {
        if (dataType == null) {
            return true; // Default to string
        }

        try {
            switch (dataType) {
                case "int":
                    Integer.parseInt(value);
                    return true;
                case "float":
                    Float.parseFloat(value);
                    return true;
                case "bool":
                    return value.equals("true") || value.equals("false");
                case "date":
                    return value.matches("\\d{4}-\\d{2}-\\d{2}");
                case "datetime":
                    return value.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{3})?(Z|[+-]\\d{2}:\\d{2})");
                case "string":
                    return true;
                default:
                    return false;
            }
        }
        catch (NumberFormatException e) {
            return false;
        }
    }



    /**
     * Validate data line with detailed error messages
     */
    private static String validateDataLineWithDetails(String dataLine, ASTNode schema, int lineNumber) {
        try {
            List<String> values = FieldSplitter.splitDataValues(dataLine);

            if (values.size() > schema.children.size()) {
                return "Line " + lineNumber + ": Too many values (" + values.size() + ") for schema fields ("
                        + schema.children.size() + ")";
            }

            if (values.size() < schema.children.size()) {
                for (int i = values.size(); i < schema.children.size(); i++) {
                    if (!schema.children.get(i).isOptional) {
                        return "Line " + lineNumber + ": Missing required field '" + schema.children.get(i).name
                                + "' at position " + (i + 1);
                    }
                }
            }

            for (int i = 0; i < values.size(); i++) {
                ASTNode field = schema.children.get(i);
                String value = values.get(i);

                String valueValidationResult = validateValueWithDetails(value, field, field.name, lineNumber);
                if (valueValidationResult != null) {
                    return valueValidationResult;
                }
            }

            return null;
        }
        catch (Exception e) {
            return "Line " + lineNumber + ": Data parsing failed - " + e.getMessage();
        }
    }



    /**
     * Validate a value with detailed error message
     */
    private static String validateValueWithDetails(String value, ASTNode field, String fieldName, int lineNumber) {
        value = CharacterUtils.restoreEscapedChars(value);

        if (value == null || value.isEmpty()) {
            if (!field.isOptional) {
                return "Line " + lineNumber + ": Field '" + fieldName + "' is required but is empty";
            }
            return null;
        }

        try {
            switch (field.type) {
                case PRIMITIVE:
                    return validatePrimitiveValueWithDetails(value, field.dataType, fieldName, lineNumber);

                case ENUM:
                    if (!field.enumValues.contains(value)) {
                        return "Line " + lineNumber + ": Field '" + fieldName + "' has invalid enum value '" + value
                                + "'. Valid values are: " + field.enumValues;
                    }
                    return null;

                case ARRAY:
                    if (value.isEmpty() && field.isOptional) {
                        return null;
                    }
                    if (!value.startsWith(String.valueOf(CharacterUtils.ARRAY_START))
                            || !value.endsWith(String.valueOf(CharacterUtils.ARRAY_END))) {
                        return "Line " + lineNumber + ": Field '" + fieldName
                                + "' must be an array (surrounded by « »)";
                    }
                    return null;

                case OBJECT:
                    if (value.isEmpty() && field.isOptional) {
                        return null;
                    }
                    if (!value.startsWith(String.valueOf(CharacterUtils.NESTED_START))
                            || !value.endsWith(String.valueOf(CharacterUtils.NESTED_END))) {
                        return "Line " + lineNumber + ": Field '" + fieldName
                                + "' must be an object (surrounded by ‹ ›)";
                    }
                    return null;

                default:
                    return "Line " + lineNumber + ": Field '" + fieldName + "' has unknown type";
            }
        }
        catch (Exception e) {
            return "Line " + lineNumber + ": Field '" + fieldName + "' validation failed - " + e.getMessage();
        }
    }



    /**
     * Validate primitive value with detailed error message
     */
    private static String validatePrimitiveValueWithDetails(String value,
            String dataType,
            String fieldName,
            int lineNumber) {
        if (dataType == null) {
            return null;
        }

        try {
            switch (dataType) {
                case "int":
                    try {
                        Integer.parseInt(value);
                        return null;
                    }
                    catch (NumberFormatException e) {
                        return "Line " + lineNumber + ": Field '" + fieldName + "' must be an integer, got '" + value
                                + "'";
                    }
                case "float":
                    try {
                        Float.parseFloat(value);
                        return null;
                    }
                    catch (NumberFormatException e) {
                        return "Line " + lineNumber + ": Field '" + fieldName + "' must be a float, got '" + value
                                + "'";
                    }
                case "bool":
                    if (!value.equals("true") && !value.equals("false")) {
                        return "Line " + lineNumber + ": Field '" + fieldName + "' must be 'true' or 'false', got '"
                                + value + "'";
                    }
                    return null;
                case "date":
                    if (!value.matches("\\d{4}-\\d{2}-\\d{2}")) {
                        return "Line " + lineNumber + ": Field '" + fieldName
                                + "' must be in ISO 8601 date format (YYYY-MM-DD), got '" + value + "'";
                    }
                    return null;
                case "datetime":
                    if (!value.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{3})?(Z|[+-]\\d{2}:\\d{2})")) {
                        return "Line " + lineNumber + ": Field '" + fieldName
                                + "' must be in ISO 8601 datetime format (YYYY-MM-DDTHH:MM:SSZ), got '" + value + "'";
                    }
                    return null;
                case "string":
                    return null;
                default:
                    return "Line " + lineNumber + ": Field '" + fieldName + "' has unknown primitive type '" + dataType
                            + "'";
            }
        }
        catch (Exception e) {
            return "Line " + lineNumber + ": Field '" + fieldName + "' validation failed - " + e.getMessage();
        }
    }



    /**
     * Validate schema against YAML with details
     */
    @SuppressWarnings("unchecked")
    private static String validateSchemaAgainstYamlWithDetails(ASTNode lineStructRoot, Object yamlData) {
        Map<String, Object> yamlSchema;

        if (yamlData instanceof Map) {
            yamlSchema = (Map<String, Object>) yamlData;
        }
        else if (yamlData instanceof List) {
            yamlSchema = new HashMap<>();
            List<?> yamlList = (List<?>) yamlData;
            for (Object item : yamlList) {
                if (item instanceof Map) {
                    Map<String, Object> itemMap = (Map<String, Object>) item;
                    String name = String.valueOf(itemMap.get("name"));
                    yamlSchema.put(name, itemMap);
                }
            }
        }
        else {
            return "YAML schema must be either a Map or List structure";
        }

        // Extract fields
        Map<String, Object> fields = extractFields(yamlSchema);

        // Check field compatibility
        for (ASTNode field : lineStructRoot.children) {
            Object yamlField = fields.get(field.name);
            if (yamlField == null && !field.isOptional) {
                return "Required field '" + field.name + "' from LineStruct is not defined in YAML schema";
            }
            if (yamlField != null) {
                String compatibilityResult = checkYamlTypeCompatibility(field, yamlField);
                if (compatibilityResult != null) {
                    return "Field '" + field.name + "' type mismatch: " + compatibilityResult;
                }
            }
        }

        // Check required fields from YAML
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            String fieldName = entry.getKey();
            Object fieldDef = entry.getValue();

            boolean isRequired = extractRequired(fieldDef);

            if (isRequired) {
                boolean found = false;
                for (ASTNode field : lineStructRoot.children) {
                    if (field.name.equals(fieldName)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return "Required field '" + fieldName + "' from YAML schema is missing in LineStruct";
                }
            }
        }

        return null;
    }



    /**
     * Extract fields from YAML schema
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractFields(Map<String, Object> yamlSchema) {
        Map<String, Object> fields = new HashMap<>();

        if (yamlSchema.containsKey("fields")) {
            Object fieldsObj = yamlSchema.get("fields");
            if (fieldsObj instanceof Map) {
                fields = (Map<String, Object>) fieldsObj;
            }
        }
        else if (yamlSchema.containsKey("properties")) {
            Object propsObj = yamlSchema.get("properties");
            if (propsObj instanceof Map) {
                fields = (Map<String, Object>) propsObj;
            }
        }
        else {
            fields = yamlSchema;
        }

        return fields;
    }



    /**
     * Extract required flag from field definition
     */
    @SuppressWarnings("unchecked")
    private static boolean extractRequired(Object fieldDef) {
        if (fieldDef instanceof Map) {
            Map<String, Object> fieldMap = (Map<String, Object>) fieldDef;
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
            }
        }
        else if (fieldDef instanceof String) {
            String typeStr = (String) fieldDef;
            return !typeStr.endsWith("?");
        }
        return true;
    }



    /**
     * Check YAML type compatibility
     */
    @SuppressWarnings("unchecked")
    private static String checkYamlTypeCompatibility(ASTNode field, Object yamlField) {
        String yamlType = null;

        if (yamlField instanceof String) {
            yamlType = (String) yamlField;
        }
        else if (yamlField instanceof Map) {
            Map<String, Object> fieldMap = (Map<String, Object>) yamlField;
            if (fieldMap.containsKey("type")) {
                yamlType = String.valueOf(fieldMap.get("type"));
            }
            else if (fieldMap.containsKey("dataType")) {
                yamlType = String.valueOf(fieldMap.get("dataType"));
            }
        }

        if (yamlType == null || "null".equals(yamlType)) {
            return "YAML field has no type defined";
        }

        yamlType = yamlType.toLowerCase().trim().replace("?", "");

        if (field.type == NodeType.PRIMITIVE) {
            String fieldType = field.dataType.toLowerCase();
            boolean compatible = isTypeCompatible(fieldType, yamlType);

            if (!compatible) {
                return "LineStruct type '" + fieldType + "' is not compatible with YAML type '" + yamlType + "'";
            }
        }
        else if (field.type == NodeType.ARRAY) {
            if (!yamlType.contains("array") && !yamlType.contains("list") && !yamlType.contains("[]")) {
                return "LineStruct array type is not compatible with YAML type '" + yamlType + "'";
            }
        }
        else if (field.type == NodeType.OBJECT) {
            if (!yamlType.contains("object") && !yamlType.contains("map") && !yamlType.contains("dict")) {
                return "LineStruct object type is not compatible with YAML type '" + yamlType + "'";
            }
        }
        else if (field.type == NodeType.ENUM) {
            if (!yamlType.contains("enum") && !yamlType.contains("|")) {
                return "LineStruct enum type is not compatible with YAML type '" + yamlType + "'";
            }
        }

        return null;
    }



    /**
     * Check if primitive types are compatible
     */
    private static boolean isTypeCompatible(String fieldType, String yamlType) {
        return fieldType.equals(yamlType)
                || (fieldType.equals("string") && (yamlType.contains("str") || yamlType.contains("text")))
                || (fieldType.equals("int") && (yamlType.contains("int") || yamlType.contains("number")))
                || (fieldType.equals("float") && (yamlType.contains("float") || yamlType.contains("double")
                        || yamlType.contains("decimal") || yamlType.contains("number")))
                || (fieldType.equals("bool") && yamlType.contains("bool"));
    }
}