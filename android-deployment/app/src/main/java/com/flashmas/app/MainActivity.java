package com.flashmas.app;

import android.content.res.ColorStateList;
import android.hardware.Sensor;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.flashmas.app.ui.OnFragmentInteractionListener;
import com.flashmas.lib.CompositeAgentBuilder;
import com.flashmas.lib.FlashManager;
import com.flashmas.lib.sensors.SensorsAgentShard;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.xqhs.flash.core.agent.Agent;

import java.util.Arrays;

import static com.flashmas.app.Utils.enableDisableViewGroup;

public class MainActivity extends AppCompatActivity implements OnFragmentInteractionListener {

    private FloatingActionButton mainFab;
    private FloatingActionButton addAgentFab;
    private TextView addAgentTextView;
    private FloatingActionButton toggleStateButton;
    private TextView toggleStateTextView;
    private Toolbar toolbar;
    private BottomNavigationView bottomNavigationView;
    private NavController navController;
    private ViewGroup navFragment;

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

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        mainFab = findViewById(R.id.main_fab);
        addAgentFab = findViewById(R.id.add_agent_fab);
        addAgentTextView = findViewById(R.id.add_agent_label);
        toggleStateButton = findViewById(R.id.toggle_state_button);
        toggleStateTextView = findViewById(R.id.toggle_state_text_view);
        toolbar = findViewById(R.id.toolbar);
        navFragment = findViewById(R.id.nav_host_fragment);
        setSupportActionBar(toolbar);

        mainFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                animateFab();
            }
        });

        addAgentFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Agent agent = getCompositeAgent();
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

        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupWithNavController(bottomNavigationView, navController);
        NavigationUI.setupWithNavController(toolbar, navController);
    }

    private Agent getCompositeAgent() {
        return new CompositeAgentBuilder()
                .addGuiShard()
                .addSensorShard(Arrays.asList(Sensor.TYPE_PROXIMITY, Sensor.TYPE_GYROSCOPE))
                .addGuiLinkShard(Arrays.asList(SensorsAgentShard.DESIGNATION))
                .build();
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
            addAgentTextView.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close));

            toggleStateButton.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close));
            toggleStateTextView.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_close));


            toolbar.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in));
            bottomNavigationView.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in));
            navFragment.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_in));


            addAgentFab.setClickable(false);
            toggleStateButton.setClickable(false);
            enableDisableViewGroup(bottomNavigationView, true);
            enableDisableViewGroup(toolbar, true);
            enableDisableViewGroup(navFragment, true);

            mainFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorAccent)));

            fabOpen = false;
        } else {
            mainFab.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_forward));

            addAgentFab.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open));
            addAgentTextView.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open));

            toggleStateButton.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open));
            toggleStateTextView.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fab_open));

            toolbar.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out));
            bottomNavigationView.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out));
            navFragment.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.fade_out));

            addAgentFab.setClickable(true);
            toggleStateButton.setClickable(true);
            enableDisableViewGroup(bottomNavigationView, false);
            enableDisableViewGroup(toolbar, false);
            enableDisableViewGroup(navFragment, false);

            mainFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));

            fabOpen = true;
        }
    }
}
