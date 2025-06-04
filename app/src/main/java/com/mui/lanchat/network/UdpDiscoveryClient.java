package com.mui.lanchat.network;

import android.content.Context;
import android.util.Log;

import com.mui.lanchat.utils.NetworkUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class UdpDiscoveryClient {

    private static final String TAG = "UdpDiscoveryClient";
    private ExecutorService executorService;
    private Future<?> discoveryTask;
    private OnDiscoveryResponseListener listener;
    private Context context;

    public interface OnDiscoveryResponseListener {
        void onPeerFound(String ipAddress, String responseMessage);
        void onDiscoveryFinished();
        void onDiscoveryError(String message);
    }

    public UdpDiscoveryClient(Context context, OnDiscoveryResponseListener listener) {
        this.context = context;
        this.listener = listener;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void startDiscovery() {
        if (discoveryTask != null && !discoveryTask.isDone()) {
            Log.w(TAG, "Discovery already in progress.");
            return;
        }

        discoveryTask = executorService.submit(() -> {
            DatagramSocket socket = null;
            try {
                socket = new DatagramSocket();
                socket.setBroadcast(true);
                socket.setSoTimeout(5000); // Set a timeout for receiving responses

                InetAddress broadcastAddress = NetworkUtils.getBroadcastAddress(context);
                if (broadcastAddress == null) {
                    if (listener != null) {
                        listener.onDiscoveryError("Could not get broadcast address. Check Wi-Fi connection.");
                    }
                    return;
                }

                String message = UdpDiscoveryServer.DISCOVERY_MESSAGE + ":" + NetworkUtils.getLocalIpAddress(context);
                byte[] sendData = message.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, broadcastAddress, UdpDiscoveryServer.DISCOVERY_PORT);

                socket.send(sendPacket);
                Log.d(TAG, "Sent discovery packet: " + message + " to " + broadcastAddress.getHostAddress());

                long startTime = System.currentTimeMillis();
                // Listen for responses for a certain period
                while (System.currentTimeMillis() - startTime < 5000) { // Listen for 5 seconds
                    try {
                        byte[] recvBuf = new byte[1024];
                        DatagramPacket receivePacket = new DatagramPacket(recvBuf, recvBuf.length);
                        socket.receive(receivePacket);

                        String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
                        String senderIp = receivePacket.getAddress().getHostAddress();

                        // Ignore messages from self or malformed responses
                        String localIp = NetworkUtils.getLocalIpAddress(context);
                        if (localIp != null && localIp.equals(senderIp)) {
                            continue;
                        }

                        if (response.startsWith(UdpDiscoveryServer.DISCOVERY_RESPONSE)) {
                            Log.d(TAG, "Received discovery response from " + senderIp + ": " + response);
                            if (listener != null) {
                                listener.onPeerFound(senderIp, response);
                            }
                        }
                    } catch (SocketTimeoutException e) {
                        Log.d(TAG, "Socket timed out, no more responses or discovery finished.");
                        break; // Exit loop if timeout occurs
                    } catch (IOException e) {
                        Log.e(TAG, "IOException in UDP client receive: " + e.getMessage());
                        if (listener != null) {
                            listener.onDiscoveryError("IO error during receive: " + e.getMessage());
                        }
                        break;
                    }
                }
            } catch (SocketException e) {
                Log.e(TAG, "SocketException in UDP client: " + e.getMessage());
                if (listener != null) {
                    listener.onDiscoveryError("Socket error: " + e.getMessage());
                }
            } catch (IOException e) {
                Log.e(TAG, "IOException in UDP client send: " + e.getMessage());
                if (listener != null) {
                    listener.onDiscoveryError("IO error during send: " + e.getMessage());
                }
            } finally {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
                Log.d(TAG, "UDP Discovery Client finished.");
                if (listener != null) {
                    listener.onDiscoveryFinished();
                }
            }
        });
    }

    public void stopDiscovery() {
        if (discoveryTask != null && !discoveryTask.isDone()) {
            discoveryTask.cancel(true); // Interrupt the task
            Log.d(TAG, "UDP Discovery Client stopped.");
        }
    }

    public boolean isDiscovering() {
        return discoveryTask != null && !discoveryTask.isDone() && !discoveryTask.isCancelled();
    }
}