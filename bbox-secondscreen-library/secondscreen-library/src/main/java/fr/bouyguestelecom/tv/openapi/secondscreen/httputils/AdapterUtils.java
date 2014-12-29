package fr.bouyguestelecom.tv.openapi.secondscreen.httputils;

import fr.bouyguestelecom.tv.openapi.secondscreen.bbox.Bbox;
import fr.bouyguestelecom.tv.openapi.secondscreen.security.SSLUtils;
import retrofit.RestAdapter;
import retrofit.client.ApacheClient;

/**
 * Created by vincent on 19/12/2014.
 */
public class AdapterUtils {
    public static <T> T createBboxService(Bbox bbox, Class<T> service) {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint("http://" + bbox.getIp() + ":8080/api.bbox.lan/v0")
                .build();
        return restAdapter.create(service);
    }

    public static IPFSService createPfsService() {
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint("https://dev.bouyguestelecom.fr")
                //.setClient(new ApacheClient(SSLUtils.createSslClient()))
                .build();
        return restAdapter.create(IPFSService.class);
    }
}
