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

import static com.flashmas.lib.gui.FlashGuiUtils.getAgentView;

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
        View view = getAgentView(agentName, getContext());

        if (view == null) { // Fallback on a default view is something went wrong
            view = inflater.inflate(R.layout.fragment_agent_details, container, false);
        }

        return view;
    }

    @Override
    public void onDestroyView() {
//        FlashGuiUtils.onDestroyView(agentName);
        super.onDestroyView();
        Log.d(TAG, "On destory view called");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "On destory called");
    }
}
