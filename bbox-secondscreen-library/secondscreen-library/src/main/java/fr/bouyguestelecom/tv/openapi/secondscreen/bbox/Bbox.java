package fr.bouyguestelecom.tv.openapi.secondscreen.bbox;

import android.content.Context;
import android.util.Log;

import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import fr.bouyguestelecom.tv.openapi.secondscreen.application.ApplicationsManager;
import fr.bouyguestelecom.tv.openapi.secondscreen.httputils.BboxRestClient;
import fr.bouyguestelecom.tv.openapi.secondscreen.httputils.CallbackHttpStatus;
import fr.bouyguestelecom.tv.openapi.secondscreen.httputils.PFSRestClient;
import fr.bouyguestelecom.tv.openapi.secondscreen.remote.RemoteManager;
import fr.bouyguestelecom.tv.openapi.secondscreen.security.Token;

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
    private PFSRestClient pfsRestClient;
    private Context mContex;
    private Token securityToken;

    public Bbox(String ip, Context context) {
        this.mContex = context;
        this.ip = ip;
        this.macAddress = WOLPowerManager.getMacFromArpCache(ip);
        bboxRestClient = new BboxRestClient(ip, mContex);
        pfsRestClient = new PFSRestClient(mContex);
        applicationsManager = new ApplicationsManager(bboxRestClient);
        remoteManager = new RemoteManager(bboxRestClient);
    }

    public PFSRestClient getPFSRestClient() {
        return pfsRestClient;
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

    public Token getSecurityToken() { return securityToken; }

    public void generateSessionId(final CallbackHttpStatus callback) {
        if (securityToken == null) {
            Log.e(LOG_TAG, "createIdSession called before security token creation");
            callback.onResult(400);
        } else {
            JSONObject data = new JSONObject();
            try {
                data.put("token", securityToken.getValue());
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage());
                callback.onResult(400);
                return;
            }
            bboxRestClient.post("security/sessionId", data, new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(final int statusCode, final Header[] headers, final JSONObject response) {
                            for (Header header : headers) {
                                if (BboxRestClient.ID_SESSION_HEADER.equals(header.getName())){
                                    bboxRestClient.setSessionId(header.getValue());
                                    callback.onResult(statusCode);
                                    return;
                                }
                            }

                            Log.e(LOG_TAG, "Cannot find " + BboxRestClient.ID_SESSION_HEADER + " header in response");
                            callback.onResult(404);
                        }

                        public void onFailure(int statusCode, Throwable e, JSONObject errorResponse) {
                            Log.e(LOG_TAG, e.getMessage());
                            callback.onResult(statusCode);
                        }
                    }
            );
        }
    }

    // Requests a security token from PFS
    public void generateSecurityToken(String appId, String appSecret, final CallbackHttpStatus callback) {
        JSONObject request = new JSONObject();
        try {
            request.put("appId", appId);
            request.put("appSecret", appSecret);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage());
            callback.onResult(400);
            return;
        }
        pfsRestClient.post("security/token", request, new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(final int statusCode, final Header[] headers, final JSONObject response) {

                        String tokenValue = null;
                        Date tokenValidity = null;
                        for (Header header : headers) {
                            if (PFSRestClient.TOKEN_HEADER.equals(header.getName())){
                                tokenValue = header.getValue();
                            } else if (PFSRestClient.TOKEN_VALIDITY_HEADER.equals(header.getName()))  {
                                tokenValidity = new Date(Long.parseLong(header.getValue()));
                            }
                        }

                        // Update token
                        if (tokenValue != null && tokenValidity != null) {
                            securityToken = new Token(tokenValue, tokenValidity);
                        } else {
                            Log.e(LOG_TAG, "Cannot retrieve token info from http headers");
                        }

                        callback.onResult(statusCode);
                    }

                    public void onFailure(int statusCode, Throwable e, JSONObject errorResponse) {
                        Log.e(LOG_TAG, e.getMessage());
                        callback.onResult(statusCode);
                    }
                }
        );
    }
}
