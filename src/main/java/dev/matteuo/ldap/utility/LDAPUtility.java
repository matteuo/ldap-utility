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

    private String url;

    public LDAPUtility(String url) throws NamingException {
        this.url = url;
    }

    private DirContext createContextAuth(String principal, String credentials) throws NamingException {
        Hashtable<String, String> env = new Hashtable<>();

        // Conversione in UTF_8
        String asciiEncodedString = new String(credentials.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, url);
        //env.put(Context.SECURITY_PROTOCOL, "ssl");
        env.put(Context.SECURITY_AUTHENTICATION, LDAPConstants.SECURITY_AUTHENTICATION_SIMPLE);
        env.put(Context.SECURITY_PRINCIPAL, principal);
        env.put(Context.SECURITY_CREDENTIALS, asciiEncodedString);

        return new InitialDirContext(env);
    }

    public <T> T authentication(String baseDn, String filter, String credentials, Class<T> clazz) throws NamingException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
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
            }
        }

        return resultObj;
    }

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

    private LdapContext createContextSearch() throws NamingException {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, url);
        //env.put(Context.SECURITY_PROTOCOL, "ssl");
        env.put(Context.SECURITY_AUTHENTICATION, LDAPConstants.SECURITY_AUTHENTICATION_NONE);
        return new InitialLdapContext(env, null);
    }

    public <T> List<T> search(String baseDn, String filter, Class<T> clazz) throws Exception {
        return search(baseDn, filter, clazz, 1000, 1000, LDAPConstants.SEARCH_SCOPE_SUBTREE);
    }

    // Metodo di ricerca generico con parametro limite
    public <T> List<T> search(String baseDn, String filter, Class<T> clazz, int limitResults, int pageSize, int searchScope) throws Exception {
        LdapContext ctx = null;
        List<T> results = new ArrayList<>();

        try {
            ctx = createContextSearch();

            // Reflection per ottenere i nomi degli attributi dai campi della classe
            Field[] fields = clazz.getDeclaredFields();
            String[] attributeNames = new String[fields.length];
            for (int i = 0; i < fields.length; i++) {
                attributeNames[i] = fields[i].getName();
            }

            // Controlli di ricerca
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

                    // Cookie per la pagina successiva
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
                            logger.error("Error closing search result", e);
                        }
                    }
                }
            } while (cookie != null);

        } catch (Exception e) {
            logger.error("LDAP search operation failed", e);
            throw e;
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (Exception e) {
                    logger.error("Error closing context connection", e);
                }
            }
        }

        return results;
    }

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
