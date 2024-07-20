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

public class LDAPUtilityTest {

    private InMemoryDirectoryServer server;
    private LDAPUtility ldapUtility;

    @Before
    public void setUp() throws Exception {
        // Configurazione del server LDAP in memoria
        InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig("dc=example,dc=com");
        config.addAdditionalBindCredentials("cn=Directory Manager", "password");
        server = new InMemoryDirectoryServer(config);
        server.startListening();

        // Creazione dell'entry parent
        server.add("dn: dc=example,dc=com",
                "objectClass: domain",
                "dc: example");

        // Popolamento del server con dati di test
        server.add("dn: cn=John Doe,dc=example,dc=com",
                "objectClass: inetOrgPerson",
                "cn: John Doe",
                "sn: Doe",
                "mail: john.doe@example.com",
                "userPassword: password"); // Aggiungi la password per John Doe

        server.add("dn: cn=Jane Doe,dc=example,dc=com",
                "objectClass: inetOrgPerson",
                "cn: Jane Doe",
                "sn: Doe",
                "mail: jane.doe@example.com");

        // Inizializzazione di LDAPUtility
        ldapUtility = new LDAPUtility("ldap://localhost:" + server.getListenPort());
    }

    @After
    public void tearDown() throws Exception {
        server.shutDown(true);
    }

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
