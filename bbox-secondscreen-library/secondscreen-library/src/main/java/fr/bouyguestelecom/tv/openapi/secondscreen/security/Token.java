package fr.bouyguestelecom.tv.openapi.secondscreen.security;

import java.util.Date;

/**
 * Created by vincent on 18/12/2014.
 */
public class Token {
    private Date validity;
    private String token;

    public String getValue() { return token; }
    public Date getValidity() { return validity; }

    public Token(String value, Date validity) {
        this.token = value;
        this.validity = validity;
    }
}
