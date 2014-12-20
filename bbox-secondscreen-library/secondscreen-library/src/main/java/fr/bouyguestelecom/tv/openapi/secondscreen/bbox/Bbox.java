package fr.bouyguestelecom.tv.openapi.secondscreen.bbox;

import android.content.Context;

import fr.bouyguestelecom.tv.openapi.secondscreen.application.ApplicationsManager;
import fr.bouyguestelecom.tv.openapi.secondscreen.authenticate.Auth;
import fr.bouyguestelecom.tv.openapi.secondscreen.authenticate.IAuthCallback;
import fr.bouyguestelecom.tv.openapi.secondscreen.httputils.BboxRestClient;
import fr.bouyguestelecom.tv.openapi.secondscreen.remote.RemoteManager;

/**
 * @author Pierre-Etienne Cherière PCHERIER@bouyguestelecom.fr
 */
public class Bbox {

    private final static String LOG_TAG = Bbox.class.getName();
    public String ip;
    public String macAddress;
    public ApplicationsManager applicationsManager;
    public RemoteManager remoteManager;
    private BboxRestClient bboxRestClient;
    private Context mContex;
    private Auth auth;

    public Bbox(String ip, Context context) {
        this.mContex = context;
        this.ip = ip;
        this.macAddress = WOLPowerManager.getMacFromArpCache(ip);
        bboxRestClient = new BboxRestClient(ip, mContex);
        applicationsManager = new ApplicationsManager(this);
        remoteManager = new RemoteManager(bboxRestClient);
        auth = new Auth(mContex, this);
    }

    public BboxRestClient getBboxRestClient() {
        return bboxRestClient;
    }

    public RemoteManager getRemoteManager() {
        return remoteManager;
    }

    public String getIp() {
        return ip;
    }

    public ApplicationsManager getApplicationsManager() {
        return applicationsManager;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public void authenticate(String appId, String appSecret, IAuthCallback callback) {
        auth.authenticate(appId, appSecret, callback);
        bboxRestClient.setSessionId(auth.getSessionId());
    }

    /**
     * Can return null if authentication not done or failed.
     * @return Current session id
     */
    public String getSessionId() {
        return auth.getSessionId();
    }
}
