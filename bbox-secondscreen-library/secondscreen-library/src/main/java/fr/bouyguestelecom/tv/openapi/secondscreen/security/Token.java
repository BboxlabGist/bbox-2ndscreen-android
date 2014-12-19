package fr.bouyguestelecom.tv.openapi.secondscreen.security;

import java.util.Date;

/**
 * Created by vincent on 18/12/2014.
 */
public class Token {
    private Date validity;
    private String value;

    public String getValue() { return value; }
    public Date getValidity() { return validity; }

    public Token(String value, Date validity) {
        this.value = value;
        this.validity = validity;
    }
}
