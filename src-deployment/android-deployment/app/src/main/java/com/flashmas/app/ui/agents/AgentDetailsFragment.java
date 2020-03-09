package com.flashmas.app.ui.agents;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.flashmas.app.R;
import com.google.android.material.switchmaterial.SwitchMaterial;

import net.xqhs.flash.core.agent.Agent;

public class AgentDetailsFragment extends Fragment {
    public static final String AGENT_KEY = "agent_key";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_agent_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();

        TextView nameTextView = view.findViewById(R.id.agent_name);
        SwitchMaterial switchMaterial = view.findViewById(R.id.state_switch);

        if (args != null && args.get(AGENT_KEY) instanceof Agent) {
            Agent selectedAgent = (Agent) args.get(AGENT_KEY);
            nameTextView.setText(selectedAgent.getName());
            switchMaterial.setChecked(selectedAgent.isRunning());
        }
        switchMaterial.setClickable(false); // enable after listener implementation
        switchMaterial.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // TODO disable/enable agent
            }
        });
    }
}
