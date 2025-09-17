package com.bentzn.util.linestruct;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

 class LineStructDateTimeSimpleTest {
    
    @Test
    void testDateValidation() {
        // Valid date
        String valid = """
            EntityName:Test
            schema:id:int¦date:date
            1¦2024-01-15
            """;
        
        System.out.println("Testing valid date format (2024-01-15): " + 
            LineStructParser.isValidLineStruct(valid));
        assertTrue(LineStructParser.isValidLineStruct(valid));
        
        // Invalid date format
        String invalid = """
            EntityName:Test
            schema:id:int¦date:date
            1¦01/15/2024
            """;
        
        System.out.println("Testing invalid date format (01/15/2024): " + 
            LineStructParser.isValidLineStruct(invalid));
        assertFalse(LineStructParser.isValidLineStruct(invalid));
    }
    
    @Test
    void testDateTimeValidation() {
        // Valid datetime
        String valid = """
            EntityName:Test
            schema:id:int¦time:datetime
            1¦2024-01-15T14:30:00Z
            """;
        
        System.out.println("Testing valid datetime format (2024-01-15T14:30:00Z): " + 
            LineStructParser.isValidLineStruct(valid));
        assertTrue(LineStructParser.isValidLineStruct(valid));
        
        // Invalid datetime (no timezone)
        String invalid = """
            EntityName:Test
            schema:id:int¦time:datetime
            1¦2024-01-15T14:30:00
            """;
        
        System.out.println("Testing invalid datetime format (no timezone): " + 
            LineStructParser.isValidLineStruct(invalid));
        assertFalse(LineStructParser.isValidLineStruct(invalid));
    }
}