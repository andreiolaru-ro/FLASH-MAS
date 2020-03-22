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
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.flashmas.app.ui.OnFragmentInteractionListener;
import com.flashmas.lib.FlashManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import examples.echoAgent.EchoAgent;

public class MainActivity extends AppCompatActivity implements OnFragmentInteractionListener {

    private FloatingActionButton mainFab;
    private FloatingActionButton addAgentFab;
    private TextView addAgentTextView;
    private FloatingActionButton toggleStateButton;
    private TextView toggleStateTextView;

    private boolean fabOpen = false;

    private Observer<Boolean> flashStateObserver = new Observer<Boolean>() {
        @Override
        public void onChanged(Boolean state) {
            if (state) {
                toggleStateButton.setImageResource(R.drawable.ic_stop_black_24dp);
                toggleStateTextView.setText(R.string.stop_node_button);
            } else {
                toggleStateButton.setImageResource(R.drawable.ic_play_arrow_black_24dp);
                toggleStateTextView.setText(R.string.start_node_button);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO use ViewModel

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        mainFab = findViewById(R.id.main_fab);
        addAgentFab = findViewById(R.id.add_agent_fab);
        addAgentTextView = findViewById(R.id.add_agent_label);
        toggleStateButton = findViewById(R.id.toggle_state_button);
        toggleStateTextView = findViewById(R.id.toggle_state_text_view);
        Toolbar bar = findViewById(R.id.toolbar);

        mainFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateFab();
            }
        });

        addAgentFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EchoAgent agent = new EchoAgent();
                FlashManager.getInstance().addAgent(agent);
                animateFab();
            }
        });

        toggleStateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FlashManager.getInstance().toggleState();
                animateFab();
            }
        });

        FlashManager.getInstance().getRunningLiveData().observe(this, flashStateObserver);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupWithNavController(bottomNavigationView, navController);
        NavigationUI.setupWithNavController(bar, navController);
    }

    @Override
    public void onFragmentInteraction() {

    }

    @Override
    public void onBackPressed() {
        if (fabOpen) {
            animateFab();
        } else {
            super.onBackPressed();
        }
    }

    void animateFab() {
        if (fabOpen) {
            mainFab.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_backwards));

            addAgentFab.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close));
            addAgentFab.setClickable(false);
            addAgentTextView.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close));

            toggleStateButton.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close));
            toggleStateButton.setClickable(false);
            toggleStateTextView.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close));

            fabOpen = false;
        } else {
            mainFab.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_forward));

            addAgentFab.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open));
            addAgentFab.setClickable(true);
            addAgentTextView.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open));

            toggleStateButton.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open));
            toggleStateButton.setClickable(true);
            toggleStateTextView.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open));

            fabOpen = true;
        }
    }
}
