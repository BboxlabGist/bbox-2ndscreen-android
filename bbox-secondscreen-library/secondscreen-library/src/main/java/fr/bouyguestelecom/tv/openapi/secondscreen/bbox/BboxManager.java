package fr.bouyguestelecom.tv.openapi.secondscreen.bbox;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;


/**
 * @author Pierre-Etienne Cherière PCHERIER@bouyguestelecom.fr
 */
public class BboxManager {

    private final static String LOG_TAG = "BboxManager";
    private final static String SERVICE_TYPE = "_bbox._tcp.local.";
    private WifiManager.MulticastLock multicastLock = null;
    private WifiManager wifiManager = null;
    private JmDNS jmDNS;
    private ServiceListener serviceListener;
    private CallbackBboxFound callbackBboxFound;
    private Context context;

    public void startLookingForBbox(Context context, CallbackBboxFound callbackBboxFound) {

        Log.i("BboxManager", "Start looking for Bbox");

        this.context = context;
        this.callbackBboxFound = callbackBboxFound;
        if (multicastLock == null) {
            wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            multicastLock = wifiManager.createMulticastLock("LibBboxOpenAPI");
            multicastLock.setReferenceCounted(true);
            multicastLock.acquire();
        }
        JmDNSThread jmDNSThread = new JmDNSThread(callbackBboxFound);
        jmDNSThread.execute();
    }

    public void stopLookingForBbox() {
        if (multicastLock != null) {
            multicastLock.release();
            multicastLock = null;
            jmDNS.removeServiceListener(SERVICE_TYPE, serviceListener);
            try {
                jmDNS.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage());
            }
        }
    }

    public interface CallbackBboxFound {
        public void onResult(Bbox bbox);
    }

    private class JmDNSThread extends AsyncTask<Void, Void, Void> {

        CallbackBboxFound callbackBboxFound;

        public JmDNSThread(CallbackBboxFound callbackBboxFound) {
            this.callbackBboxFound = callbackBboxFound;
        }

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                jmDNS = JmDNS.create();
                serviceListener = new ServiceListener() {
                    @Override
                    public void serviceAdded(ServiceEvent event) {
                        Log.i(LOG_TAG, "Service added");
                        jmDNS.requestServiceInfo(event.getType(), event.getName(), true);
                    }

                    @Override
                    public void serviceRemoved(ServiceEvent event) {

                    }

                    @Override
                    public void serviceResolved(ServiceEvent event) {
                        Log.i(LOG_TAG, "Bbox found on " + event.getInfo().getInet4Addresses()[0].getHostAddress());
                        String bboxIP = event.getInfo().getInet4Addresses()[0].getHostAddress();
                        callbackBboxFound.onResult(new Bbox(bboxIP, context));
                    }
                };

                jmDNS.addServiceListener(SERVICE_TYPE, serviceListener);

            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage());
            }
            return null;
        }
    }

}