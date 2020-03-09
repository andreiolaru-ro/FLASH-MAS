package com.flashmas.app.ui.agents;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.flashmas.app.R;

import net.xqhs.flash.core.agent.Agent;

import java.util.LinkedList;
import java.util.List;

public class AgentsListAdapter extends RecyclerView.Adapter<AgentsListAdapter.AgentHolder> {
    private List<Agent> data = new LinkedList<Agent>();
    private Context context;

    public AgentsListAdapter(Context context) {
        this.context = context.getApplicationContext();
    }

    public void addData(List<Agent> toAdd) {
        if (toAdd == null) {
            return;
        }
        data.addAll(toAdd);
        notifyDataSetChanged();
    }

    public void updateData(List<Agent> newData) {
        data.clear();
        if (newData != null) {
            data.addAll(newData);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AgentHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.item_agent, parent,false);
        return new AgentHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull AgentHolder holder, int position) {
        holder.bind(data.get(position).getName());
    }

    @Override
    public int getItemCount() {
        if (data == null) {
            return 0;
        }

        return data.size();
    }

    class AgentHolder extends RecyclerView.ViewHolder {
        private TextView agentNameView;

        AgentHolder(@NonNull View itemView) {
            super(itemView);
            agentNameView = itemView.findViewById(R.id.agent_name);
        }

        void bind(String agentName) {
            agentNameView.setText(agentName);
        }
    }
}