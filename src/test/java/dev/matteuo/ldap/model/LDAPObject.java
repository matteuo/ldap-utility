package dev.matteuo.ldap.model;

/**
 * LDAPObject represents a simple LDAP entry with common attributes.
 */
public class LDAPObject {

    /**
     * Common Name (cn) attribute.
     */
    private String cn;

    /**
     * Surname (sn) attribute.
     */
    private String sn;

    /**
     * Email (mail) attribute.
     */
    private String mail;

    /**
     * Gets the Common Name (cn) attribute.
     *
     * @return the cn attribute.
     */
    public String getCn() {
        return cn;
    }

    /**
     * Sets the Common Name (cn) attribute.
     *
     * @param cn the cn attribute to set.
     */
    public void setCn(String cn) {
        this.cn = cn;
    }

    /**
     * Gets the Surname (sn) attribute.
     *
     * @return the sn attribute.
     */
    public String getSn() {
        return sn;
    }

    /**
     * Sets the Surname (sn) attribute.
     *
     * @param sn the sn attribute to set.
     */
    public void setSn(String sn) {
        this.sn = sn;
    }

    /**
     * Gets the Email (mail) attribute.
     *
     * @return the mail attribute.
     */
    public String getMail() {
        return mail;
    }

    /**
     * Sets the Email (mail) attribute.
     *
     * @param mail the mail attribute to set.
     */
    public void setMail(String mail) {
        this.mail = mail;
    }
}
