package dev.matteuo.ldap.utility;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import dev.matteuo.ldap.model.LDAPObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.naming.NamingException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for the LDAPUtility class.
 */
public class LDAPUtilityTest {

    private InMemoryDirectoryServer server;
    private LDAPUtility ldapUtility;

    /**
     * Sets up the in-memory LDAP server and populates it with test data before each test.
     *
     * @throws Exception If an error occurs during setup.
     */
    @Before
    public void setUp() throws Exception {
        // Configuration of the in-memory LDAP server
        InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig("dc=example,dc=com");
        config.addAdditionalBindCredentials("cn=Directory Manager", "password");
        server = new InMemoryDirectoryServer(config);
        server.startListening();

        // Creation of the parent entry
        server.add("dn: dc=example,dc=com",
                "objectClass: domain",
                "dc: example");

        // Populating the server with test data
        server.add("dn: cn=John Doe,dc=example,dc=com",
                "objectClass: inetOrgPerson",
                "cn: John Doe",
                "sn: Doe",
                "mail: john.doe@example.com",
                "userPassword: password"); // Add password for John Doe

        server.add("dn: cn=Jane Doe,dc=example,dc=com",
                "objectClass: inetOrgPerson",
                "cn: Jane Doe",
                "sn: Doe",
                "mail: jane.doe@example.com");

        // Initialization of LDAPUtility
        ldapUtility = new LDAPUtility("ldap://localhost:" + server.getListenPort());
    }

    /**
     * Shuts down the in-memory LDAP server after each test.
     *
     * @throws Exception If an error occurs during shutdown.
     */
    @After
    public void tearDown() throws Exception {
        server.shutDown(true);
    }

    /**
     * Tests the authentication method of LDAPUtility.
     *
     * @throws Exception If an error occurs during the test.
     */
    @Test
    public void testAuthentication() throws Exception {
        String baseDn = "cn=John Doe,dc=example,dc=com";
        String filter = "(cn=John Doe)";
        String credentials = "password";

        LDAPObject ldapObject = ldapUtility.authentication(baseDn, filter, credentials, LDAPObject.class);

        assertNotNull(ldapObject);
        assertEquals("John Doe", ldapObject.getCn());
        assertEquals("Doe", ldapObject.getSn());
        assertEquals("john.doe@example.com", ldapObject.getMail());
    }

    /**
     * Tests the search method of LDAPUtility.
     *
     * @throws Exception If an error occurs during the test.
     */
    @Test
    public void testSearch() throws Exception {
        String baseDn = "dc=example,dc=com";
        String filter = "(sn=Doe)";

        List<LDAPObject> results = ldapUtility.search(baseDn, filter, LDAPObject.class);

        assertEquals(2, results.size());

        LDAPObject john = results.stream().filter(obj -> "John Doe".equals(obj.getCn())).findFirst().orElse(null);
        LDAPObject jane = results.stream().filter(obj -> "Jane Doe".equals(obj.getCn())).findFirst().orElse(null);

        assertNotNull(john);
        assertNotNull(jane);
    }
}
