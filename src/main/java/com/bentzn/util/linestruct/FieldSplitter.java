package com.bentzn.util.linestruct;

import static com.bentzn.util.linestruct.CharacterUtils.ARRAY_END;
import static com.bentzn.util.linestruct.CharacterUtils.ARRAY_START;
import static com.bentzn.util.linestruct.CharacterUtils.FIELD_DELIMITER;
import static com.bentzn.util.linestruct.CharacterUtils.NESTED_END;
import static com.bentzn.util.linestruct.CharacterUtils.NESTED_START;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for splitting fields and values in LineStruct format.
 * 
 * This class handles the complex logic of splitting fields while respecting
 * nested structures like objects and arrays.
 * 
 * @author bentzn/Grok/Claude
 */
class FieldSplitter {

    /**
     * Split fields at delimiter, respecting nested structures.
     * 
     * @param content The content to split
     * @return List of field strings
     */
    static List<String> splitFields(String content) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;

        // Handle empty content
        if (content == null || content.isEmpty()) {
            return fields;
        }

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            if (c == NESTED_START || c == ARRAY_START || c == '{') {
                depth++;
            }
            else if (c == NESTED_END || c == ARRAY_END || c == '}') {
                depth--;
            }
            else if (c == FIELD_DELIMITER && depth == 0) {
                fields.add(current.toString());
                current = new StringBuilder();
                continue;
            }

            current.append(c);
        }

        // Always add the last field, even if empty
        fields.add(current.toString());

        return fields;
    }



    /**
     * Split data values, respecting nested structures.
     * 
     * @param dataLine The data line to split
     * @return List of value strings
     */
    static List<String> splitDataValues(String dataLine) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;

        if (dataLine == null || dataLine.isEmpty()) {
            return values;
        }

        for (int i = 0; i < dataLine.length(); i++) {
            char c = dataLine.charAt(i);

            if (c == NESTED_START || c == ARRAY_START) {
                depth++;
            }
            else if (c == NESTED_END || c == ARRAY_END) {
                depth--;
            }
            else if (c == FIELD_DELIMITER && depth == 0) {
                values.add(current.toString());
                current = new StringBuilder();
                continue;
            }

            current.append(c);
        }

        // Always add the last value, even if empty
        values.add(current.toString());

        return values;
    }



    /**
     * Split array of objects, respecting object boundaries.
     * 
     * @param content The array content
     * @return List of object strings
     */
    static List<String> splitArrayOfObjects(String content) {
        List<String> objects = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;
        boolean inObject = false;

        if (content == null || content.isEmpty()) {
            return objects;
        }

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            if (c == NESTED_START) {
                depth++;
                inObject = true;
            }
            else if (c == NESTED_END) {
                depth--;
                if (depth == 0 && inObject) {
                    current.append(c);
                    objects.add(current.toString());
                    current = new StringBuilder();
                    inObject = false;

                    // Skip delimiter after object if present
                    if (i + 1 < content.length() && content.charAt(i + 1) == FIELD_DELIMITER) {
                        i++;
                    }
                    continue;
                }
            }

            if (inObject || c == NESTED_START) {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            objects.add(current.toString());
        }

        return objects;
    }



    /**
     * Split array of arrays, respecting array boundaries.
     * 
     * @param content The array content
     * @return List of array strings
     */
    static List<String> splitArrayOfArrays(String content) {
        List<String> arrays = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int depth = 0;

        if (content == null || content.isEmpty()) {
            return arrays;
        }

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            if (c == ARRAY_START) {
                depth++;
            }
            else if (c == ARRAY_END) {
                depth--;
                if (depth == 0) {
                    current.append(c);
                    arrays.add(current.toString());
                    current = new StringBuilder();

                    // Skip delimiter after array if present
                    if (i + 1 < content.length() && content.charAt(i + 1) == FIELD_DELIMITER) {
                        i++;
                    }
                    continue;
                }
            }

            current.append(c);
        }

        if (current.length() > 0) {
            arrays.add(current.toString());
        }

        return arrays;
    }
}