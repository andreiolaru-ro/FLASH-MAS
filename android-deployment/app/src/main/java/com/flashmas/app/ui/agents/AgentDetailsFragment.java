package com.flashmas.app.ui.agents;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.flashmas.app.R;
import com.flashmas.app.ui.generator.UiViewFactory;

import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.composite.CompositeAgent;

import java.io.IOException;
import java.io.InputStream;

import static com.flashmas.lib.FlashUtils.unregisterAllAgentGuiHandlers;

public class AgentDetailsFragment extends Fragment {
    public static final String AGENT_KEY = "agent_key";
    private Agent agent = null;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null && args.get(AGENT_KEY) instanceof Agent) {
            agent = (Agent) args.get(AGENT_KEY);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = null;

        try {
            InputStream inputStream = getActivity().getAssets().open("agent_view2.yaml");
            view = UiViewFactory.parseAndCreateView(inputStream, getContext(), agent);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (view == null) {
            view = inflater.inflate(R.layout.fragment_agent_details, container, false);
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
//        Bundle args = getArguments();
//
//        TextView nameTextView = view.findViewById(R.id.agent_name);
//        SwitchMaterial switchMaterial = view.findViewById(R.id.state_switch);
//
//        if (args != null && args.get(AGENT_KEY) instanceof Agent) {
//            agent = (Agent) args.get(AGENT_KEY);
//            nameTextView.setText(agent.getName());
//            switchMaterial.setChecked(agent.isRunning());
//        } else {
//            switchMaterial.setClickable(false);
//        }
//
//        switchMaterial.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                if (isChecked) {
//                    agent.start();
//                } else {
//                    agent.stop();
//                }
//            }
//        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (agent instanceof CompositeAgent) {
            unregisterAllAgentGuiHandlers((CompositeAgent) agent);
        }
    }
}
