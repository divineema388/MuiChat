package com.mui.lanchat.utils;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;

public class NetworkUtils {

    private static final String TAG = "NetworkUtils";

    /**
     * Get the IP address of the device on the current Wi-Fi network.
     *
     * @param context The application context.
     * @return The IP address as a String, or null if not connected to Wi-Fi or unable to get IP.
     */
    public static String getLocalIpAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null && wifiManager.isWifiEnabled()) {
            int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
            if (ipAddress != 0) {
                return formatIpAddress(ipAddress);
            }
        }
        // Fallback for other network types or if WifiManager fails
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (InetAddress addr : Collections.list(ni.getInetAddresses())) {
                    if (!addr.isLoopbackAddress() && addr.isSiteLocalAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            Log.e(TAG, "Error getting local IP address: " + e.getMessage());
        }
        return null;
    }

    /**
     * Get the broadcast address for the current Wi-Fi network.
     * This is crucial for UDP broadcast discovery.
     *
     * @param context The application context.
     * @return The broadcast address as InetAddress, or null if not connected to Wi-Fi.
     */
    public static InetAddress getBroadcastAddress(Context context) {
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi == null || !wifi.isWifiEnabled()) {
            return null;
        }

        DhcpInfo dhcp = wifi.getDhcpInfo();
        if (dhcp == null) {
            return null;
        }

        int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
        byte[] quads = new byte[4];
        for (int i = 0; i < 4; i++) {
            quads[i] = (byte) ((broadcast >> i * 8) & 0xFF);
        }
        try {
            return InetAddress.getByAddress(quads);
        } catch (UnknownHostException e) {
            Log.e(TAG, "Error getting broadcast address: " + e.getMessage());
            return null;
        }
    }

    private static String formatIpAddress(int ipAddress) {
        return String.format(
                "%d.%d.%d.%d",
                (ipAddress & 0xff),
                (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff),
                (ipAddress >> 24 & 0xff));
    }
}