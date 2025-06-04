package com.mui.lanchat.network;

import android.content.Context;
import android.util.Log;

import com.mui.lanchat.utils.NetworkUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UdpDiscoveryServer {

    private static final String TAG = "UdpDiscoveryServer";
    public static final int DISCOVERY_PORT = 8888;
    public static final String DISCOVERY_MESSAGE = "LAN_CHAT_DISCOVER"; // Message clients send
    public static final String DISCOVERY_RESPONSE = "LAN_CHAT_HELLO"; // Message server responds with

    private DatagramSocket socket;
    private boolean running;
    private ExecutorService executorService;
    private OnDiscoveryListener listener;
    private Context context;

    public interface OnDiscoveryListener {
        void onPeerDiscovered(String ipAddress, String senderMessage);
        void onError(String message);
    }

    public UdpDiscoveryServer(Context context, OnDiscoveryListener listener) {
        this.context = context;
        this.listener = listener;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public void start() {
        if (running) {
            Log.w(TAG, "Discovery server already running.");
            return;
        }

        running = true;
        executorService.execute(() -> {
            try {
                socket = new DatagramSocket(DISCOVERY_PORT, NetworkUtils.getBroadcastAddress(context));
                socket.setBroadcast(true); // Ensure socket is set for broadcast

                Log.d(TAG, "UDP Discovery Server started on port " + DISCOVERY_PORT);

                while (running) {
                    byte[] buffer = new byte[1024];
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet); // This blocks until a packet is received

                    String message = new String(packet.getData(), 0, packet.getLength());
                    String senderIp = packet.getAddress().getHostAddress();

                    Log.d(TAG, "Received UDP packet from " + senderIp + ": " + message);

                    // Ignore messages from self
                    String localIp = NetworkUtils.getLocalIpAddress(context);
                    if (localIp != null && localIp.equals(senderIp)) {
                        continue;
                    }

                    if (message.startsWith(DISCOVERY_MESSAGE)) {
                        // Respond to discovery request
                        sendResponse(packet.getAddress(), packet.getPort());
                        if (listener != null) {
                            listener.onPeerDiscovered(senderIp, message);
                        }
                    }
                }
            } catch (SocketException e) {
                if (running) { // Only log if it's an unexpected error, not due to stop()
                    Log.e(TAG, "SocketException in UDP server: " + e.getMessage());
                    if (listener != null) {
                        listener.onError("Socket error: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "IOException in UDP server: " + e.getMessage());
                if (listener != null) {
                    listener.onError("IO error: " + e.getMessage());
                }
            } finally {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
                Log.d(TAG, "UDP Discovery Server stopped.");
            }
        });
    }

    private void sendResponse(InetAddress clientAddress, int clientPort) throws IOException {
        String response = DISCOVERY_RESPONSE + ":" + NetworkUtils.getLocalIpAddress(context);
        byte[] responseData = response.getBytes();
        DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, clientAddress, clientPort);
        if (socket != null && !socket.isClosed()) {
            socket.send(responsePacket);
            Log.d(TAG, "Sent UDP response to " + clientAddress.getHostAddress() + ":" + clientPort);
        }
    }

    public void stop() {
        running = false;
        if (executorService != null) {
            executorService.shutdownNow(); // Interrupt the running thread
        }
        if (socket != null && !socket.isClosed()) {
            socket.close(); // Close the socket to unblock receive()
        }
        Log.d(TAG, "Attempting to stop UDP Discovery Server.");
    }
}