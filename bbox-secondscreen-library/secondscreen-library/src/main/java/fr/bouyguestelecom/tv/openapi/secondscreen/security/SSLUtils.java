package fr.bouyguestelecom.tv.openapi.secondscreen.security;

import android.util.Base64;
import android.util.Log;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;

/**
 * Provides tools to accept bouyguestelecom certificate.
 */
public class SSLUtils {
    private final static String TAG = SSLUtils.class.getName();
    private final static byte[] dev_bouyguestelecom_fr_certificate = Base64.decode("LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tDQpNSUlGT1RDQ0JDR2dBd0lCQWdJUUhXOGtROGtjaVpnNXIzeXl4RG9STkRBTkJna3Foa2lHOXcwQkFRVUZBRENCDQp0VEVMTUFrR0ExVUVCaE1DVlZNeEZ6QVZCZ05WQkFvVERsWmxjbWxUYVdkdUxDQkpibU11TVI4d0hRWURWUVFMDQpFeFpXWlhKcFUybG5iaUJVY25WemRDQk9aWFIzYjNKck1Uc3dPUVlEVlFRTEV6SlVaWEp0Y3lCdlppQjFjMlVnDQpZWFFnYUhSMGNITTZMeTkzZDNjdWRtVnlhWE5wWjI0dVkyOXRMM0p3WVNBb1l5a3hNREV2TUMwR0ExVUVBeE1tDQpWbVZ5YVZOcFoyNGdRMnhoYzNNZ015QlRaV04xY21VZ1UyVnlkbVZ5SUVOQklDMGdSek13SGhjTk1UUXdOekF6DQpNREF3TURBd1doY05NVFV3TnpBME1qTTFPVFU1V2pDQmh6RUxNQWtHQTFVRUJoTUNSbEl4RmpBVUJnTlZCQWdUDQpEVWxzWlNCa1pTQkdjbUZ1WTJVeERqQU1CZ05WQkFjVUJWQmhjbWx6TVJrd0Z3WURWUVFLRkJCQ2IzVjVaM1ZsDQpjeUJVWld4bFkyOXRNUlF3RWdZRFZRUUxGQXN6T1RjZ05EZ3dJRGt6TURFZk1CMEdBMVVFQXhRV1pHVjJMbUp2DQpkWGxuZFdWemRHVnNaV052YlM1bWNqQ0NBU0l3RFFZSktvWklodmNOQVFFQkJRQURnZ0VQQURDQ0FRb0NnZ0VCDQpBTXZCWTh2cEN3ZExGTVZnS2xpQ2lqcjF3VFM1eTlCTnM4bXdjTTVxSXVOaktIam13TWpZeTY4VTBUeTFCTGMyDQpKN2NDdDRGRTJ1R1ROMXRyRWp1Q2h0Z0k1SW5tOU5RZVpmNnlMY2ZLc1BseEVLWjE0SXhEeUMyaEord3FybDJIDQpRMUY3UkxjVlpVWjhVQlgvb3VqZWxkcmpaMUh1YWhEZElGRnlGMGNWVjdXSVh6bzNHWnRPUjAvTTJaQ1F2d0YzDQpmbXhmRnJXSGxSbnZ0U0plTCtzVWNENElaanZhVWNZT2xqSVRPa3Y0VkdkVkF1Y3JHd2o1dGhDR0ZabE54WjVWDQpWNzdlMVNvV2dncHVxbmw2VVF2K0NIY3liS2d5V0FXS2VkN3FyZjhxTUNSOVBqV3lwNnNCMmZXODYxUnY4dUhlDQpkUWFmQ09wTzFBaUZYQ2xrVjM2a1l5VUNBd0VBQWFPQ0FXOHdnZ0ZyTUNFR0ExVWRFUVFhTUJpQ0ZtUmxkaTVpDQpiM1Y1WjNWbGMzUmxiR1ZqYjIwdVpuSXdDUVlEVlIwVEJBSXdBREFPQmdOVkhROEJBZjhFQkFNQ0JhQXdIUVlEDQpWUjBsQkJZd0ZBWUlLd1lCQlFVSEF3RUdDQ3NHQVFVRkJ3TUNNR1VHQTFVZElBUmVNRnd3V2dZS1lJWklBWWI0DQpSUUVITmpCTU1DTUdDQ3NHQVFVRkJ3SUJGaGRvZEhSd2N6b3ZMMlF1YzNsdFkySXVZMjl0TDJOd2N6QWxCZ2dyDQpCZ0VGQlFjQ0FqQVpHaGRvZEhSd2N6b3ZMMlF1YzNsdFkySXVZMjl0TDNKd1lUQWZCZ05WSFNNRUdEQVdnQlFODQpSRndXVTBUQmduNGRJS3NsOUFGajJMNTVwVEFyQmdOVkhSOEVKREFpTUNDZ0hxQWNoaHBvZEhSd09pOHZjMlF1DQpjM2x0WTJJdVkyOXRMM05rTG1OeWJEQlhCZ2dyQmdFRkJRY0JBUVJMTUVrd0h3WUlLd1lCQlFVSE1BR0dFMmgwDQpkSEE2THk5elpDNXplVzFqWkM1amIyMHdKZ1lJS3dZQkJRVUhNQUtHR21oMGRIQTZMeTl6WkM1emVXMWpZaTVqDQpiMjB2YzJRdVkzSjBNQTBHQ1NxR1NJYjNEUUVCQlFVQUE0SUJBUUI2OUhoS1gwdGRqcno4VEFMcDNZWVAwd1o5DQphK0Y2V1hmVjZQZnRLTUxybjJwWERPSTd6Zk4xMW9rYTRITGJ5UzE5cGVSQnlaeHprcjYzMGNhNm8waDhNb1lrDQplckZBejhyZng2MnZIdi9xZndQU01xd2cyWG45QlBqNy9Ud2szOHhyUmFpVk9PdzFKeG04U25hTVdtcGUxNHhQDQpSM2toeUpVNFBMSDFHTXpOb3BVWkZLYjFyM1NzWG1aVXpvVkNxOWFYZnhtSjFZSlJvYk1SZk4ydjNLTlAxVC9FDQpVYk1mM2ZWZEFnUDBLQzRURS9ucTM2bjY3cWRrWEgyRVQybUtGWHpSSS85cW9vWWhKdVFXclVyT3RnOTJXbEFpDQpPUlg0alNGZU91YkFQUDBNUVpQbTRhRFJPSUxwY2lwM1d5REdHQmF4TFQxSjBwLzNSRlIrT1dMWGlCL1ANCi0tLS0tRU5EIENFUlRJRklDQVRFLS0tLS0NCg==", Base64.DEFAULT);
    private static SSLSocketFactory trustSslFactory;

    static {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            java.security.cert.Certificate ca = cf.generateCertificate(new ByteArrayInputStream(dev_bouyguestelecom_fr_certificate));

            keyStore.setCertificateEntry("ca", ca);
            trustSslFactory = new SSLSocketFactory(keyStore);
        } catch (Exception ex) {
            ex.printStackTrace();
            Log.e(TAG, "Cannot intialize keystore: " + ex.getMessage());
            trustSslFactory = SSLSocketFactory.getSocketFactory();
        }
    }

    public static SSLSocketFactory getSSLSocketFactory() {
        return trustSslFactory;
    }

    public static HttpClient createSslClient() {
        HttpClient client = new DefaultHttpClient();
        client.getConnectionManager().getSchemeRegistry().register(new Scheme("https", SSLUtils.getSSLSocketFactory(), 443));
        return client;
    }
}
