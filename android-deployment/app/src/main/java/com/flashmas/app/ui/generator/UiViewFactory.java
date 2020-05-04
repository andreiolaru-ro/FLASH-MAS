package com.flashmas.app.ui.generator;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.composite.CompositeAgent;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;

import static com.flashmas.lib.FlashUtils.registerGuiEventHandler;

public class UiViewFactory {
    private static final String TAG = "UiViewFactory";
    private static Yaml yamlParser = new Yaml();

    public static Configuration parseYaml(InputStream inputStream) {
        return yamlParser.loadAs(inputStream, Configuration.class);
    }

    public static View createView(Configuration config, Context context, Agent agent) {
        if (config == null) {
            return null;
        }

        if (PlatformType.valueOfLabel(config.getPlatformType()) != PlatformType.ANDROID) {
            Log.e(TAG, "Platform type is not android!!!");
            return null;
        }

        return createView(config.getNode(), context, agent);
    }

    public static View createView(Element element, Context context, Agent agent) {
        if (element == null || element.getType() == null) {
            return null;
        }

        View currentView, childView;
        ElementType type = ElementType.valueOfLabel(element.getType());

        if (type == null) {
            return null;
        }

        switch (type) {
            case BLOCK:
                currentView = createLinearLayout(element, context);
                for (Element childElement: element.getChildren()) {
                    childView = createView(childElement, context, agent);
                    if (childView == null)
                        continue;
                    ((LinearLayout) currentView).addView(childView);
                }
                break;
            case BUTTON:
                currentView = createButton(element, context, agent);
                break;
            case LABEL:
                currentView = createLabel(element, context, agent);
                break;
            case FORM:
                currentView = createForm(element, context, agent);
                break;
            default:
                currentView = null;
        }

        return currentView;
    }

    private static View createForm(Element element, Context context, Agent agent) {
        EditText view = new EditText(context);

        if (element.getId() != null) {
            view.setId(IdResourceManager.addId(element.getId()));
        }

        if (element.getText() != null) {
            view.setText(element.getText());
        }

        return view;
    }

    private static View createLabel(Element element, Context context, Agent agent) {
        TextView view = new TextView(context);

        if (element.getId() != null) {
            view.setId(IdResourceManager.addId(element.getId()));
        }

        if (element.getText() != null && element.getText().equals("__agent_name__")) {
            view.setText("Waiting agent event..."); // Default text maybe?
            if (agent instanceof CompositeAgent) {
                registerGuiEventHandler((CompositeAgent) agent,
                        agentEvent -> view.setText(agentEvent.getType().toString()));
            }
        } else {
            view.setText(element.getText());
        }

        if (element.getProperties().containsKey("align") &&
                element.getProperties().get("align").equals("center")) {
            view.setGravity(Gravity.CENTER);
        }
        return view;
    }

    private static View createButton(Element element, Context context, Agent agent) {
        Button button = new Button(context);
        button.setText(element.getText());
        if (element.getId() != null) {
            button.setId(IdResourceManager.addId(element.getId()));
        }

        if (element.getProperties().containsKey("action") &&
                element.getProperties().get("action").equals("send")) {
            button.setOnClickListener(v -> {
                Toast.makeText(context, "Sending message...", Toast.LENGTH_LONG).show();
//                if (agent instanceof CompositeAgent) {
//
//                }
            });
        }

        if (element.getProperties().containsKey("action") &&
                element.getProperties().get("action").equals("move")) {
            button.setOnClickListener(v -> {
                Toast.makeText(context, "Moving agent...", Toast.LENGTH_LONG).show();
            });
        }

        return button;
    }

    private static View createLinearLayout(Element element, Context context) {
        LinearLayout linearLayout = new LinearLayout(context);

        if (element.getId() != null) {
            linearLayout.setId(IdResourceManager.addId(element.getId()));
        }

        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        linearLayout.setOrientation(LinearLayout.VERTICAL);
        if (element.getProperties().containsKey("orientation") &&
                element.getProperties().get("orientation").equals("horizontal")) {
            linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        }

        return linearLayout;
    }

    public static View parseAndCreateView(InputStream inputStream, Context context, Agent agent) {
        if (inputStream == null || context == null || agent == null) {
            return null;
        }

        Configuration config = parseYaml(inputStream);

        if (config == null) {
            Log.d(TAG, "parseYaml returned null");
            return null;
        }

        return createView(config, context, agent);
    }
}
