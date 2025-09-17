

# LineStruct Parser

A Java library for parsing and converting data in the LineStruct format, a custom UTF-8 encoded format for structured data. It supports bidirectional conversion between LineStruct, JSON, YAML, and Java class generation, with comprehensive validation and formatting capabilities.

## Features

- **Parsing**: Parse LineStruct data into an Abstract Syntax Tree (AST) for structured data manipulation.
- **Conversion**: Convert between LineStruct, JSON, and YAML formats.
- **Validation**: Validate LineStruct data against schemas and detect data type issues.
- **Formatting**: Pretty-print LineStruct schemas for readability and compact them for storage.
- **Java Code Generation**: Generate Java classes from LineStruct schemas.
- **Comprehensive Testing**: Extensive JUnit tests covering simple, nested, optional, and large datasets.

## LineStruct Format

LineStruct is a UTF-8 encoded format with the following structure:

- **Line 1**: `EntityName:<name>`
- **Line 2**: `schema:<field definitions>`
- **Lines 3+**: Data rows

### Character Definitions
- Field delimiter: `¦` (U+00A6)
- Nested object start/end: `‹` (U+2039) / `›` (U+203A)
- Array start/end: `«` (U+00AB) / `»` (U+00BB)
- Enum delimiter: `|` (U+007C)
- Escape character: `⁊` (U+204A)

### Schema Definition
- **Primitive Types**: `string`, `int`, `float`, `bool` (`true`/`false`), `date` (ISO 8601: YYYY-MM-DD), `datetime` (ISO 8601: YYYY-MM-DDTHH:MM:SSZ)
- **Arrays**: `«type»`
- **Objects**: `‹field:type¦field:type¦...›`
- **Enums**: `{value|value|...}`
- **Optional Fields**: `<type>?`
- **Field Descriptions**: `field:type@desc="description"`

### Data Format
- Fields are separated by `¦`.
- Arrays: `«value¦value¦...»`
- Objects: `‹value¦value¦...›`
- Optional fields can be empty or omitted at the end of a row.
- Escape sequences (e.g., `⁊¦` for literal `¦`) ensure unambiguous parsing.

### Example
```
EntityName:Person
schema:id:int¦name:string¦age:int¦active:bool
1¦John Doe¦30¦true
2¦Jane Smith¦25¦false
```

## Installation

Clone the repository and build with Maven:

```bash
git clone https://github.com/yourusername/linestruct-parser.git
cd linestruct-parser
mvn clean install
```

Add the dependency to your Maven project:

```xml
<dependency>
    <groupId>com.bentzn.util</groupId>
    <artifactId>linestruct-parser</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## Usage

### Converting LineStruct to JSON
```java
String lineStruct = """
    EntityName:Person
    schema:id:int¦name:string¦age:int
    1¦John Doe¦30
    2¦Jane Smith¦25
    """;
String json = LineStructParser.toJson(lineStruct);
System.out.println(json);
// Output: {"entityName":"Person","data":[{"id":1,"name":"John Doe","age":30},{"id":2,"name":"Jane Smith","age":25}]}
```

### Converting JSON to LineStruct
```java
String json = "{\"entityName\":\"Person\",\"data\":[{\"id\":1,\"name\":\"John Doe\",\"age\":30}]}";
String lineStruct = LineStructParser.fromJson(json);
System.out.println(lineStruct);
// Output:
// EntityName:Person
// schema:id:int¦name:string¦age:int
// 1¦John Doe¦30
```

### Generating YAML Schema
```java
String lineStruct = """
    EntityName:Person
    schema:id:int¦name:string¦active:bool
    1¦John Doe¦true
    """;
String yaml = LineStructParser.generateYaml(lineStruct);
System.out.println(yaml);
// Output:
// entityName: Person
// fields:
//   id:
//     type: int
//     required: true
//   name:
//     type: string
//     required: true
//   active:
//     type: bool
//     required: true
```

### Pretty-Printing Schema
```java
String schema = "schema:id:int¦person:‹name:string¦age:int›";
String pretty = LineStructParser.prettyPrintSchema(schema);
System.out.println(pretty);
// Output:
// schema:
//   id:int
//   person:
//   ‹
//     name:string
//     age:int
//   ›
```

### Generating Java Classes
```java
String lineStruct = """
    EntityName:Person
    schema:id:int¦name:string
    1¦John Doe
    """;
JavaClassDef[] classes = LineStructParser.generateJava(lineStruct, "com.example");
System.out.println(classes[0].getCode());
// Output: Java class definition for Person
```

## Project Structure

- **Core Classes**:
  - `LineStructParser.java`: Main public interface for LineStruct operations.
  - `SchemaParser.java`: Parses schema definitions into AST.
  - `LineStructFormatter.java`: Formats schemas (pretty-print and compact).
  - `YamlGenerator.java`: Handles YAML schema generation and parsing.
  - `JsonGenerator.java`: Converts LineStruct to JSON with sampling and trimming options.

- **Test Classes**:
  - `GenerateSchemaTest.java`: Tests YAML to LineStruct schema conversion.
  - `JsonToLineStructTest.java`: Tests JSON to LineStruct conversion.
  - `Grok3LineStructParserTest.java`, `Grok4LineStructParserTest.java`, etc.: Comprehensive tests for various scenarios.
  - `LineStructPrettyPrintTest.java`: Tests schema formatting.
  - `LineStructParserComplexTest.java`: Tests complex nested structures.
  - `LargeLineStructTest.java`: Tests large datasets with 200+ records.

## Testing

Run tests using Maven:

```bash
mvn test
```

The test suite covers:
- Basic conversions (LineStruct ↔ JSON/YAML)
- Nested objects and arrays
- Optional fields and enums
- Date/time validation
- Large dataset handling
- Edge cases (empty inputs, invalid formats)

## Dependencies

- **Jackson Databind**: For JSON processing.
- **SnakeYAML**: For YAML processing.
- **JUnit 5**: For testing.

```xml
<dependencies>
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.17.2</version>
    </dependency>
    <dependency>
        <groupId>org.yaml</groupId>
        <artifactId>snakeyaml</artifactId>
        <version>2.2</version>
    </dependency>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.10.3</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

## Contributing

Contributions are welcome! Please submit issues or pull requests on GitHub. Ensure new code includes tests and follows the formatting guidelines (see `formatter.xml`).

## License

MIT License

Copyright (c) 2025 bentzn

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
