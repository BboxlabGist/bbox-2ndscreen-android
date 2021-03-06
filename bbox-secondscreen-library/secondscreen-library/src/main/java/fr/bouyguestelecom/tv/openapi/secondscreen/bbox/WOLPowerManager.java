package fr.bouyguestelecom.tv.openapi.secondscreen.bbox;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * Send a Wake-On-Lan packet to the given host
 * Sources are from: https://code.google.com/p/mythmote/source/browse/branches/Wake_On_LAN/src/tkj/android/homecontrol/mythmote/WOLPowerManager.java
 * @author rob elsner
 */
public class WOLPowerManager {

    public static void sendWOL(final Context context, final String MACAddress, int packetCount) {
        //check for errors
        if (MACAddress == null || MACAddress.length() == 0) return;

        try {
            //build task data parameter
            SendWolTaskData taskData = new SendWolTaskData();
            taskData.count = packetCount;
            taskData.MACAddress = MACAddress;
            taskData.broadcastAddress = getBroadcastAddress(context);

            //run sendwoltask
            new SendWolTask().execute(taskData);
        } catch (Exception e) {
            Log.e("WOL", null != e.getMessage() ? e.getMessage() : "Unknown error in sendWOL(Conext, String, int)");
        }

    }

    /**
     * Returns the IP broadcast address for the current wifi connection
     *
     * @param mContext
     * @return
     * @throws java.io.IOException
     */
    private static InetAddress getBroadcastAddress(final Context mContext) throws IOException {
        WifiManager wifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++) {
            quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
        }
        return InetAddress.getByAddress(quads);
    }

    public static String getMacFromArpCache(String ip) {
        if (ip == null)
            return null;
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            while ((line = br.readLine()) != null) {
                String[] splitted = line.split(" +");
                if (splitted != null && splitted.length >= 4 && ip.equals(splitted[0])) {
                    // Basic sanity check
                    String mac = splitted[3];
                    if (mac.matches("..:..:..:..:..:..")) {
                        return mac;
                    } else {
                        return null;
                    }
                }
            }
        } catch (Exception e) {

            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

}

/**
 * Parameter container for sending WOL packets using
 * the SendWolTask
 *
 * @author pot8oe
 */
class SendWolTaskData {
    public String MACAddress;
    public InetAddress broadcastAddress;
    public int count;

    public SendWolTaskData() {
    }
}

/**
 * Sends the given WOL packet
 *
 * @author pot8oe
 */
class SendWolTask extends AsyncTask<SendWolTaskData, Integer, Boolean> {


    static String LOG_TAG = "WOL";
    private static int MACPORT = 7000;

    /**
     * PJRS WOL's getBytes
     *
     * @param macStr
     * @return
     * @throws IllegalArgumentException
     */
    private static byte[] getMacBytes(String macStr) throws IllegalArgumentException {

        byte[] bytes = new byte[6];
        String[] hex = macStr.split("(\\:|\\-)");
        if (hex.length != 6) {
            //throw new IllegalArgumentException("Invalid MAC address.");
        }
        try {
            for (int i = 0; i < 6; i++) {
                bytes[i] = (byte) Integer.parseInt(hex[i], 16);
            }
        } catch (NumberFormatException e) {
            //throw new IllegalArgumentException("Invalid hex digit in MAC address.");
            Log.e(LOG_TAG, "Invalid hex digit in MAC address.");
        }
        return bytes;
    }

    /**
     * Returns a WOL magic packet
     *
     * @param MACAddress
     * @param broadcastAddress
     * @return
     */
    private static DatagramPacket getWolMagicPacket(final String MACAddress, final InetAddress broadcastAddress) {
        byte[] macBytes = getMacBytes(MACAddress);
        byte[] bytes = new byte[6 + 16 * macBytes.length];

        for (int i = 0; i < 6; i++) bytes[i] = (byte) 0xff;

        for (int i = 6; i < bytes.length; i += macBytes.length) {
            System.arraycopy(macBytes, 0, bytes, i, macBytes.length);
        }

        return new DatagramPacket(bytes, bytes.length, broadcastAddress, MACPORT);
    }

    /**
     * Creates a datagram socket, creates wol magic packet and sends the packet
     * the requested number of times.
     */
    @Override
    protected Boolean doInBackground(SendWolTaskData... params) {

        //check for param errors
        if (null == params || params.length <= 0)
            return false;
        if (null == params[0])
            return false;

        try {
            //create magic packet
            DatagramPacket packet = getWolMagicPacket(params[0].MACAddress, params[0].broadcastAddress);

            //create socket
            DatagramSocket socket = new DatagramSocket(MACPORT);
            socket.setBroadcast(true);

            //send packet requested number of times
            for (int i = 0; i < params[0].count; i++) {
                socket.send(packet);
                Log.e(LOG_TAG, "WOL Magic Packet sent");
            }

            //close socket
            socket.close();
        } catch (IOException e) {
            Log.e(LOG_TAG, e.getMessage());
            return false;
        }

        return true;
    }


}
