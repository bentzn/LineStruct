package com.bentzn.util.linestruct;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for JavaGenerator.
 * Tests that generated Java code is syntactically correct and compiles.
 */
 class JavaGeneratorTest {

    @TempDir
    Path tempDir;

    /**
     * Helper method to compile Java source code and verify it compiles successfully
     */
    private boolean compileJavaCode(JavaClassDef[] classes, String packageName) throws IOException {
        // First, print all generated code
        for (JavaClassDef classDef : classes) {
            System.out.println("Generated file: " + classDef.getFilename());
            System.out.println("----------------------------------------");
            System.out.println(classDef.getCode());
            System.out.println("----------------------------------------\n");
        }
        
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            System.out.println("Warning: Java compiler not available, skipping compilation test");
            return true; // Skip compilation test if compiler not available
        }

        // Create a diagnostic collector to capture compilation errors
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null);
        List<File> sourceFiles = new ArrayList<>();

        try {
            // Create package directory structure
            Path packageDir = tempDir;
            if (packageName != null && !packageName.isEmpty()) {
                String[] packageParts = packageName.split("\\.");
                for (String part : packageParts) {
                    packageDir = packageDir.resolve(part);
                }
                Files.createDirectories(packageDir);
            }

            // Write source files to temp directory
            for (JavaClassDef classDef : classes) {
                Path sourceFile = packageDir.resolve(classDef.getFilename());
                Files.writeString(sourceFile, classDef.getCode());
                sourceFiles.add(sourceFile.toFile());
            }

            // Compile the source files
            JavaCompiler.CompilationTask task = compiler.getTask(
                null,
                fileManager,
                diagnostics,
                Arrays.asList("-d", tempDir.toString()),
                null,
                fileManager.getJavaFileObjectsFromFiles(sourceFiles)
            );

            boolean success = task.call();
            
            // Print compilation errors if any
            if (!success) {
                System.err.println("Compilation failed with the following errors:");
                for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                    System.err.format("Error on line %d in %s: %s%n",
                        diagnostic.getLineNumber(),
                        diagnostic.getSource() != null ? diagnostic.getSource().toUri() : "unknown",
                        diagnostic.getMessage(null));
                }
            }
            
            return success;
        } finally {
            fileManager.close();
        }
    }

    @Test
    @DisplayName("Test simple POJO generation and compilation")
    void testSimplePojoGeneration() throws Exception {
        String lineStruct = """
            EntityName:Person
            schema:id:int¦name:string¦age:int¦active:bool
            1¦John Doe¦30¦true
            2¦Jane Smith¦25¦false
            """;

        JavaClassDef[] classes = LineStructParser.generateJava(lineStruct, "com.example.model");
        
        assertNotNull(classes);
        assertEquals(1, classes.length, "Should generate exactly one class");
        
        JavaClassDef personClass = classes[0];
        assertEquals("Person.java", personClass.getFilename());
        
        // Verify code contains expected elements
        String code = personClass.getCode();
        assertTrue(code.contains("package com.example.model;"));
        assertTrue(code.contains("public class Person"));
        assertTrue(code.contains("private int id;"));
        assertTrue(code.contains("private String name;"));
        assertTrue(code.contains("private int age;"));
        assertTrue(code.contains("private boolean active;"));
        assertTrue(code.contains("public int getId()"));
        assertTrue(code.contains("public void setId(int id)"));
        assertTrue(code.contains("public String toString()"));
        assertTrue(code.contains("public boolean equals(Object o)"));
        assertTrue(code.contains("public int hashCode()"));
        
        // Test compilation
        assertTrue(compileJavaCode(classes, "com.example.model"), "Generated code should compile");
    }

    @Test
    @DisplayName("Test optional fields generation")
    void testOptionalFieldsGeneration() throws Exception {
        String lineStruct = """
            EntityName:Employee
            schema:id:int¦name:string¦age:<int>?¦email:<string>?¦salary:<float>?
            1¦John¦30¦john@example.com¦50000.0
            2¦Jane¦¦¦
            """;

        JavaClassDef[] classes = LineStructParser.generateJava(lineStruct, "com.example");
        
        assertNotNull(classes);
        assertEquals(1, classes.length);
        
        String code = classes[0].getCode();
        
        // Verify optional fields use wrapper classes
        assertTrue(code.contains("private Integer age;"), "Optional int should be Integer");
        assertTrue(code.contains("private String email;"), "Optional string should be String");
        assertTrue(code.contains("private Float salary;"), "Optional float should be Float");
        
        // Verify required field uses primitive
        assertTrue(code.contains("private int id;"), "Required int should be primitive");
        
        // Test compilation
        assertTrue(compileJavaCode(classes, "com.example"), "Generated code should compile");
    }

    @Test
    @DisplayName("Test nested object generation")
    void testNestedObjectGeneration() throws Exception {
        String lineStruct = """
            EntityName:Employee
            schema:id:int¦name:string¦address:‹street:string¦city:string¦zip:int›
            1¦John Doe¦‹123 Main St¦New York¦10001›
            """;

        JavaClassDef[] classes = LineStructParser.generateJava(lineStruct, "com.example");
        
        assertNotNull(classes);
        assertEquals(2, classes.length, "Should generate main class and nested class");
        
        // Find the classes
        JavaClassDef employeeClass = null;
        JavaClassDef addressClass = null;
        
        for (JavaClassDef classDef : classes) {
            if (classDef.getFilename().equals("Employee.java")) {
                employeeClass = classDef;
            } else if (classDef.getFilename().equals("EmployeeAddress.java")) {
                addressClass = classDef;
            }
        }
        
        assertNotNull(employeeClass, "Should generate Employee class");
        assertNotNull(addressClass, "Should generate EmployeeAddress class");
        
        // Verify Employee class references EmployeeAddress
        assertTrue(employeeClass.getCode().contains("private EmployeeAddress address;"));
        
        // Verify EmployeeAddress class structure
        String addressCode = addressClass.getCode();
        assertTrue(addressCode.contains("public class EmployeeAddress"));
        assertTrue(addressCode.contains("private String street;"));
        assertTrue(addressCode.contains("private String city;"));
        assertTrue(addressCode.contains("private int zip;"));
        
        // Test compilation
        assertTrue(compileJavaCode(classes, "com.example"), "Generated code should compile");
    }

    @Test
    @DisplayName("Test array field generation")
    void testArrayFieldGeneration() throws Exception {
        String lineStruct = """
            EntityName:Student
            schema:id:int¦name:string¦subjects:«string»¦grades:«float»
            1¦Alice¦«Math¦Science¦English»¦«95.5¦88.0¦92.5»
            """;

        JavaClassDef[] classes = LineStructParser.generateJava(lineStruct, "edu.example");
        
        assertNotNull(classes);
        assertEquals(1, classes.length);
        
        String code = classes[0].getCode();
        
        // Verify imports for List
        assertTrue(code.contains("import java.util.List;"));
        assertTrue(code.contains("import java.util.ArrayList;"));
        
        // Verify array fields use List
        assertTrue(code.contains("private List<String> subjects;"));
        assertTrue(code.contains("private List<Float> grades;"));
        assertTrue(code.contains("public List<String> getSubjects()"));
        assertTrue(code.contains("public void setSubjects(List<String> subjects)"));
        
        // Test compilation
        assertTrue(compileJavaCode(classes, "edu.example"), "Generated code should compile");
    }

    @Test
    @DisplayName("Test array of objects generation")
    void testArrayOfObjectsGeneration() throws Exception {
        String lineStruct = """
            EntityName:Order
            schema:id:int¦customer:string¦items:«‹product:string¦quantity:int¦price:float›»
            1¦John¦«‹Widget¦2¦25.50›¦‹Gadget¦1¦45.00›»
            """;

        System.out.println("\n=== TEST: Array of Objects Generation ===");
        System.out.println("Testing array of objects with LineStruct:");
        System.out.println(lineStruct);
        System.out.println();
        
        JavaClassDef[] classes = LineStructParser.generateJava(lineStruct, "com.shop");
        
        assertNotNull(classes);
        System.out.println("Number of classes generated: " + classes.length);
        for (JavaClassDef classDef : classes) {
            System.out.println("  - " + classDef.getFilename());
        }
        
        assertEquals(2, classes.length, "Should generate Order and OrderItem classes");
        
        // Find the classes
        JavaClassDef orderClass = null;
        JavaClassDef itemClass = null;
        
        for (JavaClassDef classDef : classes) {
            System.out.println("Checking class: " + classDef.getFilename());
            if (classDef.getFilename().equals("Order.java")) {
                orderClass = classDef;
                System.out.println("  Found Order class");
            } else if (classDef.getFilename().equals("OrderItem.java")) {
                itemClass = classDef;
                System.out.println("  Found OrderItem class");
            }
        }
        
        assertNotNull(orderClass, "Should generate Order class");
        assertNotNull(itemClass, "Should generate OrderItem class");
        
        System.out.println("\nChecking Order class content...");
        // Verify Order class has List of OrderItem
        String orderCode = orderClass.getCode();
        System.out.println("Order class contains 'private List<OrderItem> items;': " + 
                         orderCode.contains("private List<OrderItem> items;"));
        assertTrue(orderCode.contains("private List<OrderItem> items;"), 
                  "Order class should have List<OrderItem> items field");
        
        System.out.println("\nChecking OrderItem class content...");
        // Verify OrderItem structure
        String itemCode = itemClass.getCode();
        System.out.println("OrderItem contains 'public class OrderItem': " + 
                         itemCode.contains("public class OrderItem"));
        System.out.println("OrderItem contains 'private String product;': " + 
                         itemCode.contains("private String product;"));
        System.out.println("OrderItem contains 'private int quantity;': " + 
                         itemCode.contains("private int quantity;"));
        System.out.println("OrderItem contains 'private float price;': " + 
                         itemCode.contains("private float price;"));
        
        assertTrue(itemCode.contains("public class OrderItem"));
        assertTrue(itemCode.contains("private String product;"));
        assertTrue(itemCode.contains("private int quantity;"));
        assertTrue(itemCode.contains("private float price;"));
        
        System.out.println("\nAttempting compilation...");
        // Test compilation
        boolean compileSuccess = compileJavaCode(classes, "com.shop");
        assertTrue(compileSuccess, "Generated code should compile");
        System.out.println("=== END TEST: Array of Objects Generation ===\n");
    }

    @Test
    @DisplayName("Test enum field generation")
    void testEnumFieldGeneration() throws Exception {
        String lineStruct = """
            EntityName:Task
            schema:id:int¦title:string¦status:{TODO|IN_PROGRESS|DONE}¦priority:<{LOW|MEDIUM|HIGH}>?
            1¦Fix bug¦IN_PROGRESS¦HIGH
            2¦Write tests¦TODO¦
            """;

        JavaClassDef[] classes = LineStructParser.generateJava(lineStruct, "com.tasks");
        
        assertNotNull(classes);
        assertEquals(1, classes.length);
        
        String code = classes[0].getCode();
        
        // Currently enums are generated as String
        assertTrue(code.contains("private String status;"));
        assertTrue(code.contains("private String priority;"));
        
        // Test compilation
        assertTrue(compileJavaCode(classes, "com.tasks"), "Generated code should compile");
    }

    @Test
    @DisplayName("Test date and datetime field generation")
    void testDateTimeFieldGeneration() throws Exception {
        String lineStruct = """
            EntityName:Event
            schema:id:int¦name:string¦eventDate:date¦createdAt:datetime
            1¦Conference¦2024-03-15¦2024-01-10T10:30:00Z
            """;

        JavaClassDef[] classes = LineStructParser.generateJava(lineStruct, "com.events");
        
        assertNotNull(classes);
        assertEquals(1, classes.length);
        
        String code = classes[0].getCode();
        
        // Verify imports for date/time
        assertTrue(code.contains("import java.time.LocalDate;"));
        assertTrue(code.contains("import java.time.LocalDateTime;"));
        
        // Verify date fields
        assertTrue(code.contains("private LocalDate eventDate;"));
        assertTrue(code.contains("private LocalDateTime createdAt;"));
        
        // Test compilation
        assertTrue(compileJavaCode(classes, "com.events"), "Generated code should compile");
    }

    @Test
    @DisplayName("Test complex nested structure generation")
    void testComplexNestedStructure() throws Exception {
        String lineStruct = """
            EntityName:Company
            schema:id:int¦name:string¦headquarters:‹city:string¦country:string›¦departments:«‹name:string¦manager:‹name:string¦email:string›¦employees:«‹id:int¦name:string›»›»
            1¦TechCorp¦‹San Francisco¦USA›¦«‹Engineering¦‹Alice¦alice@tech.com›¦«‹101¦Bob›¦‹102¦Charlie›»›»
            """;

        System.out.println("\n=== TEST: Complex Nested Structure ===");
        JavaClassDef[] classes = LineStructParser.generateJava(lineStruct, "com.company");
        
        assertNotNull(classes);
        System.out.println("Number of classes generated: " + classes.length);
        
        // Print all generated class names
        Set<String> generatedFiles = new HashSet<>();
        for (JavaClassDef classDef : classes) {
            generatedFiles.add(classDef.getFilename());
            System.out.println("  - " + classDef.getFilename());
        }
        
        assertTrue(classes.length >= 4, "Should generate at least 4 nested classes, but got " + classes.length);
        
        // Verify all expected classes are generated
        System.out.println("\nChecking for expected classes:");
        System.out.println("Company.java: " + generatedFiles.contains("Company.java"));
        System.out.println("CompanyHeadquarters.java: " + generatedFiles.contains("CompanyHeadquarters.java"));
        System.out.println("CompanyDepartment.java: " + generatedFiles.contains("CompanyDepartment.java"));
        System.out.println("CompanyDepartmentManager.java: " + generatedFiles.contains("CompanyDepartmentManager.java"));
        System.out.println("CompanyDepartmentEmployee.java: " + generatedFiles.contains("CompanyDepartmentEmployee.java"));
        
        assertTrue(generatedFiles.contains("Company.java"), "Should generate Company.java");
        assertTrue(generatedFiles.contains("CompanyHeadquarters.java"), "Should generate CompanyHeadquarters.java");
        assertTrue(generatedFiles.contains("CompanyDepartment.java"), "Should generate CompanyDepartment.java");
        assertTrue(generatedFiles.contains("CompanyDepartmentManager.java"), "Should generate CompanyDepartmentManager.java");
        assertTrue(generatedFiles.contains("CompanyDepartmentEmployee.java"), "Should generate CompanyDepartmentEmployee.java");
        
        // Test compilation
        System.out.println("\nAttempting compilation...");
        boolean success = compileJavaCode(classes, "com.company");
        assertTrue(success, "Generated code should compile");
        System.out.println("=== END TEST: Complex Nested Structure ===\n");
    }

    @Test
    @DisplayName("Test generation without package name")
    void testGenerationWithoutPackage() throws Exception {
        String lineStruct = """
            EntityName:Simple
            schema:id:int¦name:string
            1¦Test
            """;

        JavaClassDef[] classes = LineStructParser.generateJava(lineStruct, null);
        
        assertNotNull(classes);
        assertEquals(1, classes.length);
        
        String code = classes[0].getCode();
        
        // Should not have package declaration
        assertFalse(code.startsWith("package "));
        assertTrue(code.contains("public class Simple"));
        
        // Test compilation
        assertTrue(compileJavaCode(classes, null), "Generated code should compile");
    }

    @Test
    @DisplayName("Test all-args constructor generation")
    void testConstructorGeneration() throws Exception {
        String lineStruct = """
            EntityName:Product
            schema:id:int¦name:string¦price:float¦inStock:bool
            1¦Widget¦19.99¦true
            """;

        JavaClassDef[] classes = LineStructParser.generateJava(lineStruct, "com.store");
        
        assertNotNull(classes);
        assertEquals(1, classes.length);
        
        String code = classes[0].getCode();
        
        // Verify default constructor
        assertTrue(code.contains("public Product()"));
        
        // Verify all-args constructor
        assertTrue(code.contains("public Product(int id, String name, float price, boolean inStock)"));
        assertTrue(code.contains("this.id = id;"));
        assertTrue(code.contains("this.name = name;"));
        assertTrue(code.contains("this.price = price;"));
        assertTrue(code.contains("this.inStock = inStock;"));
        
        // Test compilation
        assertTrue(compileJavaCode(classes, "com.store"), "Generated code should compile");
    }

    @Test
    @DisplayName("Test empty schema handling")
    void testEmptySchema() throws Exception {
        String lineStruct = """
            EntityName:Empty
            schema:
            """;

        JavaClassDef[] classes = LineStructParser.generateJava(lineStruct, "com.test");
        
        assertNotNull(classes);
        assertEquals(1, classes.length);
        
        String code = classes[0].getCode();
        
        // Should generate a class with no fields
        assertTrue(code.contains("public class Empty"));
        assertTrue(code.contains("public Empty()")); // Default constructor
        assertFalse(code.contains("private ")); // No fields
        
        // Test compilation
        assertTrue(compileJavaCode(classes, "com.test"), "Generated code should compile");
    }

    @Test
    @DisplayName("Test boolean getter naming convention")
    void testBooleanGetterNaming() throws Exception {
        String lineStruct = """
            EntityName:Settings
            schema:id:int¦enabled:bool¦active:bool¦deleted:<bool>?
            1¦true¦false¦
            """;

        JavaClassDef[] classes = LineStructParser.generateJava(lineStruct, "com.config");
        
        assertNotNull(classes);
        assertEquals(1, classes.length);
        
        String code = classes[0].getCode();
        
        // Boolean getters should use 'is' prefix
        assertTrue(code.contains("public boolean isEnabled()"));
        assertTrue(code.contains("public boolean isActive()"));
        assertTrue(code.contains("public Boolean isDeleted()")); // Optional Boolean
        
        // Test compilation
        assertTrue(compileJavaCode(classes, "com.config"), "Generated code should compile");
    }
    
    
    @Test
    @DisplayName("Test triple-nested arrays with Organization structure")
    void testTripleNestedArraysOrganization() throws Exception {
        String lineStruct = """
                EntityName:Organization
                schema:id:int¦name:string¦departments:«‹name:string¦employees:«‹id:int¦name:string¦skills:«string»›»›»
                1¦TechCo¦«‹Engineering¦«‹101¦Alice¦«Java¦Python»›¦‹102¦Bob¦«JavaScript¦TypeScript¦React»›»›¦‹Marketing¦«‹201¦Charlie¦«SEO¦Content»›»›»
                """;

        System.out.println("\n=== TEST: Triple-Nested Arrays Organization ===");
        JavaClassDef[] classes = LineStructParser.generateJava(lineStruct, "com.org");
        
        assertNotNull(classes);
        System.out.println("Number of classes generated: " + classes.length);
        
        Set<String> generatedFiles = new HashSet<>();
        for (JavaClassDef classDef : classes) {
            generatedFiles.add(classDef.getFilename());
            System.out.println("  - " + classDef.getFilename());
        }
        
        // Verify expected classes
        assertTrue(generatedFiles.contains("Organization.java"), "Should generate Organization.java");
        assertTrue(generatedFiles.contains("OrganizationDepartment.java"), "Should generate OrganizationDepartment.java");
        assertTrue(generatedFiles.contains("OrganizationDepartmentEmployee.java"), "Should generate OrganizationDepartmentEmployee.java");
        
        // Find and verify Organization class
        JavaClassDef orgClass = Arrays.stream(classes)
            .filter(c -> c.getFilename().equals("Organization.java"))
            .findFirst().orElse(null);
        assertNotNull(orgClass);
        
        String orgCode = orgClass.getCode();
        assertTrue(orgCode.contains("private List<OrganizationDepartment> departments;"), 
                  "Organization should have List<OrganizationDepartment>");
        
        // Find and verify OrganizationDepartment class
        JavaClassDef deptClass = Arrays.stream(classes)
            .filter(c -> c.getFilename().equals("OrganizationDepartment.java"))
            .findFirst().orElse(null);
        assertNotNull(deptClass);
        
        String deptCode = deptClass.getCode();
        assertTrue(deptCode.contains("private List<OrganizationDepartmentEmployee> employees;"), 
                  "Department should have List<OrganizationDepartmentEmployee>");
        
        // Find and verify OrganizationDepartmentEmployee class
        JavaClassDef empClass = Arrays.stream(classes)
            .filter(c -> c.getFilename().equals("OrganizationDepartmentEmployee.java"))
            .findFirst().orElse(null);
        assertNotNull(empClass);
        
        String empCode = empClass.getCode();
        assertTrue(empCode.contains("private List<String> skills;"), 
                  "Employee should have List<String> skills");
        
        // Test compilation
        assertTrue(compileJavaCode(classes, "com.org"), "Generated code should compile");
        System.out.println("=== END TEST ===\n");
    }

    @Test
    @DisplayName("Test Order with customer object and enum status")
    void testOrderWithCustomerObjectAndEnum() throws Exception {
        String lineStruct = """
            EntityName:Order
            schema:id:int¦customer:‹name:string¦email:string›¦items:«‹product:string¦quantity:int¦price:float›»¦status:{pending|shipped|delivered}
            1¦‹John Doe¦john@example.com›¦«‹Laptop¦1¦999.99›¦‹Mouse¦2¦25.50›»¦pending
            2¦‹Jane Smith¦jane@test.com›¦«‹Book¦3¦15.99›»¦shipped
            """;

        System.out.println("\n=== TEST: Order with Customer Object and Enum ===");
        JavaClassDef[] classes = LineStructParser.generateJava(lineStruct, "com.sales");
        
        assertNotNull(classes);
        
        Set<String> generatedFiles = new HashSet<>();
        for (JavaClassDef classDef : classes) {
            generatedFiles.add(classDef.getFilename());
            System.out.println("  - " + classDef.getFilename());
        }
        
        // Verify classes
        assertTrue(generatedFiles.contains("Order.java"));
        assertTrue(generatedFiles.contains("OrderCustomer.java"));
        assertTrue(generatedFiles.contains("OrderItem.java"));
        
        // Check Order class
        JavaClassDef orderClass = Arrays.stream(classes)
            .filter(c -> c.getFilename().equals("Order.java"))
            .findFirst().orElse(null);
        assertNotNull(orderClass);
        
        String orderCode = orderClass.getCode();
        assertTrue(orderCode.contains("private OrderCustomer customer;"));
        assertTrue(orderCode.contains("private List<OrderItem> items;"));
        assertTrue(orderCode.contains("private String status;"), "Status enum should be String");
        
        // Check OrderCustomer class
        JavaClassDef customerClass = Arrays.stream(classes)
            .filter(c -> c.getFilename().equals("OrderCustomer.java"))
            .findFirst().orElse(null);
        assertNotNull(customerClass);
        
        String customerCode = customerClass.getCode();
        assertTrue(customerCode.contains("private String name;"));
        assertTrue(customerCode.contains("private String email;"));
        
        // Test compilation
        assertTrue(compileJavaCode(classes, "com.sales"), "Generated code should compile");
        System.out.println("=== END TEST ===\n");
    }

    @Test
    @DisplayName("Test simple Person with boolean field")
    void testSimplePersonWithBoolean() throws Exception {
        String lineStruct = String.join("\n",
                "EntityName:Person",
                "schema:id:int¦name:string¦active:bool",
                "1¦John Doe¦true",
                "2¦Jane Smith¦false"
        );

        System.out.println("\n=== TEST: Simple Person with Boolean ===");
        JavaClassDef[] classes = LineStructParser.generateJava(lineStruct, "com.people");
        
        assertNotNull(classes);
        assertEquals(1, classes.length);
        
        JavaClassDef personClass = classes[0];
        assertEquals("Person.java", personClass.getFilename());
        
        String code = personClass.getCode();
        assertTrue(code.contains("private boolean active;"));
        assertTrue(code.contains("public boolean isActive()"), "Boolean getter should use 'is' prefix");
        assertTrue(code.contains("public void setActive(boolean active)"));
        
        // Test compilation
        assertTrue(compileJavaCode(classes, "com.people"), "Generated code should compile");
        System.out.println("=== END TEST ===\n");
    }

    @Test
    @DisplayName("Test Employee with optional fields and enum")
    void testEmployeeWithOptionalFieldsAndEnum() throws Exception {
        String input = """
            EntityName:Employee
            schema:id:int¦details:‹name:string¦age:<int>?›¦roles:«string»¦status:{active|inactive}
            1¦‹Alice¦30›¦«admin¦user»¦active
            2¦‹Bob¦›¦«user»¦inactive
            """;

        System.out.println("\n=== TEST: Employee with Optional Fields and Enum ===");
        JavaClassDef[] classes = LineStructParser.generateJava(input, "com.hr");
        
        assertNotNull(classes);
        
        Set<String> generatedFiles = new HashSet<>();
        for (JavaClassDef classDef : classes) {
            generatedFiles.add(classDef.getFilename());
            System.out.println("  - " + classDef.getFilename());
        }
        
        assertTrue(generatedFiles.contains("Employee.java"));
        assertTrue(generatedFiles.contains("EmployeeDetails.java"));
        
        // Check EmployeeDetails class for optional age
        JavaClassDef detailsClass = Arrays.stream(classes)
            .filter(c -> c.getFilename().equals("EmployeeDetails.java"))
            .findFirst().orElse(null);
        assertNotNull(detailsClass);
        
        String detailsCode = detailsClass.getCode();
        assertTrue(detailsCode.contains("private String name;"));
        assertTrue(detailsCode.contains("private Integer age;"), "Optional int should be Integer");
        
        // Check Employee class
        JavaClassDef empClass = Arrays.stream(classes)
            .filter(c -> c.getFilename().equals("Employee.java"))
            .findFirst().orElse(null);
        assertNotNull(empClass);
        
        String empCode = empClass.getCode();
        assertTrue(empCode.contains("private EmployeeDetails details;"));
        assertTrue(empCode.contains("private List<String> roles;"));
        assertTrue(empCode.contains("private String status;"));
        
        // Test compilation
        assertTrue(compileJavaCode(classes, "com.hr"), "Generated code should compile");
        System.out.println("=== END TEST ===\n");
    }

    @Test
    @DisplayName("Test Profile with multiple optional types including date")
    void testProfileWithMultipleOptionalTypes() throws Exception {
        String structLine = """
            EntityName:Profile
            schema:id:int¦score:<float>?¦birth:<date>?¦member:<bool>?¦status:{active|inactive|suspended}¦extra:<‹note:<string>?›>?
            1¦3.14¦1990-01-01¦true¦active¦‹Hello›
            2¦¦¦¦inactive¦
            """;

        System.out.println("\n=== TEST: Profile with Multiple Optional Types ===");
        JavaClassDef[] classes = LineStructParser.generateJava(structLine, "com.profile");
        
        assertNotNull(classes);
        
        Set<String> generatedFiles = new HashSet<>();
        for (JavaClassDef classDef : classes) {
            generatedFiles.add(classDef.getFilename());
            System.out.println("  - " + classDef.getFilename());
        }
        
        // Check Profile class
        JavaClassDef profileClass = Arrays.stream(classes)
            .filter(c -> c.getFilename().equals("Profile.java"))
            .findFirst().orElse(null);
        assertNotNull(profileClass);
        
        String profileCode = profileClass.getCode();
        assertTrue(profileCode.contains("private int id;"));
        assertTrue(profileCode.contains("private Float score;"), "Optional float should be Float");
        assertTrue(profileCode.contains("private LocalDate birth;"), "Optional date should be LocalDate");
        assertTrue(profileCode.contains("private Boolean member;"), "Optional bool should be Boolean");
        assertTrue(profileCode.contains("private String status;"));
        assertTrue(profileCode.contains("private ProfileExtra extra;"), "Optional object field");
        
        // Check imports for date
        assertTrue(profileCode.contains("import java.time.LocalDate;"));
        
        // Check ProfileExtra class
        if (generatedFiles.contains("ProfileExtra.java")) {
            JavaClassDef extraClass = Arrays.stream(classes)
                .filter(c -> c.getFilename().equals("ProfileExtra.java"))
                .findFirst().orElse(null);
            assertNotNull(extraClass);
            
            String extraCode = extraClass.getCode();
            assertTrue(extraCode.contains("private String note;"), "Optional string in nested object");
        }
        
        // Test compilation
        assertTrue(compileJavaCode(classes, "com.profile"), "Generated code should compile");
        System.out.println("=== END TEST ===\n");
    }

    @Test
    @DisplayName("Test Product with complex pricing and multiple enums")
    void testProductWithComplexPricingAndEnums() throws Exception {
        String lineStruct = """
            EntityName:Product
            schema:id:int¦name:string¦category:{ELECTRONICS|CLOTHING|FOOD}¦description:<string>?¦tags:«string»¦pricing:‹regular:float¦discount:<float>?¦currency:{USD|EUR|GBP}›
            1¦Laptop¦ELECTRONICS¦High-performance laptop¦«portable¦powerful¦business»¦‹999.99¦100.00¦USD›
            2¦T-Shirt¦CLOTHING¦¦«casual¦cotton»¦‹29.99¦¦EUR›
            3¦Coffee¦FOOD¦Premium blend¦«organic¦fair-trade¦arabica»¦‹12.50¦2.50¦GBP›
            """;

        System.out.println("\n=== TEST: Product with Complex Pricing and Enums ===");
        JavaClassDef[] classes = LineStructParser.generateJava(lineStruct, "com.products");
        
        assertNotNull(classes);
        
        Set<String> generatedFiles = new HashSet<>();
        for (JavaClassDef classDef : classes) {
            generatedFiles.add(classDef.getFilename());
            System.out.println("  - " + classDef.getFilename());
        }
        
        assertTrue(generatedFiles.contains("Product.java"));
        assertTrue(generatedFiles.contains("ProductPricing.java"));
        
        // Check Product class
        JavaClassDef productClass = Arrays.stream(classes)
            .filter(c -> c.getFilename().equals("Product.java"))
            .findFirst().orElse(null);
        assertNotNull(productClass);
        
        String productCode = productClass.getCode();
        assertTrue(productCode.contains("private String category;"), "Category enum as String");
        assertTrue(productCode.contains("private String description;"), "Optional string");
        assertTrue(productCode.contains("private List<String> tags;"));
        assertTrue(productCode.contains("private ProductPricing pricing;"));
        
        // Check ProductPricing class
        JavaClassDef pricingClass = Arrays.stream(classes)
            .filter(c -> c.getFilename().equals("ProductPricing.java"))
            .findFirst().orElse(null);
        assertNotNull(pricingClass);
        
        String pricingCode = pricingClass.getCode();
        assertTrue(pricingCode.contains("private float regular;"));
        assertTrue(pricingCode.contains("private Float discount;"), "Optional float");
        assertTrue(pricingCode.contains("private String currency;"), "Currency enum as String");
        
        // Test compilation
        assertTrue(compileJavaCode(classes, "com.products"), "Generated code should compile");
        System.out.println("=== END TEST ===\n");
    }

    @Test
    @DisplayName("Test School with nested classes, students, grades and enum schedule")
    void testSchoolWithComplexNestedStructure() throws Exception {
        String lineStruct = """
            EntityName:School
            schema:id:int¦name:string¦classes:«‹classId:int¦subject:string¦students:«‹studentId:int¦name:string¦grades:«int»›»¦schedule:‹day:{MON|TUE|WED|THU|FRI}¦time:string››»
            1¦Central High¦«‹101¦Math¦«‹1¦John¦«95¦87¦92»›¦‹2¦Jane¦«88¦91¦94»›»¦‹MON¦09:00››¦‹102¦Science¦«‹3¦Bob¦«78¦82¦85»›»¦‹WED¦14:00››»
            """;

        System.out.println("\n=== TEST: School with Complex Nested Structure ===");
        JavaClassDef[] classes = LineStructParser.generateJava(lineStruct, "com.education");
        
        assertNotNull(classes);
        System.out.println("Number of classes generated: " + classes.length);
        
        Set<String> generatedFiles = new HashSet<>();
        for (JavaClassDef classDef : classes) {
            generatedFiles.add(classDef.getFilename());
            System.out.println("  - " + classDef.getFilename());
        }
        
        // Verify all expected classes
        assertTrue(generatedFiles.contains("School.java"));
        assertTrue(generatedFiles.contains("SchoolClass.java"));
        assertTrue(generatedFiles.contains("SchoolClassStudent.java"));
        assertTrue(generatedFiles.contains("SchoolClassSchedule.java"));
        
        // Check School class
        JavaClassDef schoolClass = Arrays.stream(classes)
            .filter(c -> c.getFilename().equals("School.java"))
            .findFirst().orElse(null);
        assertNotNull(schoolClass);
        
        String schoolCode = schoolClass.getCode();
        assertTrue(schoolCode.contains("private List<SchoolClass> classes;"));
        
        // Check SchoolClass
        JavaClassDef classClass = Arrays.stream(classes)
            .filter(c -> c.getFilename().equals("SchoolClass.java"))
            .findFirst().orElse(null);
        assertNotNull(classClass);
        
        String classCode = classClass.getCode();
        assertTrue(classCode.contains("private int classId;"));
        assertTrue(classCode.contains("private String subject;"));
        assertTrue(classCode.contains("private List<SchoolClassStudent> students;"));
        assertTrue(classCode.contains("private SchoolClassSchedule schedule;"));
        
        // Check SchoolClassStudent
        JavaClassDef studentClass = Arrays.stream(classes)
            .filter(c -> c.getFilename().equals("SchoolClassStudent.java"))
            .findFirst().orElse(null);
        assertNotNull(studentClass);
        
        String studentCode = studentClass.getCode();
        assertTrue(studentCode.contains("private int studentId;"));
        assertTrue(studentCode.contains("private String name;"));
        assertTrue(studentCode.contains("private List<Integer> grades;"));
        
        // Check SchoolClassSchedule
        JavaClassDef scheduleClass = Arrays.stream(classes)
            .filter(c -> c.getFilename().equals("SchoolClassSchedule.java"))
            .findFirst().orElse(null);
        assertNotNull(scheduleClass);
        
        String scheduleCode = scheduleClass.getCode();
        assertTrue(scheduleCode.contains("private String day;"), "Day enum as String");
        assertTrue(scheduleCode.contains("private String time;"));
        
        // Test compilation
        assertTrue(compileJavaCode(classes, "com.education"), "Generated code should compile");
        System.out.println("=== END TEST ===\n");
    }

    @Test
    @DisplayName("Test ComplexEntity with deeply nested arrays and mixed types")
    void testComplexEntityWithDeepNesting() throws Exception {
        String original = """
            EntityName:ComplexEntity
            schema:id:int¦data:‹values:«int»¦nested:‹deep:«‹key:string¦value:float›»››¦status:{ACTIVE|PENDING|DONE}
            1¦‹«10¦20¦30»¦‹«‹alpha¦1.5›¦‹beta¦2.7›»››¦ACTIVE
            2¦‹«40¦50»¦‹«‹gamma¦3.14›»››¦PENDING
            """;

        System.out.println("\n=== TEST: ComplexEntity with Deep Nesting ===");
        JavaClassDef[] classes = LineStructParser.generateJava(original, "com.complex");
        
        assertNotNull(classes);
        System.out.println("Number of classes generated: " + classes.length);
        
        Set<String> generatedFiles = new HashSet<>();
        for (JavaClassDef classDef : classes) {
            generatedFiles.add(classDef.getFilename());
            System.out.println("  - " + classDef.getFilename());
        }
        
        // Verify expected classes
        assertTrue(generatedFiles.contains("ComplexEntity.java"));
        assertTrue(generatedFiles.contains("ComplexEntityData.java"));
        assertTrue(generatedFiles.contains("ComplexEntityDataNested.java"));
        assertTrue(generatedFiles.contains("ComplexEntityDataNestedDeep.java"));
        
        // Check ComplexEntity class
        JavaClassDef entityClass = Arrays.stream(classes)
            .filter(c -> c.getFilename().equals("ComplexEntity.java"))
            .findFirst().orElse(null);
        assertNotNull(entityClass);
        
        String entityCode = entityClass.getCode();
        assertTrue(entityCode.contains("private int id;"));
        assertTrue(entityCode.contains("private ComplexEntityData data;"));
        assertTrue(entityCode.contains("private String status;"));
        
        // Check ComplexEntityData class
        JavaClassDef dataClass = Arrays.stream(classes)
            .filter(c -> c.getFilename().equals("ComplexEntityData.java"))
            .findFirst().orElse(null);
        assertNotNull(dataClass);
        
        String dataCode = dataClass.getCode();
        assertTrue(dataCode.contains("private List<Integer> values;"));
        assertTrue(dataCode.contains("private ComplexEntityDataNested nested;"));
        
        // Check ComplexEntityDataNested class
        JavaClassDef nestedClass = Arrays.stream(classes)
            .filter(c -> c.getFilename().equals("ComplexEntityDataNested.java"))
            .findFirst().orElse(null);
        assertNotNull(nestedClass);
        
        String nestedCode = nestedClass.getCode();
        assertTrue(nestedCode.contains("private List<ComplexEntityDataNestedDeep> deep;"));
        
        // Check ComplexEntityDataNestedDeep class
        JavaClassDef deepClass = Arrays.stream(classes)
            .filter(c -> c.getFilename().equals("ComplexEntityDataNestedDeep.java"))
            .findFirst().orElse(null);
        assertNotNull(deepClass);
        
        String deepCode = deepClass.getCode();
        assertTrue(deepCode.contains("private String key;"));
        assertTrue(deepCode.contains("private float value;"));
        
        // Test compilation
        assertTrue(compileJavaCode(classes, "com.complex"), "Generated code should compile");
        System.out.println("=== END TEST ===\n");
    }

    @Test
    @DisplayName("Test all primitive data types")
    void testAllPrimitiveDataTypes() throws Exception {
        String lineStruct = """
            EntityName:DataTypes
            schema:id:int¦name:string¦price:float¦active:bool¦birthDate:date¦lastLogin:datetime¦optInt:<int>?¦optFloat:<float>?¦optBool:<bool>?¦optString:<string>?¦optDate:<date>?¦optDateTime:<datetime>?
            1¦Test¦99.99¦true¦2024-01-15¦2024-01-15T10:30:00Z¦42¦3.14¦false¦optional¦2024-02-20¦2024-02-20T15:45:00Z
            2¦Test2¦19.99¦false¦2024-03-10¦2024-03-10T08:00:00Z¦¦¦¦¦¦
            """;

        System.out.println("\n=== TEST: All Primitive Data Types ===");
        JavaClassDef[] classes = LineStructParser.generateJava(lineStruct, "com.datatypes");
        
        assertNotNull(classes);
        assertEquals(1, classes.length);
        
        JavaClassDef dataTypesClass = classes[0];
        String code = dataTypesClass.getCode();
        
        // Check required primitives
        assertTrue(code.contains("private int id;"));
        assertTrue(code.contains("private String name;"));
        assertTrue(code.contains("private float price;"));
        assertTrue(code.contains("private boolean active;"));
        assertTrue(code.contains("private LocalDate birthDate;"));
        assertTrue(code.contains("private LocalDateTime lastLogin;"));
        
        // Check optional types (should use wrapper classes)
        assertTrue(code.contains("private Integer optInt;"));
        assertTrue(code.contains("private Float optFloat;"));
        assertTrue(code.contains("private Boolean optBool;"));
        assertTrue(code.contains("private String optString;"));
        assertTrue(code.contains("private LocalDate optDate;"));
        assertTrue(code.contains("private LocalDateTime optDateTime;"));
        
        // Check imports
        assertTrue(code.contains("import java.time.LocalDate;"));
        assertTrue(code.contains("import java.time.LocalDateTime;"));
        
        // Test compilation
        assertTrue(compileJavaCode(classes, "com.datatypes"), "Generated code should compile");
        System.out.println("=== END TEST ===\n");
    }
}