package com.flashmas.lib.gui.generator;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.flashmas.lib.gui.AndroidGuiShard;
import com.flashmas.lib.gui.IdResourceManager;

import net.xqhs.flash.core.agent.AgentWave;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;

public class UiViewFactory {
    private static final String TAG = UiViewFactory.class.getSimpleName();
    private static Yaml yamlParser = new Yaml();
    private static Handler uiHandler = new Handler(Looper.getMainLooper());
    private static Handler backendHandler = new Handler();

    public static Configuration parseYaml(InputStream inputStream) {
        return yamlParser.loadAs(inputStream, Configuration.class);
    }

    public static View createView(Configuration config, Context context, AndroidGuiShard guiShard) {
        if (config == null) {
            return null;
        }

        if (PlatformType.valueOfLabel(config.getPlatformType()) != PlatformType.ANDROID) {
            Log.e(TAG, "Platform type is not android!!!");
            return null;
        }

        return createView(config.getNode(), context, guiShard);
    }

    public static View createView(Element element, Context context, AndroidGuiShard guiShard) {
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
                    childView = createView(childElement, context, guiShard);
                    if (childView == null)
                        continue;
                    childView.setLayoutParams(
                            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    ((LinearLayout) currentView).addView(childView);
                }
                break;
            case BUTTON:
                currentView = createButton(element, context, guiShard);
                break;
            case LABEL:
                currentView = createLabel(element, context, guiShard);
                break;
            case FORM:
                currentView = createForm(element, context, guiShard);
                break;
            default:
                currentView = null;
        }

        return currentView;
    }

    private static View createForm(Element element, Context context, AndroidGuiShard guiShard) {
        EditText view = new EditText(context);

        if (element.getId() != null) {
            view.setId(IdResourceManager.addId(element.getId(), element.getPort()));
        } else {
            Log.e(TAG, "Form element doesn't have id");
        }

        if (element.getText() != null) {
            view.setText(element.getText());
        }

        return view;
    }

    private static View createLabel(Element element, Context context, AndroidGuiShard guiShard) {
        TextView view = new TextView(context);

        if (element.getId() != null) {
            view.setId(IdResourceManager.addId(element.getId(), element.getPort()));
        } else {
            Log.e(TAG, "Label element doesn't have id");
        }

        if (element.getText() != null && element.getText().equals("__agent_name__")) {
            view.setText("Waiting agent event..."); // Default text maybe?
            guiShard.registerEventHandler(agentEvent ->
                    uiHandler.post(() -> view.setText(agentEvent.getType().toString()))
            );
        } else {
            view.setText(element.getText());
        }

        if (element.getRole() != null && element.getRole().equals("logging")) {
            guiShard.registerEventHandler(agentEvent -> {
                if (agentEvent instanceof AgentWave) {
                    uiHandler.post(() ->
                        view.append("\nReceived agent wave: " + agentEvent.toString())
                    );
                }
            });
        }

        if (element.getProperties().containsKey("align") &&
                element.getProperties().get("align").equals("center")) {
            view.setGravity(Gravity.CENTER);
        }
        return view;
    }

    private static View createButton(Element element, Context context, AndroidGuiShard guiShard) {
        Button button = new Button(context);
        button.setText(element.getText());
        if (element.getId() != null) {
            button.setId(IdResourceManager.addId(element.getId(), element.getPort()));
        } else {
            Log.e(TAG, "Button element doesn't have id");
        }

        button.setOnClickListener(v -> {
            Toast.makeText(context, "Sending message...", Toast.LENGTH_LONG).show();
            backendHandler.post(() ->
                    guiShard.onActiveInput(element.getId(), element.getRole(), element.getPort())
            );
        });

        return button;
    }

    private static View createLinearLayout(Element element, Context context) {
        LinearLayout linearLayout = new LinearLayout(context);

        if (element.getId() != null) {
            linearLayout.setId(IdResourceManager.addId(element.getId(), element.getPort()));
        } else {
            Log.e(TAG, "BLOCK element doesn't have id");
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

    public static View parseAndCreateView(InputStream inputStream, Context context, AndroidGuiShard guiShard) {
        if (inputStream == null || context == null || guiShard == null) {
            return null;
        }

        Configuration config = parseYaml(inputStream);

        if (config == null) {
            Log.d(TAG, "parseYaml returned null");
            return null;
        }

        return createView(config, context, guiShard);
    }
}
