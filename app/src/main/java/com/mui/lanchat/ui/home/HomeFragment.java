package com.mui.lanchat.ui.home;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mui.lanchat.R;
import com.mui.lanchat.databinding.FragmentHomeBinding;
import com.mui.lanchat.model.ChatMessage;
import com.mui.lanchat.network.ChatClient;
import com.mui.lanchat.network.ChatServer;
import com.mui.lanchat.network.UdpDiscoveryClient;
import com.mui.lanchat.network.UdpDiscoveryServer;
import com.mui.lanchat.utils.NetworkUtils;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements UdpDiscoveryServer.OnDiscoveryListener,
        UdpDiscoveryClient.OnDiscoveryResponseListener,
        ChatServer.OnMessageReceivedListener, ChatServer.OnClientConnectionListener,
        ChatClient.OnMessageReceivedListener, ChatClient.OnConnectionStatusListener {

    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private ChatMessageAdapter chatMessageAdapter;
    private List<ChatMessage> messageList;
    private EditText messageInput;
    private Button sendButton;
    private TextView statusText;
    private RecyclerView recyclerView;

    private UdpDiscoveryServer udpDiscoveryServer;
    private UdpDiscoveryClient udpDiscoveryClient;
    private ChatServer chatServer;
    private ChatClient chatClient;

    private String localIpAddress;
    private String connectedPeerIp = null; // IP of the peer we are actively chatting with

    private Handler uiHandler = new Handler(Looper.getMainLooper());


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        messageList = new ArrayList<>();
        chatMessageAdapter = new ChatMessageAdapter(messageList, getContext());

        recyclerView = binding.recyclerViewChat;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(chatMessageAdapter);

        messageInput = binding.editTextMessage;
        sendButton = binding.buttonSend;
        statusText = binding.textStatus;

        localIpAddress = NetworkUtils.getLocalIpAddress(getContext());
        if (localIpAddress != null) {
            statusText.setText("My IP: " + localIpAddress + "\nStatus: Initializing...");
        } else {
            statusText.setText("No Wi-Fi/LAN connection. Please connect to Wi-Fi.");
            sendButton.setEnabled(false);
            return root; // Exit if no IP
        }

        sendButton.setOnClickListener(v -> sendMessage());

        initializeNetworkComponents();

        return root;
    }

    private void initializeNetworkComponents() {
        // Start UDP Server to listen for discovery requests
        udpDiscoveryServer = new UdpDiscoveryServer(getContext(), this);
        udpDiscoveryServer.start();
        Log.d(TAG, "UDP Discovery Server started.");

        // Start UDP Client to discover peers
        udpDiscoveryClient = new UdpDiscoveryClient(getContext(), this);
        udpDiscoveryClient.startDiscovery();
        Log.d(TAG, "UDP Discovery Client started.");

        // Start TCP Chat Server to accept incoming chat connections
        chatServer = new ChatServer(this, this);
        chatServer.startServer();
        Log.d(TAG, "TCP Chat Server started.");

        updateStatus("Searching for peers...");
    }

    private void sendMessage() {
        String messageContent = messageInput.getText().toString().trim();
        if (messageContent.isEmpty()) {
            return;
        }

        ChatMessage chatMessage = new ChatMessage(localIpAddress, messageContent);

        // Add to local chat
        addMessage(chatMessage);
        messageInput.setText(""); // Clear input

        if (chatClient != null && chatClient.isConnected()) {
            // If connected as a client, send via client
            chatClient.sendMessage(chatMessage);
        } else if (chatServer != null) {
            // If acting as a server, broadcast to connected clients
            chatServer.broadcastMessage(chatMessage);
        } else {
            updateStatus("Not connected to any peer to send messages.");
            Log.w(TAG, "Attempted to send message without an active connection.");
        }
    }

    private void addMessage(ChatMessage message) {
        uiHandler.post(() -> {
            messageList.add(message);
            chatMessageAdapter.notifyItemInserted(messageList.size() - 1);
            recyclerView.scrollToPosition(messageList.size() - 1);
        });
    }

    private void updateStatus(String status) {
        uiHandler.post(() -> {
            String currentIp = (localIpAddress != null) ? "My IP: " + localIpAddress + "\n" : "";
            statusText.setText(currentIp + "Status: " + status);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        if (udpDiscoveryServer != null) {
            udpDiscoveryServer.stop();
        }
        if (udpDiscoveryClient != null) {
            udpDiscoveryClient.stopDiscovery();
        }
        if (chatServer != null) {
            chatServer.stopServer();
        }
        if (chatClient != null) {
            chatClient.close();
        }
        Log.d(TAG, "HomeFragment destroyed, networking components stopped.");
    }

    //region UDP Discovery Server Callbacks
    @Override
    public void onPeerDiscovered(String ipAddress, String senderMessage) {
        Log.d(TAG, "Server: Peer Discovered: " + ipAddress + " msg: " + senderMessage);
        uiHandler.post(() -> {
            if (connectedPeerIp == null && !ipAddress.equals(localIpAddress)) {
                updateStatus("Found peer: " + ipAddress + ". Attempting to connect...");
                // Automatically connect to the first discovered peer if not already connected
                connectToPeer(ipAddress);
            }
        });
    }

    @Override
    public void onError(String message) {
        Log.e(TAG, "UDP Server Error: " + message);
        updateStatus("Discovery Error: " + message);
    }
    //endregion

    //region UDP Discovery Client Callbacks
    @Override
    public void onPeerFound(String ipAddress, String responseMessage) {
        Log.d(TAG, "Client: Peer Found: " + ipAddress + " msg: " + responseMessage);
        uiHandler.post(() -> {
            if (connectedPeerIp == null && !ipAddress.equals(localIpAddress)) {
                updateStatus("Found peer: " + ipAddress + ". Attempting to connect...");
                // Automatically connect to the first discovered peer if not already connected
                connectToPeer(ipAddress);
            }
        });
    }

    @Override
    public void onDiscoveryFinished() {
        Log.d(TAG, "UDP Discovery Client finished searching.");
        // If no peer found after discovery, update status
        if (connectedPeerIp == null) {
            updateStatus("No peers found. Waiting for incoming connections...");
        }
    }

    @Override
    public void onDiscoveryError(String message) {
        Log.e(TAG, "UDP Client Error: " + message);
        updateStatus("Discovery Error: " + message);
    }
    //endregion

    //region TCP Chat Server Callbacks (for when THIS device acts as server)
    @Override
    public void onClientConnected(String ipAddress) {
        Log.d(TAG, "Client Connected to THIS Server: " + ipAddress);
        uiHandler.post(() -> {
            if (connectedPeerIp == null) { // If not already managing a peer
                connectedPeerIp = ipAddress;
                updateStatus("Connected to: " + ipAddress + " (acting as Server)");
                sendButton.setEnabled(true);
            }
            addMessage(new ChatMessage("System", ipAddress + " joined the chat."));
        });
    }

    @Override
    public void onClientDisconnected(String ipAddress) {
        Log.d(TAG, "Client Disconnected from THIS Server: " + ipAddress);
        uiHandler.post(() -> {
            if (ipAddress.equals(connectedPeerIp)) {
                connectedPeerIp = null; // Reset if the specific peer disconnected
                updateStatus("Peer " + ipAddress + " disconnected. Searching for peers...");
                sendButton.setEnabled(false);
                // Restart discovery to find new peers
                if (udpDiscoveryClient != null) udpDiscoveryClient.startDiscovery();
            }
            addMessage(new ChatMessage("System", ipAddress + " left the chat."));
        });
    }

    // Message received by THIS device when it's acting as a server
    @Override
    public void onMessageReceived(ChatMessage message) {
        Log.d(TAG, "Server Received: " + message.getMessage() + " from " + message.getSenderIp());
        addMessage(message);
        // If acting as server, broadcast to all connected clients
        if (!message.getSenderIp().equals(localIpAddress)) { // Don't re-broadcast own messages
             chatServer.broadcastMessage(message);
        }
    }
    //endregion

    //region TCP Chat Client Callbacks (for when THIS device acts as client)
    private void connectToPeer(String ipAddress) {
        if (chatClient != null && chatClient.isConnected()) {
            chatClient.close(); // Close existing client connection if any
        }
        chatClient = new ChatClient(ipAddress, this, this);
        chatClient.connect();
    }

    @Override
    public void onConnected(String serverIp) {
        Log.d(TAG, "Connected to Peer " + serverIp + " (acting as Client)");
        uiHandler.post(() -> {
            connectedPeerIp = serverIp;
            updateStatus("Connected to: " + serverIp + " (acting as Client)");
            sendButton.setEnabled(true);
            addMessage(new ChatMessage("System", "Connected to " + serverIp));
        });
    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "Disconnected from Peer (acting as Client)");
        uiHandler.post(() -> {
            connectedPeerIp = null;
            updateStatus("Disconnected from peer. Searching for peers...");
            sendButton.setEnabled(false);
            addMessage(new ChatMessage("System", "Disconnected."));
            // Restart discovery to find new peers
            if (udpDiscoveryClient != null) udpDiscoveryClient.startDiscovery();
        });
    }

    // Message received by THIS device when it's acting as a client
    @Override
    public void onError(String message) {
        Log.e(TAG, "Chat Client Error: " + message);
        uiHandler.post(() -> updateStatus("Chat Error: " + message));
    }
    //endregion
}