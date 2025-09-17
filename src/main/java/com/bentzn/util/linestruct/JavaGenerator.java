package com.bentzn.util.linestruct;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.bentzn.util.linestruct.LineStructParser.ASTNode;
import com.bentzn.util.linestruct.LineStructParser.NodeType;
import com.bentzn.util.linestruct.LineStructParser.Schema;

/**
 * Generator for Java POJO classes from LineStruct format.
 * 
 * This class analyzes LineStruct schemas and generates corresponding Java
 * classes with proper field types, getters, setters, constructors, and utility
 * methods.
 * 
 * @author bentzn/Grok/Claude
 */
class JavaGenerator {



    /**
     * Generate Java POJO classes from LineStruct format.
     * 
     * Analyzes the LineStruct schema and generates Java classes with: - Proper
     * field types (primitives, objects, collections) - Getters and setters for
     * all fields - Default constructor and all-args constructor - toString(),
     * equals(), and hashCode() methods - Proper imports for required Java
     * classes
     * 
     * @param lineStruct The LineStruct formatted string
     * @param packageName The Java package name for generated classes
     * @return Array of JavaClassDef objects containing filename and code for
     *         each generated class
     * @throws RuntimeException if the LineStruct format is invalid
     */
    static JavaClassDef[] generateJava(String lineStruct, String packageName) {
        try {
            if (lineStruct == null || lineStruct.trim().isEmpty()) {
                throw new IllegalArgumentException("LineStruct input is null or empty");
            }

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

            // Generate classes
            List<JavaClassDef> classes = new ArrayList<>();
            Set<String> generatedClasses = new HashSet<>();

            // Generate main class
            String mainClassName = capitalize(entityName);
            String mainClassCode = generateClassCode(mainClassName, schema.root, packageName, generatedClasses);
            classes.add(new JavaClassDef(mainClassName + ".java", mainClassCode));
            generatedClasses.add(mainClassName);

            // Generate nested classes (for object fields)
            generateNestedClasses(mainClassName, schema.root, packageName, classes, generatedClasses);

            return classes.toArray(new JavaClassDef[0]);

        }
        catch (Exception e) {
            throw new RuntimeException("Error generating Java from LineStruct: " + e.getMessage(), e);
        }
    }



    /**
     * Generate Java code for a class
     */
    private static String generateClassCode(String className,
            ASTNode schema,
            String packageName,
            Set<String> generatedClasses) {
        StringBuilder code = new StringBuilder();

        // Package declaration
        if (packageName != null && !packageName.isEmpty()) {
            code.append("package ").append(packageName).append(";\n\n");
        }

        // Determine required imports
        Set<String> imports = determineImports(schema);
        if (!imports.isEmpty()) {
            for (String imp : imports) {
                code.append("import ").append(imp).append(";\n");
            }
            code.append("\n");
        }

        // Class declaration
        code.append("/**\n");
        code.append(" * Generated POJO class for ").append(className).append("\n");
        code.append(" */\n");
        code.append("public class ").append(className).append(" {\n\n");

        // Generate fields
        for (ASTNode field : schema.children) {
            code.append("    private ").append(getJavaType(field, className)).append(" ").append(field.name)
                    .append(";\n");
        }
        code.append("\n");

        // Default constructor
        code.append("    /**\n");
        code.append("     * Default constructor\n");
        code.append("     */\n");
        code.append("    public ").append(className).append("() {\n");
        code.append("    }\n\n");

        // All-args constructor
        if (!schema.children.isEmpty()) {
            code.append("    /**\n");
            code.append("     * All-args constructor\n");
            code.append("     */\n");
            code.append("    public ").append(className).append("(");

            boolean first = true;
            for (ASTNode field : schema.children) {
                if (!first)
                    code.append(", ");
                first = false;
                code.append(getJavaType(field, className)).append(" ").append(field.name);
            }
            code.append(") {\n");

            for (ASTNode field : schema.children) {
                code.append("        this.").append(field.name).append(" = ").append(field.name).append(";\n");
            }
            code.append("    }\n\n");
        }

        // Generate getters and setters
        for (ASTNode field : schema.children) {
            generateGetterSetter(code, field, className);
        }

        // toString method
        generateToString(code, className, schema);

        // equals method
        generateEquals(code, className, schema);

        // hashCode method
        generateHashCode(code, className, schema);

        code.append("}\n");

        return code.toString();
    }



    /**
     * Generate getter and setter for a field
     */
    private static void generateGetterSetter(StringBuilder code, ASTNode field, String parentClassName) {
        String javaType = getJavaType(field, parentClassName);
        String capitalizedName = capitalize(field.name);

        // Getter
        code.append("    /**\n");
        code.append("     * Get ").append(field.name).append("\n");
        if (field.description != null && !field.description.isEmpty()) {
            code.append("     * ").append(field.description).append("\n");
        }
        code.append("     * @return ").append(field.name).append("\n");
        code.append("     */\n");
        code.append("    public ").append(javaType).append(" ");
        if (javaType.equals("boolean") || javaType.equals("Boolean")) {
            code.append("is");
        }
        else {
            code.append("get");
        }
        code.append(capitalizedName).append("() {\n");
        code.append("        return ").append(field.name).append(";\n");
        code.append("    }\n\n");

        // Setter
        code.append("    /**\n");
        code.append("     * Set ").append(field.name).append("\n");
        code.append("     * @param ").append(field.name).append(" the ").append(field.name).append(" to set\n");
        code.append("     */\n");
        code.append("    public void set").append(capitalizedName).append("(").append(javaType).append(" ")
                .append(field.name).append(") {\n");
        code.append("        this.").append(field.name).append(" = ").append(field.name).append(";\n");
        code.append("    }\n\n");
    }



    /**
     * Generate toString method
     */
    private static void generateToString(StringBuilder code, String className, ASTNode schema) {
        code.append("    @Override\n");
        code.append("    public String toString() {\n");

        if (schema.children.isEmpty()) {
            code.append("        return \"").append(className).append("{}\";\n");
        }
        else {
            code.append("        return \"").append(className).append("{\" +\n");

            boolean first = true;
            for (ASTNode field : schema.children) {
                if (!first) {
                    code.append(" +\n");
                }
                code.append("            \"");
                if (!first) {
                    code.append(", ");
                }
                code.append(field.name).append("=\" + ").append(field.name);
                first = false;
            }

            code.append(" +\n");
            code.append("            '}';\n");
        }
        code.append("    }\n\n");
    }



    /**
     * Generate equals method
     */
    private static void generateEquals(StringBuilder code, String className, ASTNode schema) {
        code.append("    @Override\n");
        code.append("    public boolean equals(Object o) {\n");
        code.append("        if (this == o) return true;\n");
        code.append("        if (o == null || getClass() != o.getClass()) return false;\n");

        if (!schema.children.isEmpty()) {
            code.append("        ").append(className).append(" that = (").append(className).append(") o;\n");
            code.append("        return ");
            boolean first = true;
            for (ASTNode field : schema.children) {
                if (!first) {
                    code.append(" &&\n            ");
                }
                first = false;

                String javaType = getJavaType(field, className);
                if (isPrimitive(javaType)) {
                    code.append(field.name).append(" == that.").append(field.name);
                }
                else {
                    code.append("Objects.equals(").append(field.name).append(", that.").append(field.name).append(")");
                }
            }
            code.append(";\n");
        }
        else {
            code.append("        return true;\n");
        }

        code.append("    }\n\n");
    }



    /**
     * Generate hashCode method
     */
    private static void generateHashCode(StringBuilder code, String className, ASTNode schema) {
        code.append("    @Override\n");
        code.append("    public int hashCode() {\n");

        if (!schema.children.isEmpty()) {
            code.append("        return Objects.hash(");
            boolean first = true;
            for (ASTNode field : schema.children) {
                if (!first)
                    code.append(", ");
                first = false;
                code.append(field.name);
            }
            code.append(");\n");
        }
        else {
            code.append("        return 0;\n");
        }

        code.append("    }\n");
    }



    /**
     * Generate nested classes for object fields
     */
    private static void generateNestedClasses(String parentClassName,
            ASTNode schema,
            String packageName,
            List<JavaClassDef> classes,
            Set<String> generatedClasses) {
        for (ASTNode field : schema.children) {
            if (field.type == NodeType.OBJECT) {
                String nestedClassName = parentClassName + capitalize(field.name);
                if (!generatedClasses.contains(nestedClassName)) {
                    String nestedClassCode = generateClassCode(nestedClassName, field, packageName, generatedClasses);
                    classes.add(new JavaClassDef(nestedClassName + ".java", nestedClassCode));
                    generatedClasses.add(nestedClassName);

                    // Recursively generate nested classes
                    generateNestedClasses(nestedClassName, field, packageName, classes, generatedClasses);
                }
            }
            else if (field.type == NodeType.ARRAY && field.elementType != null
                    && field.elementType.type == NodeType.OBJECT) {
                String nestedClassName = parentClassName + capitalize(singularize(field.name));
                if (!generatedClasses.contains(nestedClassName)) {
                    String nestedClassCode = generateClassCode(nestedClassName,
                            field.elementType,
                            packageName,
                            generatedClasses);
                    classes.add(new JavaClassDef(nestedClassName + ".java", nestedClassCode));
                    generatedClasses.add(nestedClassName);

                    // Recursively generate nested classes
                    generateNestedClasses(nestedClassName, field.elementType, packageName, classes, generatedClasses);
                }
            }
        }
    }



    /**
     * Determine required imports for a class
     */
    private static Set<String> determineImports(ASTNode schema) {
        Set<String> imports = new TreeSet<>(); // TreeSet for sorted imports
        boolean needsObjects = false;
        boolean needsList = false;
        boolean needsDate = false;

        // Check if we need Objects for equals/hashCode
        if (!schema.children.isEmpty()) {
            needsObjects = true;
        }

        for (ASTNode field : schema.children) {
            if (field.type == NodeType.ARRAY) {
                needsList = true;
            }

            // Check for non-primitive fields that need Objects.equals
            String javaType = getJavaType(field, "");
            if (!isPrimitive(javaType)) {
                needsObjects = true;
            }

            if (field.dataType != null && (field.dataType.equals("date") || field.dataType.equals("datetime"))) {
                needsDate = true;
            }
        }

        if (needsList) {
            imports.add("java.util.List");
            imports.add("java.util.ArrayList");
        }
        if (needsObjects) {
            imports.add("java.util.Objects");
        }
        if (needsDate) {
            imports.add("java.time.LocalDate");
            imports.add("java.time.LocalDateTime");
        }

        return imports;
    }



    /**
     * Get Java type for a field
     */
    private static String getJavaType(ASTNode field, String parentClassName) {
        switch (field.type) {
            case PRIMITIVE:
                return getPrimitiveJavaType(field.dataType, field.isOptional);

            case ARRAY:
                if (field.elementType != null) {
                    if (field.elementType.type == NodeType.OBJECT) {
                        String itemClassName = parentClassName + capitalize(singularize(field.name));
                        return "List<" + itemClassName + ">";
                    }
                    else {
                        String elementType = getJavaType(field.elementType, parentClassName);
                        return "List<" + boxedType(elementType) + ">";
                    }
                }
                else {
                    return "List<Object>";
                }

            case OBJECT:
                return parentClassName + capitalize(field.name);

            case ENUM:
                // For simplicity, using String for enums
                return "String";

            default:
                return "Object";
        }
    }



    /**
     * Get Java type for primitive types
     */
    private static String getPrimitiveJavaType(String dataType, boolean isOptional) {
        if (dataType == null) {
            return "String";
        }

        switch (dataType) {
            case "int":
                return isOptional ? "Integer" : "int";
            case "float":
                return isOptional ? "Float" : "float";
            case "bool":
                return isOptional ? "Boolean" : "boolean";
            case "date":
                return "LocalDate";
            case "datetime":
                return "LocalDateTime";
            case "string":
            default:
                return "String";
        }
    }



    /**
     * Get boxed type for primitives (for use in generics)
     */
    private static String boxedType(String type) {
        switch (type) {
            case "int":
                return "Integer";
            case "float":
                return "Float";
            case "boolean":
                return "Boolean";
            case "double":
                return "Double";
            case "long":
                return "Long";
            case "byte":
                return "Byte";
            case "short":
                return "Short";
            case "char":
                return "Character";
            default:
                return type;
        }
    }



    /**
     * Check if a type is primitive
     */
    private static boolean isPrimitive(String type) {
        return type.equals("int") || type.equals("float") || type.equals("boolean") || type.equals("double")
                || type.equals("long") || type.equals("byte") || type.equals("short") || type.equals("char");
    }



    /**
     * Capitalize first letter of a string
     */
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }



    /**
     * Convert plural to singular (simple heuristic)
     */
    private static String singularize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        // Special cases
        if (str.equalsIgnoreCase("employees")) {
            return "Employee";
        }
        if (str.equalsIgnoreCase("companies")) {
            return "Company";
        }
        if (str.equalsIgnoreCase("categories")) {
            return "Category";
        }
        if (str.equalsIgnoreCase("cities")) {
            return "City";
        }

        // Simple rules for common cases
        if (str.endsWith("ies") && str.length() > 3) {
            return str.substring(0, str.length() - 3) + "y";
        }
        if (str.endsWith("ees") && str.length() > 3) {
            return str.substring(0, str.length() - 1);
        }
        if (str.endsWith("es") && str.length() > 2) {
            if (str.length() > 3
                    && (str.endsWith("ses") || str.endsWith("xes") || str.endsWith("ches") || str.endsWith("shes"))) {
                return str.substring(0, str.length() - 2);
            }
            return str.substring(0, str.length() - 1);
        }
        if (str.endsWith("s") && !str.endsWith("ss") && str.length() > 1) {
            return str.substring(0, str.length() - 1);
        }

        return str;
    }
}