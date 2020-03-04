package com.flashmas.app;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.xqhs.flash.core.agent.Agent;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    AgentsListAdapter adapter = new AgentsListAdapter(this);

    Observer<List<Agent>> agentsObserver = new Observer<List<Agent>>() {
        @Override
        public void onChanged(List<Agent> agents) {
            if (adapter == null) {
                return;
            }

            adapter.updateData(agents);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TODO use only one button for start and stop
        // TODO use ViewModel

        Button start = findViewById(R.id.start_node);
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


        Button stop = findViewById(R.id.stop_node);
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NodeForegroundService.class);
                stopService(intent);
            }
        });

        // Set up the RecyclerView
        RecyclerView recyclerView = findViewById(R.id.agents_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        NodeForegroundService.getAgentsLiveData().observe(this, agentsObserver);
    }
}
