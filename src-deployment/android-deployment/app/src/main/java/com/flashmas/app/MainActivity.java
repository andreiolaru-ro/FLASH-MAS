package com.flashmas.app;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;

import com.flashmas.app.ui.OnFragmentInteractionListener;
import com.flashmas.lib.FlashManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity implements OnFragmentInteractionListener {

    private FloatingActionButton fab;
    private Observer<Boolean> flashStateObserver = new Observer<Boolean>() {
        @Override
        public void onChanged(Boolean state) {
            if (state) {
                fab.setImageResource(R.drawable.ic_stop_black_24dp);
            } else {
                fab.setImageResource(R.drawable.ic_play_arrow_black_24dp);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO use ViewModel

        final Button nav = findViewById(R.id.navigate);
        fab = findViewById(R.id.fab);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               FlashManager.getInstance().toggleState();
            }
        });

        FlashManager.getInstance().getRunningLiveData().observe(this, flashStateObserver);

        nav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavController navController = Navigation.findNavController(MainActivity.this, R.id.nav_host_fragment);
                NavDestination destination = navController.getCurrentDestination();

                if (destination == null) return;

                if (destination.getId() == R.id.logsFragment) {
                    navController.popBackStack();
                    navController.navigate(R.id.agentsListFragment);
                } else if (destination.getId() == R.id.agentsListFragment) {
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
