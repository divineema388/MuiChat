package com.mui.lanchat.ui.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.mui.lanchat.R;
import com.mui.lanchat.model.ChatMessage;
import com.mui.lanchat.utils.NetworkUtils;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder> {

    private List<ChatMessage> messageList;
    private Context context;
    private String localIp;

    public ChatMessageAdapter(List<ChatMessage> messageList, Context context) {
        this.messageList = messageList;
        this.context = context;
        this.localIp = NetworkUtils.getLocalIpAddress(context);
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == 0) { // My message
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
        } else { // Received message
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
        }
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);
        holder.messageTextView.setText(message.getMessage());
        holder.senderTextView.setText(message.getSenderIp());
        holder.timeTextView.setText(formatTimestamp(message.getTimestamp()));
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = messageList.get(position);
        // If sender IP matches local IP, it's a sent message (0), otherwise received (1)
        return (localIp != null && localIp.equals(message.getSenderIp())) ? 0 : 1;
    }

    private String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(timestamp);
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        TextView senderTextView;
        TextView timeTextView;
        CardView messageCard;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageTextView = itemView.findViewById(R.id.textViewMessage);
            senderTextView = itemView.findViewById(R.id.textViewSender);
            timeTextView = itemView.findViewById(R.id.textViewTime);
            messageCard = itemView.findViewById(R.id.messageCard);
        }
    }
}