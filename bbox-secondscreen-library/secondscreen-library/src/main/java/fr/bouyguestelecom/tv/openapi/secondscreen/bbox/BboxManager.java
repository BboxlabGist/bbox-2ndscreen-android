package fr.bouyguestelecom.tv.openapi.secondscreen.bbox;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

/**
 * @author Pierre-Etienne Cherière PCHERIER@bouyguestelecom.fr
 */
public class BboxManager {

    private final static String LOG_TAG = "BboxManager";
    private final static String SERVICE_NAME = "Bboxapi";
    private final static String SERVICE_TYPE = "_http._tcp.";
    private final static String SERVICE_TYPE_LOCAL = "_http._tcp.local.";
    private WifiManager.MulticastLock multicastLock = null;
    private WifiManager wifiManager = null;
    private JmDNS jmDNS;
    private ServiceListener serviceListener;
    private Context context;
    private CallbackBboxFound callbackBboxFound;

    private NsdManager.ResolveListener mResolveListener;
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private NsdManager mNsdManager;
    JmDNSThread jmDNSThread;

    public static InetAddress getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress;
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public void startLookingForBbox(final Context context, final CallbackBboxFound callbackBboxFound) {

        Log.i("BboxManager", "Start looking for Bbox");

        this.callbackBboxFound = callbackBboxFound;

        this.context = context;
        if (multicastLock == null) {
            wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            multicastLock = wifiManager.createMulticastLock("LibBboxOpenAPI");
            multicastLock.setReferenceCounted(true);
            multicastLock.acquire();
        }

        jmDNSThread = new JmDNSThread(callbackBboxFound);
        jmDNSThread.execute();

    }

    public void stopLookingForBbox() {

        StopThread stopThread = new StopThread();
        stopThread.execute();

    }

    private class StopThread extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            if (jmDNSThread != null) {
                jmDNS.removeServiceListener(SERVICE_TYPE_LOCAL, serviceListener);
                try {
                    jmDNS.close();
                } catch (IOException e) {
                    Log.e(LOG_TAG, e.getMessage());
                }
                if (multicastLock != null) {
                    multicastLock.release();
                    multicastLock = null;
                }
                jmDNSThread.cancel(true);
            }
            return null;
        }
    };

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

                InetAddress ip = getLocalIpAddress();
                if (ip != null) {
                    Log.d(LOG_TAG, "YES " + ip.getHostAddress() + ip.getHostName());
                    jmDNS = JmDNS.create(ip, ip.getHostName());
                } else {
                    jmDNS = JmDNS.create();
                }
                serviceListener = new ServiceListener() {

                    @Override
                    public void serviceAdded(ServiceEvent event) {
                        Log.d(LOG_TAG, "Service found: " + event.getName());
                    }

                    @Override
                    public void serviceRemoved(ServiceEvent event) {

                    }

                    @Override
                    public void serviceResolved(ServiceEvent event) {
                        if (event.getName().startsWith(SERVICE_NAME)) {
                            String bboxIP = event.getInfo().getInet4Addresses()[0].getHostAddress();
                            Log.i(LOG_TAG, "Bbox found on IP: " + bboxIP);
                            callbackBboxFound.onResult(new Bbox(bboxIP, context));
                        }
                    }
                };

                jmDNS.addServiceListener(SERVICE_TYPE_LOCAL, serviceListener);

            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage());
            }
            return null;
        }
    }
}