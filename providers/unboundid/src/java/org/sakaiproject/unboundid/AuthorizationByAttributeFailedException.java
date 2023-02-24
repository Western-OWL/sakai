package org.sakaiproject.unboundid;

import lombok.Getter;

/**
 * Exception class for case where user fails LDAP authorization by attribute check
 * @author plukasew
 */
public class AuthorizationByAttributeFailedException extends Exception
{
    @Getter private String attributeName; // name of ldap attribute that was checked
    @Getter private String attributeValue; // value of attribute that failed check

    /**
     * Constructor.
     * @param message the message
     */
    public AuthorizationByAttributeFailedException(String message)
    {
        super(message);
        attributeName = "";
        attributeValue = "";
    }

    /**
     * Sets the name of the attribute that was checked
     * @param attributeName the name of the attribute
     */
    public void setAttributeName(String attributeName)
    {
        if (attributeName != null)
        {
            this.attributeName = attributeName;
        }
        else
        {
            this.attributeName = "";
        }
    }

    /**
     * Sets the value that failed authorization
     * @param attributeValue the value of the attribute
     */
    public void setAttributeValue(String attributeValue)
    {
        if (attributeValue != null)
        {
            this.attributeValue = attributeValue;
        }
        else
        {
            this.attributeValue = "";
        }
    }
}
