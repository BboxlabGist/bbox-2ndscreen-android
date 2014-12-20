package fr.bouyguestelecom.tv.openapi.secondscreen.security;

/**
 * Created by vincent on 19/12/2014.
 */
public class Credentials {
    private String appId;
    private String appSecret;

    public Credentials(String appId, String appSecret) {
        this.appId = appId;
        this.appSecret = appSecret;
    }

    public String getAppId() {
        return appId;
    }

    public String getAppSecret() {
        return appSecret;
    }
}
