package dev.matteuo.codegen;

import java.util.List;

/**
 * SimpleClassGenerator is a utility class for generating Java class definitions.
 */
public class SimpleClassGenerator {

    /**
     * Default constructor for SimpleClassGenerator.
     */
    public SimpleClassGenerator() {
    }

    /**
     * Generates a Java class definition based on the provided attributes and class name.
     *
     * @param attributes List of attribute names to be included in the class.
     * @param className  The name of the class to be generated.
     * @return A string representing the Java class definition.
     * @throws Exception If an error occurs during class generation.
     */
    public String generateJavaClass(List<String> attributes, String className) throws Exception {

        StringBuilder classBuilder = new StringBuilder();
        classBuilder.append("// This string is generated to create a Java class\n");
        classBuilder.append("public class ").append(className).append(" {\n");

        // Attribute declarations
        for (String attribute : attributes) {
            classBuilder.append("    private String ").append(attribute).append(";\n");
        }
        classBuilder.append("\n");

        // Getters and Setters
        for (String attribute : attributes) {
            // Getter
            classBuilder.append("    public String get").append(capitalize(attribute)).append("() {\n");
            classBuilder.append("        return ").append(attribute).append(";\n");
            classBuilder.append("    }\n\n");

            // Setter
            classBuilder.append("    public void set").append(capitalize(attribute)).append("(String ").append(attribute).append(") {\n");
            classBuilder.append("        this.").append(attribute).append(" = ").append(attribute).append(";\n");
            classBuilder.append("    }\n\n");
        }
        classBuilder.append("}\n");

        classBuilder.append("// End of the generated string to create a Java class\n");

        return classBuilder.toString();
    }

    /**
     * Capitalizes the first letter of the given string.
     *
     * @param str The string to be capitalized.
     * @return The capitalized string.
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
