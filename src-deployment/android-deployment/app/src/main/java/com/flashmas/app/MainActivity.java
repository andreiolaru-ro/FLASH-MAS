package com.flashmas.app;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;

import com.flashmas.app.ui.OnFragmentInteractionListener;
import com.flashmas.lib.NodeForegroundService;

public class MainActivity extends AppCompatActivity implements OnFragmentInteractionListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO use only one button for start and stop
        // TODO use ViewModel

        Button start = findViewById(R.id.start_node);
        Button stop = findViewById(R.id.stop_node);
        final Button nav = findViewById(R.id.navigate);

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(MainActivity.this, NodeForegroundService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent);
                } else {
                    startService(intent);
                }
            }
        });


        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NodeForegroundService.class);
                stopService(intent);
            }
        });

        nav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavController navController = Navigation.findNavController(MainActivity.this, R.id.nav_host_fragment);
                NavDestination destination = navController.getCurrentDestination();

                if (destination == null) return;

                if (destination.getId() == R.id.logsFragment) {
                    navController.popBackStack();
                    navController.navigate(R.id.agentsFragment);
                } else if (destination.getId() == R.id.agentsFragment) {
                    navController.popBackStack();
                    navController.navigate(R.id.logsFragment);
                }
            }
        });
    }

    @Override
    public void onFragmentInteraction() {

    }
}
