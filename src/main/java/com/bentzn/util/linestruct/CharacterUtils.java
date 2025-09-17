package com.bentzn.util.linestruct;

/**
 * Utility class for handling special characters and escape sequences in LineStruct format.
 * 
 * This class manages all character constants and provides methods for escaping and
 * unescaping special characters used in the LineStruct format.
 * 
 * @author bentzn/Grok/Claude
 */
 class CharacterUtils {
    
    // Character constants
    public static final char FIELD_DELIMITER = '\u00A6';  // ¦
    public static final char NESTED_START = '\u2039';     // ‹
    public static final char NESTED_END = '\u203A';       // ›
    public static final char ARRAY_START = '\u00AB';      // «
    public static final char ARRAY_END = '\u00BB';        // »
    public static final char ENUM_DELIMITER = '\u007C';   // |
    public static final char ESCAPE_CHAR = '\u204A';      // ⊊
    
    // Temporary replacement markers for escaped characters
    private static final String ESCAPED_FIELD_DELIM = "<U+00A6>";
    private static final String ESCAPED_NESTED_START = "<U+2039>";
    private static final String ESCAPED_NESTED_END = "<U+203A>";
    private static final String ESCAPED_ARRAY_START = "<U+00AB>";
    private static final String ESCAPED_ARRAY_END = "<U+00BB>";
    private static final String ESCAPED_ENUM_DELIM = "<U+007C>";
    
    /**
     * Replace escaped characters with temporary markers.
     * This is used during parsing to handle escaped special characters.
     * 
     * @param input The input string with escape sequences
     * @return String with escape sequences replaced by temporary markers
     */
     static String replaceEscapedChars(String input) {
        if (input == null) return null;
        
        return input.replace(ESCAPE_CHAR + String.valueOf(FIELD_DELIMITER), ESCAPED_FIELD_DELIM)
                .replace(ESCAPE_CHAR + String.valueOf(NESTED_START), ESCAPED_NESTED_START)
                .replace(ESCAPE_CHAR + String.valueOf(NESTED_END), ESCAPED_NESTED_END)
                .replace(ESCAPE_CHAR + String.valueOf(ARRAY_START), ESCAPED_ARRAY_START)
                .replace(ESCAPE_CHAR + String.valueOf(ARRAY_END), ESCAPED_ARRAY_END)
                .replace(ESCAPE_CHAR + String.valueOf(ENUM_DELIMITER), ESCAPED_ENUM_DELIM);
    }
    
    /**
     * Restore escaped characters from temporary markers.
     * This reverses the operation performed by replaceEscapedChars.
     * 
     * @param input The input string with temporary markers
     * @return String with temporary markers replaced by actual characters
     */
     static String restoreEscapedChars(String input) {
        if (input == null) return null;
        
        return input.replace(ESCAPED_FIELD_DELIM, String.valueOf(FIELD_DELIMITER))
                .replace(ESCAPED_NESTED_START, String.valueOf(NESTED_START))
                .replace(ESCAPED_NESTED_END, String.valueOf(NESTED_END))
                .replace(ESCAPED_ARRAY_START, String.valueOf(ARRAY_START))
                .replace(ESCAPED_ARRAY_END, String.valueOf(ARRAY_END))
                .replace(ESCAPED_ENUM_DELIM, String.valueOf(ENUM_DELIMITER));
    }
    
    /**
     * Check if a value needs escaping.
     * 
     * @param value The value to check
     * @return true if the value contains special characters that need escaping
     */
     static boolean needsEscaping(String value) {
        if (value == null) return false;
        
        return value.contains(String.valueOf(FIELD_DELIMITER)) || 
               value.contains(String.valueOf(NESTED_START)) ||
               value.contains(String.valueOf(NESTED_END)) || 
               value.contains(String.valueOf(ARRAY_START)) ||
               value.contains(String.valueOf(ARRAY_END)) || 
               value.contains(String.valueOf(ENUM_DELIMITER));
    }
    
    /**
     * Escape special characters in a value.
     * 
     * @param value The value to escape
     * @return The escaped value
     */
     static String escapeValue(String value) {
        if (value == null) return null;
        
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == FIELD_DELIMITER || c == NESTED_START || c == NESTED_END || 
                c == ARRAY_START || c == ARRAY_END || c == ENUM_DELIMITER) {
                escaped.append(ESCAPE_CHAR);
            }
            escaped.append(c);
        }
        return escaped.toString();
    }
    
    /**
     * Unescape special characters in a value.
     * This method removes escape characters and returns the original value.
     * 
     * @param value The escaped value
     * @return The unescaped value
     */
     static String unescapeValue(String value) {
        if (value == null) return null;
        
        StringBuilder unescaped = new StringBuilder();
        boolean nextIsEscaped = false;
        
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            
            if (nextIsEscaped) {
                // This character was escaped, add it as-is
                unescaped.append(c);
                nextIsEscaped = false;
            } else if (c == ESCAPE_CHAR) {
                // This is an escape character, skip it and mark next as escaped
                nextIsEscaped = true;
            } else {
                // Regular character
                unescaped.append(c);
            }
        }
        
        return unescaped.toString();
    }
}