package com.flashmas.lib.ui.agents;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.flashmas.lib.FlashManager;
import com.flashmas.lib.R;
import com.flashmas.lib.ui.OnFragmentInteractionListener;

import net.xqhs.flash.core.agent.Agent;

import java.util.List;


/**
 * A simple {@link Fragment} subclass for the Agents fragment
 * Use the {@link AgentsListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class AgentsListFragment extends Fragment {
    private AgentsListAdapter adapter;

    private Observer<List<Agent>> agentsObserver = new Observer<List<Agent>>() {
        @Override
        public void onChanged(List<Agent> agents) {
            if (adapter == null) {
                return;
            }

            adapter.updateData(agents);
        }
    };

    private OnFragmentInteractionListener mListener;

    public AgentsListFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment AgentsListFragment.
     */
    public static AgentsListFragment newInstance() {
        return new AgentsListFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_agents_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Set up the RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.agents_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AgentsListAdapter(getActivity());
        recyclerView.setAdapter(adapter);

        FlashManager.getInstance().getAgentsLiveData().observe(getViewLifecycleOwner(), agentsObserver);
    }

    public void onButtonPressed() {
        if (mListener != null) {
            mListener.onFragmentInteraction();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
