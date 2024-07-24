package dev.matteuo.ldap.utility;

import dev.matteuo.codegen.SimpleClassGenerator;
import dev.matteuo.ldap.constants.LDAPConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.ldap.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LDAPUtility {

    private static final Logger logger = LoggerFactory.getLogger(LDAPUtility.class);
    private final String url;
    private final boolean useSsl;

    /**
     * Constructor for LDAPUtility with SSL enabled by default.
     *
     * @param url The URL of the LDAP server.
     */
    public LDAPUtility(String url) {
        this(url, true);
    }

    /**
     * Constructor for LDAPUtility.
     *
     * @param url    The URL of the LDAP server.
     * @param useSsl A boolean indicating whether to use SSL.
     */
    public LDAPUtility(String url, boolean useSsl) {
        this.url = url;
        this.useSsl = useSsl;
    }

    /**
     * Creates an authenticated LDAP context using the provided principal and credentials.
     *
     * @param principal   The security principal (bind DN).
     * @param credentials The security credentials (password).
     * @return An initialized DirContext.
     * @throws Exception If an error occurs while creating the context.
     */
    private DirContext createContextAuth(String principal, String credentials) throws Exception {
        Hashtable<String, String> env = new Hashtable<>();

        // Conversion to UTF-8
        String asciiEncodedString = new String(credentials.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, url);
        if(useSsl){
            env.put(Context.SECURITY_PROTOCOL, "ssl");
        }
        env.put(Context.SECURITY_AUTHENTICATION, LDAPConstants.SECURITY_AUTHENTICATION_SIMPLE);
        env.put(Context.SECURITY_PRINCIPAL, principal);
        env.put(Context.SECURITY_CREDENTIALS, asciiEncodedString);

        try {
            logger.info("Creating authenticated LDAP context with principal: " + principal);
            return new InitialDirContext(env);
        } catch (NamingException e) {
            logger.error("Failed to create authenticated LDAP context: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error while creating authenticated LDAP context: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Authenticates a user against the LDAP server and maps the results to an instance of the specified class.
     *
     * @param <T>         The type of the class to map the search results to.
     * @param baseDn      The base DN to start the search.
     * @param filter      The search filter.
     * @param credentials The credentials for authentication.
     * @param clazz       The class to map the search results to.
     * @return An instance of the specified class with the search results.
     * @throws Exception If an error occurs during the authentication or object instantiation.
     */
    public <T> T authentication(String baseDn, String filter, String credentials, Class<T> clazz) throws Exception {
        T resultObj = null;
        DirContext ctx = null;
        NamingEnumeration<SearchResult> answer = null;

        try {
            ctx = createContextAuth(baseDn, credentials);

            SearchControls ctls = new SearchControls();
            ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);

            answer = ctx.search(baseDn, filter, ctls);

            if (answer.hasMoreElements()) {
                SearchResult sr = answer.nextElement();
                Attributes result = sr.getAttributes();

                if (result == null) {
                    logger.error("Valid person object not found");
                } else {
                    resultObj = mapAttributesToObject(result, clazz);
                }
            }
        } catch (NamingException | IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException e) {
            logger.error("LDAP operation failed", e);
            throw e;
        } finally {
            closeResources(ctx, answer);
        }

        return resultObj;
    }

    /**
     * Maps LDAP attributes to an instance of the specified class.
     *
     * @param <T>        The type of the class to map the attributes to.
     * @param attributes The LDAP attributes to map.
     * @param clazz      The class to map the attributes to.
     * @return An instance of the specified class with the mapped attributes.
     * @throws NamingException             If an error occurs while accessing the LDAP attributes.
     * @throws InstantiationException      If an error occurs while instantiating the class.
     * @throws IllegalAccessException      If an error occurs while accessing the fields of the class.
     * @throws InvocationTargetException   If an error occurs while invoking the constructor of the class.
     * @throws NoSuchMethodException       If the specified class does not have a default constructor.
     */
    public static <T> T mapAttributesToObject(Attributes attributes, Class<T> clazz)
            throws NamingException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        T resultObj = clazz.getDeclaredConstructor().newInstance();
        NamingEnumeration<? extends Attribute> allAttributes = attributes.getAll();

        while (allAttributes.hasMore()) {
            Attribute attribute = allAttributes.next();
            String attributeName = attribute.getID();
            String attributeValue = attribute.get().toString();

            try {
                Field field = null;
                try {
                    field = clazz.getDeclaredField(attributeName);
                    field.setAccessible(true);
                    field.set(resultObj, attributeValue);
                } catch (NoSuchFieldException e) {
                    logger.warn("Attribute not found in class: " + attributeName);
                }
            } catch (IllegalAccessException e) {
                logger.error("Error accessing field: " + attributeName, e);
            } catch (Exception e) {
                logger.error("Unexpected error while mapping attribute: " + attributeName + " - " + e.getMessage());
                throw new RuntimeException("Unexpected error while mapping attribute: " + attributeName, e);
            }
        }

        return resultObj;
    }

    /**
     * Closes the LDAP resources, including the DirContext and NamingEnumeration.
     *
     * @param ctx    The DirContext to be closed.
     * @param answer The NamingEnumeration to be closed.
     */
    private void closeResources(DirContext ctx, NamingEnumeration<?> answer) {
        if (answer != null) {
            try {
                answer.close();
            } catch (Exception e) {
                logger.error("Error closing search result", e);
            }
        }

        if (ctx != null) {
            try {
                ctx.close();
            } catch (Exception e) {
                logger.error("Error closing context connection", e);
            }
        }
    }

    /**
     * Creates an LDAP context for search operations.
     *
     * @return An initialized LdapContext.
     * @throws Exception If an error occurs while creating the context.
     */
    private LdapContext createContextSearch() throws Exception {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, url);
        if(useSsl){
            env.put(Context.SECURITY_PROTOCOL, "ssl");
        }
        env.put(Context.SECURITY_AUTHENTICATION, LDAPConstants.SECURITY_AUTHENTICATION_NONE);

        try {
            logger.info("Creating LDAP context with URL: " + url + " and SSL: " + useSsl);
            return new InitialLdapContext(env, null);
        } catch (NamingException e) {
            logger.error("Failed to create LDAP context: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error while creating LDAP context: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Searches an LDAP directory with default parameters and maps the results to instances of the specified class.
     *
     * @param <T>    The type of the class to map the search results to.
     * @param baseDn The base DN to start the search.
     * @param filter The search filter.
     * @param clazz  The class to map the search results to.
     * @return A list of instances of the specified class with the search results.
     * @throws Exception If an error occurs during the search or object instantiation.
     */
    public <T> List<T> search(String baseDn, String filter, Class<T> clazz) throws Exception {
        return search(baseDn, filter, clazz, 1000, 1000, LDAPConstants.SEARCH_SCOPE_SUBTREE);
    }

    /**
     * Searches an LDAP directory and maps the results to instances of the specified class.
     *
     * @param <T>           The type of the class to map the search results to.
     * @param baseDn        The base DN to start the search.
     * @param filter        The search filter.
     * @param clazz         The class to map the search results to.
     * @param limitResults  The maximum number of results to return.
     * @param pageSize      The number of results per page.
     * @param searchScope   The scope of the search.
     * @return A list of instances of the specified class with the search results.
     * @throws Exception If an error occurs during the search or object instantiation.
     */
    public <T> List<T> search(String baseDn, String filter, Class<T> clazz, int limitResults, int pageSize, int searchScope) throws Exception {
        LdapContext ctx = null;
        List<T> results = new ArrayList<>();

        try {
            ctx = createContextSearch();

            // Use reflection to get attribute names from the fields of the class
            Field[] fields = clazz.getDeclaredFields();
            String[] attributeNames = new String[fields.length];
            for (int i = 0; i < fields.length; i++) {
                attributeNames[i] = fields[i].getName();
            }

            // Search controls
            SearchControls ctls = new SearchControls();
            ctls.setReturningAttributes(attributeNames);
            ctls.setSearchScope(searchScope);

            byte[] cookie = null;
            int totalResults = 0;

            do {
                ctx.setRequestControls(new Control[]{new PagedResultsControl(pageSize, cookie, Control.CRITICAL)});
                NamingEnumeration<SearchResult> answer = ctx.search(baseDn, filter, ctls);

                try {
                    while (answer.hasMoreElements()) {
                        if (limitResults != -1 && totalResults >= limitResults) {
                            break;
                        }
                        SearchResult sr = answer.nextElement();
                        Attributes attrs = sr.getAttributes();

                        T obj = clazz.getDeclaredConstructor().newInstance();
                        for (String attrName : attributeNames) {
                            if (attrs.get(attrName) != null) {
                                String value = attrs.get(attrName).get().toString();
                                Field field = clazz.getDeclaredField(attrName);
                                field.setAccessible(true);
                                field.set(obj, value);
                            }
                        }
                        results.add(obj);
                        totalResults++;
                    }

                    if (totalResults >= limitResults) {
                        break;
                    }

                    // Cookie for the next page
                    Control[] controls = ctx.getResponseControls();
                    if (controls != null) {
                        for (Control control : controls) {
                            if (control instanceof PagedResultsResponseControl) {
                                PagedResultsResponseControl prrc = (PagedResultsResponseControl) control;
                                cookie = prrc.getCookie();
                            }
                        }
                    }
                } finally {
                    if (answer != null) {
                        try {
                            answer.close();
                        } catch (Exception e) {
                            logger.error("Error closing search result: " + e.getMessage());
                        }
                    }
                }
            } while (cookie != null);

        } catch (Exception e) {
            logger.error("LDAP search operation failed: " + e.getMessage());
            throw e;
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (Exception e) {
                    logger.error("Error closing context connection: " + e.getMessage());
                }
            }
        }

        return results;
    }

    /**
     * Generates a Java class definition based on the provided LDAP search parameters.
     *
     * @param baseDn       The base DN to start the search.
     * @param filter       The search filter.
     * @param limitResults The maximum number of results to return.
     * @param pageSize     The number of results per page.
     * @param searchScope  The scope of the search.
     * @param className    The name of the class to be generated.
     * @return A string representing the Java class definition.
     * @throws Exception If an error occurs during the LDAP search or class generation.
     */
    public String generateJavaClass(String baseDn, String filter, int limitResults, int pageSize, int searchScope, String className) throws Exception {
        List<String> attributes = getDistinctAttributes(baseDn, filter, limitResults, pageSize, searchScope);
        SimpleClassGenerator scg = new SimpleClassGenerator();
        return scg.generateJavaClass(attributes, className);
    }

    /**
     * Retrieves distinct attributes from an LDAP directory.
     *
     * @param baseDn       The base DN to start the search.
     * @param filter       The search filter.
     * @param limitResults The maximum number of results to return.
     * @param pageSize     The number of results per page.
     * @param searchScope  The scope of the search.
     * @return A list of distinct attribute names found in the search.
     * @throws Exception If an error occurs during the search.
     */
    public List<String> getDistinctAttributes(String baseDn, String filter, int limitResults, int pageSize, int searchScope) throws Exception {
        LdapContext ctx = null;
        Set<String> attributesSet = new HashSet<>();

        try {
            ctx = createContextSearch();

            // Search controls
            SearchControls ctls = new SearchControls();
            ctls.setReturningAttributes(null); // Return all attributes
            ctls.setSearchScope(searchScope);

            byte[] cookie = null;
            int totalResults = 0;

            do {
                ctx.setRequestControls(new Control[]{new PagedResultsControl(pageSize, cookie, Control.CRITICAL)});
                NamingEnumeration<SearchResult> answer = ctx.search(baseDn, filter, ctls);

                try {
                    while (answer.hasMoreElements()) {
                        if (limitResults != -1 && totalResults >= limitResults) {
                            break;
                        }
                        SearchResult sr = answer.nextElement();
                        Attributes attrs = sr.getAttributes();

                        // Add all attribute names to the set
                        NamingEnumeration<? extends Attribute> allAttrs = attrs.getAll();
                        while (allAttrs.hasMore()) {
                            Attribute attr = allAttrs.next();
                            attributesSet.add(attr.getID());
                        }
                        totalResults++;
                    }

                    if (totalResults >= limitResults) {
                        break;
                    }

                    // Cookie for the next page
                    Control[] controls = ctx.getResponseControls();
                    if (controls != null) {
                        for (Control control : controls) {
                            if (control instanceof PagedResultsResponseControl) {
                                PagedResultsResponseControl prrc = (PagedResultsResponseControl) control;
                                cookie = prrc.getCookie();
                            }
                        }
                    }
                } finally {
                    if (answer != null) {
                        try {
                            answer.close();
                        } catch (Exception e) {
                            logger.error("Error closing search result: " + e.getMessage());
                        }
                    }
                }
            } while (cookie != null);

        } catch (Exception e) {
            logger.error("LDAP search operation failed: " + e.getMessage());
            throw e;
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (Exception e) {
                    logger.error("Error closing context connection: " + e.getMessage());
                }
            }
        }

        // Convert the Set to a List before returning
        return new ArrayList<>(attributesSet);
    }

}
