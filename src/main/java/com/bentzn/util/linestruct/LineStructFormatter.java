package com.bentzn.util.linestruct;

/**
 * Utility class for formatting LineStruct schema strings between compact single-line
 * and pretty-printed multi-line representations.
 * 
 * <p>LineStruct use special Unicode characters as delimiters:
 * <ul>
 * <li>{@code ¦} (U+00A6) - Field delimiter</li>
 * <li>{@code ‹} (U+2039) - Object start marker</li>
 * <li>{@code ›} (U+203A) - Object end marker</li>
 * <li>{@code «} (U+00AB) - Array start marker</li>
 * <li>{@code »} (U+00BB) - Array end marker</li>
 * <li>{@code Š} (U+0160) - Escape character</li>
 * <li>{@code {}} - Enum delimiters</li>
 * </ul>
 * 
 * <p>This class provides bidirectional conversion between compact schema strings
 * (suitable for storage or transmission) and human-readable indented formats
 * (suitable for display or editing).
 * 
 * @author bentzn/Grok/Claude
 */
class LineStructFormatter {

    /**
     * Pretty prints a LineStruct schema string using default indentation.
     * 
     * <p>This is a convenience method that calls {@link #prettyPrintSchema(String, int)}
     * with a default indent size of 2 spaces per level.
     * 
     * <p>Example:
     * <pre>
     * Input:  "schema:name:string¦age:int‹address:string¦city:string›"
     * Output: "schema:
     *           name:string
     *           age:int
     *           ‹
     *             address:string
     *             city:string
     *           ›"
     * </pre>
     * 
     * @param schema The single-line schema string to format. May optionally start with "schema:"
     * @return The pretty-printed schema with proper indentation and line breaks
     * @throws NullPointerException if schema is null
     * @see #prettyPrintSchema(String, int)
     */
    static String prettyPrintSchema(String schema) {
        return prettyPrintSchema(schema, 2);
    }

    /**
     * Pretty prints a LineStruct schema string with custom indentation.
     * 
     * <p>Converts a compact single-line schema into a multi-line format with proper
     * indentation to show the hierarchical structure of objects and arrays. The method
     * handles:
     * <ul>
     * <li>Field separation and proper line breaks</li>
     * <li>Nested object and array indentation</li>
     * <li>Enum preservation (content within {} is kept intact)</li>
     * <li>Escape sequence handling</li>
     * </ul>
     * 
     * <p>The "schema:" prefix is automatically added to the output if not present in input.
     * 
     * @param schemaString The single-line schema string to format. May optionally start with "schema:"
     * @param indentSize Number of spaces to use for each level of indentation. Must be >= 0
     * @return The pretty-printed schema with the specified indentation
     * @throws NullPointerException if schemaString is null
     * @throws IllegalArgumentException if indentSize is negative
     * @see #compactSchema(String)
     */
    static String prettyPrintSchema(String schemaString, int indentSize) {
        // Remove "schema:" prefix if present
        String content = schemaString.startsWith("schema:") ? schemaString.substring(7) : schemaString;

        StringBuilder result = new StringBuilder("schema:\n");
        int indentLevel = 1;
        int i = 0;
        StringBuilder currentField = new StringBuilder();
        boolean inEnum = false;
        boolean escaped = false;

        while (i < content.length()) {
            char ch = content.charAt(i);

            // Handle escape sequences
            if (ch == 'Š' && !escaped) {
                escaped = true;
                currentField.append(ch);
                i++;
                continue;
            }

            if (escaped) {
                currentField.append(ch);
                escaped = false;
                i++;
                continue;
            }

            // Handle enum start/end
            if (ch == '{') {
                inEnum = true;
                currentField.append(ch);
                i++;
                continue;
            }

            if (ch == '}') {
                inEnum = false;
                currentField.append(ch);
                i++;
                continue;
            }

            // Don't process special chars inside enums
            if (inEnum) {
                currentField.append(ch);
                i++;
                continue;
            }

            // Handle delimiters and nesting
            switch (ch) {
                case '¦':
                    // Field delimiter - output current field and move to next
                    if (currentField.toString().trim().length() > 0) {
                        result.append(indent(indentLevel, indentSize)).append(currentField.toString().trim())
                                .append('\n');
                    }
                    currentField = new StringBuilder();
                    break;

                case '‹':
                    // Object start
                    if (currentField.toString().trim().length() > 0) {
                        // Field name before object
                        result.append(indent(indentLevel, indentSize)).append(currentField.toString().trim())
                                .append('\n');
                        currentField = new StringBuilder();
                    }
                    result.append(indent(indentLevel, indentSize)).append("‹\n");
                    indentLevel++;
                    break;

                case '›':
                    // Object end
                    if (currentField.toString().trim().length() > 0) {
                        result.append(indent(indentLevel, indentSize)).append(currentField.toString().trim())
                                .append('\n');
                        currentField = new StringBuilder();
                    }
                    indentLevel--;
                    result.append(indent(indentLevel, indentSize)).append("›\n");
                    break;

                case '«':
                    // Array start
                    if (currentField.toString().trim().length() > 0) {
                        // Field name before array
                        result.append(indent(indentLevel, indentSize)).append(currentField.toString().trim())
                                .append('\n');
                        currentField = new StringBuilder();
                    }
                    result.append(indent(indentLevel, indentSize)).append("«\n");
                    indentLevel++;
                    break;

                case '»':
                    // Array end
                    if (currentField.toString().trim().length() > 0) {
                        result.append(indent(indentLevel, indentSize)).append(currentField.toString().trim())
                                .append('\n');
                        currentField = new StringBuilder();
                    }
                    indentLevel--;
                    result.append(indent(indentLevel, indentSize)).append("»\n");
                    break;

                default:
                    currentField.append(ch);
            }

            i++;
        }

        // Output any remaining field
        if (currentField.toString().trim().length() > 0) {
            result.append(indent(indentLevel, indentSize)).append(currentField.toString().trim()).append('\n');
        }

        // Remove trailing newline
        if (result.length() > 0 && result.charAt(result.length() - 1) == '\n') {
            result.setLength(result.length() - 1);
        }

        return result.toString();
    }

    /**
     * Compacts a pretty-printed schema back to a single line format.
     * 
     * <p>This method performs the inverse operation of {@link #prettyPrintSchema(String, int)},
     * converting a multi-line indented schema back to a compact single-line representation.
     * The process involves:
     * <ul>
     * <li>Removing all whitespace and line breaks</li>
     * <li>Inserting appropriate field delimiters (¦) between schema elements</li>
     * <li>Preserving structural markers for objects and arrays</li>
     * <li>Maintaining proper delimiter placement around opening/closing markers</li>
     * </ul>
     * 
     * <p>Empty lines and pure whitespace lines are ignored during processing.
     * The output always includes the "schema:" prefix.
     * 
     * <p>Example:
     * <pre>
     * Input:  "schema:
     *           name:string
     *           age:int
     *           ‹
     *             address:string
     *             city:string
     *           ›"
     * Output: "schema:name:string¦age:int¦‹address:string¦city:string›"
     * </pre>
     * 
     * @param prettySchema The pretty-printed (multi-line) schema to compact
     * @return The single-line compact schema representation
     * @throws NullPointerException if prettySchema is null
     * @see #prettyPrintSchema(String)
     * @see #prettyPrintSchema(String, int)
     */
    static String compactSchema(String prettySchema) {
        String[] lines = prettySchema.split("\n");
        int startIndex = 0;

        // Skip "schema:" line if present
        if (lines.length > 0 && lines[0].trim().equals("schema:")) {
            startIndex = 1;
        }

        StringBuilder result = new StringBuilder("schema:");
        boolean needsDelimiter = false;

        for (int i = startIndex; i < lines.length; i++) {
            String trimmed = lines[i].trim();
            if (trimmed.isEmpty())
                continue;

            // Check if this is a structural marker
            boolean isOpeningMarker = trimmed.equals("‹") || trimmed.equals("«");
            boolean isClosingMarker = trimmed.equals("›") || trimmed.equals("»");

            if (isOpeningMarker) {
                // Opening markers - might need delimiter before
                if (needsDelimiter && result.charAt(result.length() - 1) != ':') {
                    result.append('¦');
                }
                result.append(trimmed);
                needsDelimiter = false;
            }
            else if (isClosingMarker) {
                // Closing markers
                result.append(trimmed);
                needsDelimiter = true;
            }
            else {
                // Regular field
                if (needsDelimiter) {
                    result.append('¦');
                }
                result.append(trimmed);
                needsDelimiter = true;
            }
        }

        return result.toString();
    }

    /**
     * Creates a string of spaces for indentation purposes.
     * 
     * <p>This is a utility method used internally by the formatting methods
     * to generate consistent indentation strings.
     * 
     * @param level The indentation level (0 = no indentation, 1 = one level, etc.)
     * @param size The number of spaces per indentation level
     * @return A string containing {@code level * size} space characters
     * @throws IllegalArgumentException if level or size is negative
     */
    private static String indent(int level, int size) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level * size; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }
}