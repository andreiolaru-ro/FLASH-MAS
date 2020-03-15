package com.flashmas.app;

import android.os.Bundle;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;

import com.flashmas.app.ui.OnFragmentInteractionListener;
import com.flashmas.lib.FlashManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity implements OnFragmentInteractionListener {

    private FloatingActionButton mainFab;
    private FloatingActionButton addAgentFab;
    private TextView addAgentTextView;

    private boolean fabOpen = false;

    private Observer<Boolean> flashStateObserver = new Observer<Boolean>() {
        @Override
        public void onChanged(Boolean state) {
            if (state) {
                mainFab.setImageResource(R.drawable.ic_stop_black_24dp);
            } else {
                mainFab.setImageResource(R.drawable.ic_play_arrow_black_24dp);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO use ViewModel

        final Button nav = findViewById(R.id.navigate);
        mainFab = findViewById(R.id.main_fab);
        addAgentFab = findViewById(R.id.add_agent_fab);
        addAgentTextView = findViewById(R.id.add_agent_label);
        Toolbar bar = findViewById(R.id.toolbar);
        setSupportActionBar(bar);

        mainFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateFab();
                //FlashManager.getInstance().toggleState();
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

    void animateFab() {
        if (fabOpen) {
            mainFab.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_backwards));
            addAgentFab.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close));
            addAgentFab.setClickable(false);
            addAgentTextView.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close));
            fabOpen = false;
        } else {
            mainFab.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_forward));
            addAgentFab.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open));
            addAgentFab.setClickable(true);
            addAgentTextView.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open));
            fabOpen = true;
        }
    }
}
