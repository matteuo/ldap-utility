package dev.matteuo.ldap.constants;

import javax.naming.directory.SearchControls;

public class LDAPConstants {
    public static final int PAGE_SIZE_DEFAULT = 1000;
    public static final int LIMIT_RESULTS_DEFAULT = 1000;

    // SECURITY_AUTHENTICATION
    public static final String SECURITY_AUTHENTICATION_NONE = "none";
    public static final String SECURITY_AUTHENTICATION_SIMPLE = "simple";

    // LDAP Search Scopes
    public static final int SEARCH_SCOPE_BASE = SearchControls.OBJECT_SCOPE;
    public static final int SEARCH_SCOPE_ONELEVEL = SearchControls.ONELEVEL_SCOPE;
    public static final int SEARCH_SCOPE_SUBTREE = SearchControls.SUBTREE_SCOPE;
}
