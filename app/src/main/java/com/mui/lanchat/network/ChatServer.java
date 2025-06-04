package com.mui.lanchat.network;

import android.util.Log;

import com.mui.lanchat.model.ChatMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {

    private static final String TAG = "ChatServer";
    public static final int CHAT_PORT = 8080; // Port for TCP chat communication

    private ServerSocket serverSocket;
    private boolean running = false;
    private ExecutorService clientThreadPool;
    private List<ClientHandler> connectedClients;
    private OnMessageReceivedListener messageListener;
    private OnClientConnectionListener connectionListener;

    public interface OnMessageReceivedListener {
        void onMessageReceived(ChatMessage message);
    }

    public interface OnClientConnectionListener {
        void onClientConnected(String ipAddress);
        void onClientDisconnected(String ipAddress);
        void onChatServerError(String message); // <--- MODIFIED LINE
    }

    public ChatServer(OnMessageReceivedListener messageListener, OnClientConnectionListener connectionListener) {
        this.messageListener = messageListener;
        this.connectionListener = connectionListener;
        this.connectedClients = new ArrayList<>();
        this.clientThreadPool = Executors.newCachedThreadPool(); // Flexible thread pool for clients
    }

    public void startServer() {
        if (running) {
            Log.w(TAG, "Chat Server already running.");
            return;
        }

        running = true;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(CHAT_PORT);
                Log.d(TAG, "Chat Server started on port " + CHAT_PORT);

                while (running) {
                    Socket clientSocket = serverSocket.accept(); // Blocks until a client connects
                    Log.d(TAG, "New client connected: " + clientSocket.getInetAddress().getHostAddress());

                    ClientHandler clientHandler = new ClientHandler(clientSocket);
                    synchronized (connectedClients) {
                        connectedClients.add(clientHandler);
                    }
                    clientThreadPool.execute(clientHandler);

                    if (connectionListener != null) {
                        connectionListener.onClientConnected(clientSocket.getInetAddress().getHostAddress());
                    }
                }
            } catch (IOException e) {
                if (running) { // Only log if server was supposed to be running
                    Log.e(TAG, "IOException in Chat Server: " + e.getMessage());
                    if (connectionListener != null) {
                        connectionListener.onChatServerError("Server error: " + e.getMessage()); // <--- MODIFIED LINE
                    }
                }
            } finally {
                stopServer(); // Ensure server is stopped on error or explicit stop
                Log.d(TAG, "Chat Server stopped.");
            }
        }).start();
    }

    public void stopServer() {
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing server socket: " + e.getMessage());
            }
        }
        if (clientThreadPool != null) {
            clientThreadPool.shutdownNow(); // Attempt to stop all client threads
        }
        synchronized (connectedClients) {
            for (ClientHandler handler : connectedClients) {
                handler.close();
            }
            connectedClients.clear();
        }
    }

    // Method to broadcast a message to all connected clients
    public void broadcastMessage(ChatMessage message) {
        String jsonMessage = message.toJson();
        synchronized (connectedClients) {
            for (ClientHandler client : connectedClients) {
                client.sendMessage(jsonMessage);
            }
        }
    }

    private class ClientHandler implements Runnable {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientIp;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
            this.clientIp = socket.getInetAddress().getHostAddress();
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            } catch (IOException e) {
                Log.e(TAG, "Error initializing ClientHandler streams: " + e.getMessage());
                close();
            }
        }

        @Override
        public void run() {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    Log.d(TAG, "Received from client " + clientIp + ": " + line);
                    try {
                        ChatMessage receivedMessage = ChatMessage.fromJson(line);
                        if (messageListener != null) {
                            messageListener.onMessageReceived(receivedMessage);
                        }
                        // Optionally, broadcast received message to other clients
                        // broadcastMessage(receivedMessage); // Uncomment if you want server to re-broadcast
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing received message from " + clientIp + ": " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Client " + clientIp + " disconnected: " + e.getMessage());
            } finally {
                close();
                synchronized (connectedClients) {
                    connectedClients.remove(this);
                }
                if (connectionListener != null) {
                    connectionListener.onClientDisconnected(clientIp);
                }
            }
        }

        public void sendMessage(final String msg) {
            new Thread(() -> {
                if (out != null && !clientSocket.isClosed()) {
                    out.println(msg);
                    Log.d(TAG, "Sent message to " + clientIp + ": " + msg);
                }
            }).start();
        }

        public void close() {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error closing client socket: " + e.getMessage());
            }
        }
    }
}