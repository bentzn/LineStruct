package com.bentzn.util.linestruct;

import static com.bentzn.util.linestruct.CharacterUtils.ARRAY_END;
import static com.bentzn.util.linestruct.CharacterUtils.ARRAY_START;
import static com.bentzn.util.linestruct.CharacterUtils.NESTED_END;
import static com.bentzn.util.linestruct.CharacterUtils.NESTED_START;
import static com.bentzn.util.linestruct.CharacterUtils.replaceEscapedChars;
import static com.bentzn.util.linestruct.CharacterUtils.restoreEscapedChars;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bentzn.util.linestruct.LineStructParser.ASTNode;
import com.bentzn.util.linestruct.LineStructParser.NodeType;
import com.bentzn.util.linestruct.LineStructParser.Schema;

/**
 * Parser for LineStruct schema definitions.
 * 
 * This class handles parsing of schema lines into Abstract Syntax Trees (AST)
 * that represent the structure of the data.
 * 
 * @author bentzn/Grok/Claude
 */
class SchemaParser {

    /**
     * Parse schema line into AST
     * 
     * @param schemaLine The schema line to parse
     * @return Schema object containing the parsed AST
     * @throws IllegalArgumentException if schema line is invalid
     */
    static Schema parseSchema(String schemaLine) {
        if (!schemaLine.startsWith("schema:")) {
            throw new IllegalArgumentException("Schema line must start with 'schema:'");
        }

        String schemaContent = schemaLine.substring("schema:".length());

        // The root is always an object containing the fields
        ASTNode root = new ASTNode(NodeType.OBJECT);
        List<String> fieldDefs = FieldSplitter.splitFields(schemaContent);

        for (String fieldDef : fieldDefs) {
            // Parse each field definition (e.g., "id:int", "name:string", etc.)
            ASTNode fieldNode = parseFieldDefinition(fieldDef);
            root.children.add(fieldNode);
        }

        return new Schema("", root);
    }



    /**
     * Parse a field definition (e.g., "id:int",
     * "address:‹street:string¦city:string›")
     * 
     * @param fieldDef The field definition string
     * @return Parsed ASTNode representing the field
     * @throws IllegalArgumentException if field definition is invalid
     */
    private static ASTNode parseFieldDefinition(String fieldDef) {
        // Extract field name and type
        int colonIndex = fieldDef.indexOf(':');
        if (colonIndex == -1) {
            throw new IllegalArgumentException("Invalid field definition: " + fieldDef);
        }

        String fieldName = fieldDef.substring(0, colonIndex);
        String typeStr = fieldDef.substring(colonIndex + 1);

        // Check for description
        String description = null;
        Pattern descPattern = Pattern.compile("(.+?)(@desc=\"([^\"]+)\")$");
        Matcher matcher = descPattern.matcher(typeStr);
        if (matcher.matches()) {
            typeStr = matcher.group(1);
            description = matcher.group(3);
        }

        // Check if the whole field is optional (e.g., "description:<string>?")
        boolean isOptional = false;
        if (typeStr.startsWith("<") && typeStr.endsWith(">?")) {
            isOptional = true;
            typeStr = typeStr.substring(1, typeStr.length() - 2);
        }

        // Parse the type
        ASTNode node = parseSchemaType(typeStr);
        node.name = fieldName;
        node.isOptional = node.isOptional || isOptional;
        if (description != null) {
            node.description = description;
        }

        return node;
    }



    /**
     * Parse a schema type definition
     * 
     * @param typeStr The type string to parse
     * @return Parsed ASTNode representing the type
     */
    private static ASTNode parseSchemaType(String typeStr) {
        typeStr = typeStr.trim();

        // Check for optional (? at the end)
        boolean isOptional = false;
        if (typeStr.endsWith("?")) {
            isOptional = true;
            typeStr = typeStr.substring(0, typeStr.length() - 1).trim();
        }

        ASTNode node;

        // Check for array
        if (typeStr.startsWith(String.valueOf(ARRAY_START)) && typeStr.endsWith(String.valueOf(ARRAY_END))) {
            node = new ASTNode(NodeType.ARRAY);
            String innerType = typeStr.substring(1, typeStr.length() - 1);
            node.elementType = parseSchemaType(innerType);
        }
        // Check for object
        else if (typeStr.startsWith(String.valueOf(NESTED_START)) && typeStr.endsWith(String.valueOf(NESTED_END))) {
            node = parseObjectSchema(typeStr);
        }
        // Check for enum
        else if (typeStr.startsWith("{") && typeStr.endsWith("}")) {
            node = new ASTNode(NodeType.ENUM);
            String enumContent = typeStr.substring(1, typeStr.length() - 1);
            // Replace escaped characters with temporary markers
            enumContent = replaceEscapedChars(enumContent);
            // Now split by pipe character (unescaped ones)
            String[] values = enumContent.split("\\|");
            for (String value : values) {
                // Restore escaped characters in each enum value
                String restoredValue = restoreEscapedChars(value.trim());
                node.enumValues.add(restoredValue);
            }
        }
        // Primitive type
        else {
            node = new ASTNode(NodeType.PRIMITIVE);
            node.dataType = typeStr;
        }

        // Apply optional flag
        node.isOptional = isOptional;
        return node;
    }



    /**
     * Parse object schema
     * 
     * @param objectStr The object schema string
     * @return Parsed ASTNode representing the object
     */
    private static ASTNode parseObjectSchema(String objectStr) {
        ASTNode node = new ASTNode(NodeType.OBJECT);

        // Remove object delimiters
        String content = objectStr;
        if (content.startsWith(String.valueOf(NESTED_START))) {
            content = content.substring(1);
        }
        if (content.endsWith(String.valueOf(NESTED_END))) {
            content = content.substring(0, content.length() - 1);
        }

        // Split fields carefully, handling nested structures
        List<String> fields = FieldSplitter.splitFields(content);

        for (String field : fields) {
            ASTNode fieldNode = parseFieldDefinition(field);
            node.children.add(fieldNode);
        }

        return node;
    }
}