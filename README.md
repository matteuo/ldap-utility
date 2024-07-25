# LDAP Utility Library

This project provides a utility library for working with LDAP (Lightweight Directory Access Protocol) servers. It includes functionality for performing LDAP searches, authenticating users, and mapping LDAP attributes to Java objects.

## Table of Contents

- [Features](#features)
- [Installation](#installation)
- [Usage](#usage)
- [Testing](#testing)
- [Contributing](#contributing)
- [License](#license)

## Features

- **LDAP Search**: Perform LDAP searches and map results to Java objects.
- **User Authentication**: Authenticate users against an LDAP server.
- **Class Generation**: Generate Java classes dynamically based on LDAP attributes.
- **In-Memory LDAP Server**: Use an in-memory LDAP server for testing purposes.

## Installation

To use this library in your project, you can include it as a dependency in your `pom.xml` if you're using Maven:

```xml
<dependency>
    <groupId>dev.matteuo</groupId>
    <artifactId>ldap-utility</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Usage

### LDAPUtility

The `LDAPUtility` class provides methods for performing LDAP searches and user authentication.

#### Example: Performing an LDAP Search

```java
LDAPUtility ldapUtility = new LDAPUtility("ldap://localhost:389");
String baseDn = "dc=example,dc=com";
String filter = "(sn=Doe)";
List<LDAPObject> results = ldapUtility.search(baseDn, filter, LDAPObject.class);

for (LDAPObject obj : results) {
    System.out.println("CN: " + obj.getCn());
    System.out.println("SN: " + obj.getSn());
    System.out.println("Mail: " + obj.getMail());
}
```

#### Example: User Authentication

```java
LDAPUtility ldapUtility = new LDAPUtility("ldap://localhost:389");
String baseDn = "cn=John Doe,dc=example,dc=com";
String filter = "(cn=John Doe)";
String credentials = "password";
LDAPObject ldapObject = ldapUtility.authentication(baseDn, filter, credentials, LDAPObject.class);

if (ldapObject != null) {
    System.out.println("Authentication successful for: " + ldapObject.getCn());
}
```

### SimpleClassGenerator

The `SimpleClassGenerator` class provides a method for generating Java class source code based on a list of attributes.

#### Example: Generating a Java Class

```java
SimpleClassGenerator generator = new SimpleClassGenerator();
List<String> attributes = Arrays.asList("cn", "sn", "mail");
String className = "GeneratedClass";
String classSource = generator.generateJavaClass(attributes, className);
System.out.println(classSource);
```

## Testing

This project includes unit tests that use an in-memory LDAP server for testing purposes.

### Running Tests

To run the tests, use the following command:

```bash
mvn test
```

### Test Coverage

- **LDAPUtilityTest**: Tests for `LDAPUtility` methods including `search`, `authentication`, and `getDistinctAttributes`.
- **SimpleClassGeneratorTest**: Tests for `SimpleClassGenerator` method `generateJavaClass`.

### Example Test: LDAPUtilityTest

```java
@Test
public void testGetDistinctAttributes() throws Exception {
    String baseDn = "dc=example,dc=com";
    String filter = "(objectClass=inetOrgPerson)";

    Set<String> distinctAttributes = ldapUtility.getDistinctAttributes(baseDn, filter, 1000, 1000, LDAPConstants.SEARCH_SCOPE_SUBTREE);

    assertNotNull(distinctAttributes);
    assertTrue(distinctAttributes.contains("cn"));
    assertTrue(distinctAttributes.contains("sn"));
    assertTrue(distinctAttributes.contains("mail"));
    assertTrue(distinctAttributes.contains("objectClass"));
    assertTrue(distinctAttributes.contains("userPassword"));
}
```

## Contributing

Contributions are welcome! Please open an issue or submit a pull request on GitHub.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.