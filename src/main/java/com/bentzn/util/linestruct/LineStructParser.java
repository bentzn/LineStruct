package com.bentzn.util.linestruct;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Main public interface for LineStruct format operations.
 * 
 * LineStruct is a UTF-8 encoded format for structured data with: - Line 1:
 * EntityName:<name> - Line 2: schema:<field definitions> - Lines 3+: Data rows
 * 
 * This class provides the public API for converting between LineStruct and
 * other formats, validating data, and generating code.
 * 
 * @author bentzn/Grok/Claude
 */
public class LineStructParser {

    // Data type enum
    public enum DataType {
        JSON, LINESTRUCT, OTHER
    }

    // AST Node Types
    public enum NodeType {
        PRIMITIVE, OBJECT, ARRAY, ENUM, OPTIONAL
    }

    // AST Node class
     static class ASTNode {
        public NodeType type;
        public String name;
        public String dataType; // For primitives
        public String description;
        public List<ASTNode> children; // For objects
        public ASTNode elementType; // For arrays
        public Set<String> enumValues; // For enums
        public boolean isOptional;

        public ASTNode(NodeType type) {
            this.type = type;
            this.children = new ArrayList<>();
            this.enumValues = new HashSet<>();
        }
    }

    // Schema parser result
     static class Schema {
        public String entityName;
        public ASTNode root;

        public Schema(String entityName, ASTNode root) {
            this.entityName = entityName;
            this.root = root;
        }
    }

    /**
     * Convert LineStruct format to JSON.
     * 
     * @param lineStruct The LineStruct formatted string
     * @return JSON string with structure: {"entityName":"...", "data":[...]}
     * @throws RuntimeException if the LineStruct format is invalid
     */
    public static String toJson(String lineStruct) {
        return JsonGenerator.toJson(lineStruct);
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
        return JsonGenerator.toJson(lineStruct, trim);
    }



    /**
     * Convert LineStruct format to JSON with a sample of up to 3 records.
     * 
     * @param lineStruct The LineStruct formatted string
     * @return JSON string with structure: {"entityName":"...", "data":[...]}
     *         containing up to 3 records
     * @throws RuntimeException if the LineStruct format is invalid
     */
    public static String toJsonSample(String lineStruct) {
        return JsonGenerator.toJsonSample(lineStruct);
    }



    /**
     * Convert JSON to LineStruct format.
     * 
     * @param json JSON string with entityName and data array
     * @return LineStruct formatted string
     * @throws RuntimeException if JSON parsing fails or required fields are
     *             missing
     */
    public static String fromJson(String json) {
        return JsonConverter.fromJson(json);
    }



    /**
     * Generate YAML schema from LineStruct format.
     * 
     * @param lineStruct The LineStruct formatted string
     * @return YAML string representing the schema
     * @throws RuntimeException if the LineStruct format is invalid
     */
    public static String generateYaml(String lineStruct) {
        return YamlGenerator.generateYaml(lineStruct);
    }



    /**
     * Generate LineStruct schema line from YAML schema definition.
     * 
     * @param yaml YAML schema definition
     * @return LineStruct schema line starting with "schema:"
     * @throws RuntimeException if YAML parsing fails or schema is invalid
     */
    public static String getSchema(String yaml) {
        return YamlGenerator.generateSchema(yaml);
    }



    /**
     * Generate Java POJO classes from LineStruct format.
     * 
     * @param lineStruct The LineStruct formatted string
     * @param packageName The Java package name for generated classes
     * @return Array of JavaClassDef objects containing filename and code for
     *         each generated class
     * @throws RuntimeException if the LineStruct format is invalid
     */
    public static JavaClassDef[] generateJava(String lineStruct, String packageName) {
        return JavaGenerator.generateJava(lineStruct, packageName);
    }



    /**
     * Validate LineStruct data against a YAML schema definition.
     * 
     * @param yaml YAML schema definition describing expected fields and types
     * @param lineStruct LineStruct data to validate
     * @return null if the LineStruct conforms to the YAML schema, otherwise a
     *         descriptive error message
     */
    public static String isValid(String yaml, String lineStruct) {
        return Validator.isValid(yaml, lineStruct);
    }



    /**
     * Determine the data type of a string.
     * 
     * @param data The string to analyze
     * @return DataType.JSON if valid JSON, DataType.LINESTRUCT if valid
     *         LineStruct, DataType.OTHER otherwise
     */
    public static DataType getDataType(String data) {
        return DataTypeDetector.getDataType(data);
    }



    /**
     * Validate if a string is valid JSON.
     * 
     * @param json The string to validate
     * @return true if the string is valid JSON, false otherwise
     */
    public static boolean isValidJson(String json) {
        return DataTypeDetector.isValidJson(json);
    }



    /**
     * Validate if a string is valid LineStruct format.
     * 
     * @param lineStruct The string to validate
     * @return true if valid LineStruct format with all data matching schema,
     *         false otherwise
     */
    public static boolean isValidLineStruct(String lineStruct) {
        return DataTypeDetector.isValidLineStruct(lineStruct);
    }



    /**
     * Pretty prints a LineStruct schema string.
     * 
     * @param strSchema The single-line schema string
     * @return The pretty-printed schema
     */
    public static String prettyPrintSchema(String strSchema) {
        return LineStructFormatter.prettyPrintSchema(strSchema);
    }



    /**
     * Replace escaped characters with temporary markers. Internal method
     * exposed for use by other classes in the package and tests.
     * 
     * @param input The input string with escape sequences
     * @return String with escape sequences replaced by temporary markers
     */
    static String replaceEscapedChars(String input) {
        return CharacterUtils.replaceEscapedChars(input);
    }



    /**
     * Restore escaped characters from temporary markers. Internal method
     * exposed for use by other classes in the package and tests.
     * 
     * @param input The input string with temporary markers
     * @return String with temporary markers replaced by actual characters
     */
    static String restoreEscapedChars(String input) {
        return CharacterUtils.restoreEscapedChars(input);
    }



    /**
     * Parse a data line according to schema. Internal method exposed for use by
     * other classes in the package and tests.
     * 
     * @param dataLine The data line to parse
     * @param schema The schema defining the structure
     * @param mapper ObjectMapper for creating JSON nodes
     * @return ObjectNode representing the parsed data
     */
    static ObjectNode parseDataLine(String dataLine, ASTNode schema, ObjectMapper mapper) {
        return DataParser.parseDataLine(dataLine, schema, mapper);
    }



    /**
     * Get the prompt describing the LineStruct format.
     * 
     * Reads the format specification from linestruct_description.txt in the
     * classpath root. This specification can be used to explain the format to
     * LLMs or for documentation.
     * 
     * @return The LineStruct format specification as a string
     * @throws RuntimeException if the description file cannot be found or read
     */
    public static String getPrompt() {
        try {
            // Read from classpath resource
            InputStream inputStream = LineStructParser.class.getClassLoader()
                    .getResourceAsStream("linestruct_description_for_LLM.txt");

            if (inputStream == null) {
                throw new RuntimeException("Could not find linestruct_description.txt in classpath");
            }

            try (Scanner scanner = new Scanner(inputStream, "UTF-8")) {
                StringBuilder content = new StringBuilder();
                while (scanner.hasNextLine()) {
                    content.append(scanner.nextLine());
                    if (scanner.hasNextLine()) {
                        content.append("\n");
                    }
                }
                return content.toString();
            }

        }
        catch (Exception e) {
            throw new RuntimeException("Error reading linestruct_description.txt: " + e.getMessage(), e);
        }
    }
}