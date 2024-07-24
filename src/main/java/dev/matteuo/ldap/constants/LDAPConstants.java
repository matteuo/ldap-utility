package dev.matteuo.ldap.constants;

import javax.naming.directory.SearchControls;

/**
 * LDAPConstants provides constant values for configuring LDAP operations.
 */
public class LDAPConstants {

    /**
     * Default page size for LDAP searches.
     */
    public static final int PAGE_SIZE_DEFAULT = 1000;

    /**
     * Default limit for the number of results in LDAP searches.
     */
    public static final int LIMIT_RESULTS_DEFAULT = 1000;

    // SECURITY_AUTHENTICATION

    /**
     * Constant for no security authentication.
     */
    public static final String SECURITY_AUTHENTICATION_NONE = "none";

    /**
     * Constant for simple security authentication.
     */
    public static final String SECURITY_AUTHENTICATION_SIMPLE = "simple";

    // LDAP Search Scopes

    /**
     * Constant for base search scope.
     * Searches only the base object.
     */
    public static final int SEARCH_SCOPE_BASE = SearchControls.OBJECT_SCOPE;

    /**
     * Constant for one-level search scope.
     * Searches only the immediate children of the base object, excluding the base object itself.
     */
    public static final int SEARCH_SCOPE_ONELEVEL = SearchControls.ONELEVEL_SCOPE;

    /**
     * Constant for subtree search scope.
     * Searches the base object and all its descendants.
     */
    public static final int SEARCH_SCOPE_SUBTREE = SearchControls.SUBTREE_SCOPE;
}