package com.bentzn.util.linestruct;

/**
 * Utility class for formatting LineStruct schema strings between compact single-line
 * and pretty-printed multi-line representations.
 * 
 * <p>LineStruct uses special Unicode characters as delimiters:
 * <ul>
 * <li>{@code ¦} (U+00A6) - Field delimiter</li>
 * <li>{@code ‹} (U+2039) - Object start marker</li>
 * <li>{@code ›} (U+203A) - Object end marker</li>
 * <li>{@code «} (U+00AB) - Array start marker</li>
 * <li>{@code »} (U+00BB) - Array end marker</li>
 * <li>{@code ⁊} (U+204A) - Escape character</li>
 * <li>{@code {}} - Enum delimiters</li>
 * </ul>
 * 
 * <p>This class provides bidirectional conversion between compact schema strings
 * (suitable for storage or transmission) and human-readable indented formats
 * (suitable for display or editing).
 * 
 * @author bentzn/Grok/Claude
 * @generated 2025-09-17T14:24:00Z
 */
class LineStructFormatter {

    /**
     * Pretty prints a LineStruct schema string
     * 
     * @param schemaString The single-line schema string
     * @return The pretty-printed schema
     */
    static String prettyPrintSchema(String schema) {
        return prettyPrintSchema(schema, 2);
    }



    /**
     * Pretty prints a LineStruct schema string
     * 
     * @param schemaString The single-line schema string
     * @param indentSize Number of spaces per indent level
     * @return The pretty-printed schema
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
            if (ch == '⁊' && !escaped) {
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
     * Compacts a pretty-printed schema back to single line
     * 
     * @param prettySchema The pretty-printed schema
     * @return The single-line schema
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
     * Helper method to create indentation
     */
    private static String indent(int level, int size) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level * size; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

}