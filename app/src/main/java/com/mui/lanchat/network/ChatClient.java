package com.mui.lanchat.network;

import android.util.Log;

import com.mui.lanchat.model.ChatMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ChatClient {

    private static final String TAG = "ChatClient";
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String serverIp;
    private ExecutorService executorService;
    private Future<?> listenTask;

    private OnMessageReceivedListener messageListener;
    private OnConnectionStatusListener connectionStatusListener;

    public interface OnMessageReceivedListener {
        void onMessageReceived(ChatMessage message);
    }

    public interface OnConnectionStatusListener {
        void onConnected(String serverIp);
        void onDisconnected();
        void onChatClientError(String message); // <--- MODIFIED LINE
    }

    public ChatClient(String serverIp, OnMessageReceivedListener messageListener, OnConnectionStatusListener connectionStatusListener) {
        this.serverIp = serverIp;
        this.messageListener = messageListener;
        this.connectionStatusListener = connectionStatusListener;
        this.executorService = Executors.newSingleThreadExecutor(); // For listening to incoming messages
    }

    public void connect() {
        new Thread(() -> {
            try {
                socket = new Socket(serverIp, ChatServer.CHAT_PORT);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Log.d(TAG, "Connected to chat server: " + serverIp);
                if (connectionStatusListener != null) {
                    connectionStatusListener.onConnected(serverIp);
                }
                startListening();
            } catch (IOException e) {
                Log.e(TAG, "Error connecting to server " + serverIp + ": " + e.getMessage());
                if (connectionStatusListener != null) {
                    connectionStatusListener.onChatClientError("Connection failed: " + e.getMessage()); // <--- MODIFIED LINE
                }
                close();
            }
        }).start();
    }

    private void startListening() {
        if (listenTask != null && !listenTask.isDone()) {
            listenTask.cancel(true); // Cancel previous listener if any
        }
        listenTask = executorService.submit(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    Log.d(TAG, "Received from server: " + line);
                    try {
                        ChatMessage receivedMessage = ChatMessage.fromJson(line);
                        if (messageListener != null) {
                            messageListener.onMessageReceived(receivedMessage);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing received message: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                if (!socket.isClosed()) { // Only log if not explicitly closed
                    Log.e(TAG, "Disconnected from server " + serverIp + ": " + e.getMessage());
                    if (connectionStatusListener != null) {
                        connectionStatusListener.onDisconnected();
                    }
                }
            } finally {
                close();
            }
        });
    }

    public void sendMessage(ChatMessage message) {
        String jsonMessage = message.toJson();
        new Thread(() -> {
            if (out != null && !socket.isClosed()) {
                out.println(jsonMessage);
                Log.d(TAG, "Sent message: " + jsonMessage);
            } else {
                Log.w(TAG, "Cannot send message, client not connected or output stream is null.");
            }
        }).start();
    }

    public void close() {
        if (listenTask != null) {
            listenTask.cancel(true); // Interrupt the listening thread
        }
        if (executorService != null) {
            executorService.shutdownNow();
        }
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null && !socket.isClosed()) {
                socket.close();
                Log.d(TAG, "Chat client closed.");
                if (connectionStatusListener != null) {
                    connectionStatusListener.onDisconnected(); // Notify about disconnect on explicit close
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing chat client: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}