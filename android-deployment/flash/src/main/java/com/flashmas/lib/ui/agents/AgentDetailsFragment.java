package com.flashmas.lib.ui.agents;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.flashmas.lib.FlashManager;
import com.flashmas.lib.R;

public class AgentDetailsFragment extends Fragment {
    public static final String AGENT_KEY = "agent_key";
    public static final String TAG = AgentDetailsFragment.class.getSimpleName();
    private String agentName = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null && args.get(AGENT_KEY) instanceof String) {
            agentName = (String) args.get(AGENT_KEY);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = FlashManager.getInstance().getAgentView(agentName);

        if (view == null) { // Fallback on a default view is something went wrong
            view = inflater.inflate(R.layout.fragment_agent_details, container, false);
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (agentName != null) {
            getActivity().setTitle(agentName);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        FlashManager.getInstance().removeAgentView(agentName);
    }
}
