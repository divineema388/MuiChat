package com.mui.lanchat;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem; // Import MenuItem

import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import com.mui.lanchat.databinding.ActivityMainBinding;
import com.mui.lanchat.ui.home.HomeFragment; // Import HomeFragment
import com.mui.lanchat.ui.settings.SettingsFragment; // Import SettingsFragment

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications, R.id.navigation_settings) // <--- MODIFIED LINE
                .build();
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        // Add a listener to the navigation controller to handle actions when navigating
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            // Check if the current destination is the settings fragment
            if (destination.getId() == R.id.navigation_settings) {
                // When SettingsFragment is loaded, we can now potentially handle its actions.
                // We'll use a direct way to communicate for the "Clear Chat History" button
                // This isn't the cleanest architecture, but works for this example.
                // A better way would be SharedViewModel or EventBus.
            }
        });

        // Set a listener on the BottomNavigationView for when items are reselected
        navView.setOnItemReselectedListener(item -> {
            // This is useful if you want to perform an action when the currently selected tab is tapped again.
            // For example, scroll to top of chat in HomeFragment.
            if (item.getItemId() == R.id.navigation_home) {
                Fragment navHostFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_main);
                if (navHostFragment != null) {
                    Fragment currentFragment = navHostFragment.getChildFragmentManager().getPrimaryNavigationFragment();
                    if (currentFragment instanceof HomeFragment) {
                        ((HomeFragment) currentFragment).scrollToBottom(); // Implement this in HomeFragment if you want
                    }
                }
            }
        });
    }

    // This method will be called by SettingsFragment through MainActivity to clear chat history
    public void onClearChatHistoryRequested() {
        Fragment navHostFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_main);
        if (navHostFragment != null) {
            Fragment currentFragment = navHostFragment.getChildFragmentManager().getPrimaryNavigationFragment();
            if (currentFragment instanceof HomeFragment) {
                ((HomeFragment) currentFragment).clearChatHistory();
            }
        }
    }
}