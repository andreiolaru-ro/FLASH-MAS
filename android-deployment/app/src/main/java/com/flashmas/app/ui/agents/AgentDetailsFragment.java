package com.flashmas.app.ui.agents;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.flashmas.app.R;

import net.xqhs.flash.core.composite.CompositeAgent;

import static com.flashmas.lib.gui.FlashGuiUtils.getAgentView;
import static com.flashmas.lib.gui.FlashGuiUtils.unregisterAllAgentGuiHandlers;

public class AgentDetailsFragment extends Fragment {
    public static final String AGENT_KEY = "agent_key";
    private CompositeAgent agent = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null && args.get(AGENT_KEY) instanceof CompositeAgent) {
            agent = (CompositeAgent) args.get(AGENT_KEY);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = null;

        if (agent != null) {
            view = getAgentView(agent, getContext());
        }

        if (view == null) { // Fallback on a default view is something went wrong
            view = inflater.inflate(R.layout.fragment_agent_details, container, false);
        }

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterAllAgentGuiHandlers(agent);
    }
}
