package com.mui.lanchat.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.mui.lanchat.R;
import com.mui.lanchat.databinding.FragmentSettingsBinding;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private EditText nicknameInput;
    private Button saveNicknameButton;
    private Button clearChatHistoryButton;

    // Key for SharedPreferences
    public static final String PREFS_NAME = "LanChatPrefs";
    public static final String KEY_NICKNAME = "nickname";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        SettingsViewModel settingsViewModel =
                new ViewModelProvider(this).get(SettingsViewModel.class);

        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        nicknameInput = binding.editTextNickname;
        saveNicknameButton = binding.buttonSaveNickname;
        clearChatHistoryButton = binding.buttonClearChatHistory;

        // Load existing nickname when the fragment starts
        loadNickname();

        saveNicknameButton.setOnClickListener(v -> saveNickname());
        clearChatHistoryButton.setOnClickListener(v -> clearChatHistory());

        return root;
    }

    private void loadNickname() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedNickname = prefs.getString(KEY_NICKNAME, ""); // Default to empty string
        nicknameInput.setText(savedNickname);
    }

    private void saveNickname() {
        String nickname = nicknameInput.getText().toString().trim();
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_NICKNAME, nickname);
        editor.apply(); // Use apply() for asynchronous save

        Toast.makeText(getContext(), R.string.nickname_saved_toast, Toast.LENGTH_SHORT).show();
    }

    private void clearChatHistory() {
        // This will send an event to HomeFragment to clear its message list.
        // We'll implement the receiving part in HomeFragment later.
        // For now, just show a toast.
        Toast.makeText(getContext(), R.string.chat_history_cleared_toast, Toast.LENGTH_SHORT).show();

        // In a real app, you might use a ViewModel or SharedViewModel to communicate
        // this event to HomeFragment more cleanly. For this simple example,
        // we'll add a direct approach to HomeFragment later.
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}