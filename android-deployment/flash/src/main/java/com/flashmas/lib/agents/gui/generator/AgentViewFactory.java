package com.flashmas.lib.agents.gui.generator;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.flashmas.lib.agents.gui.AndroidGuiShard;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;

public class AgentViewFactory {
    private static final String TAG = AgentViewFactory.class.getSimpleName();
    private static Yaml yamlParser = new Yaml();
    private static Handler backendHandler = new Handler();

    public static Configuration parseYaml(InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }

        return yamlParser.loadAs(inputStream, Configuration.class);
    }

    public static View createView(Configuration config, Context context, AndroidGuiShard guiShard) {
        if (config == null) {
            return null;
        }

        if (PlatformType.valueOfLabel(config.getPlatformType()) != null &&
                PlatformType.valueOfLabel(config.getPlatformType()) != PlatformType.ANDROID) {
            Log.e(TAG, "Platform type is not android!!!");
            return null;
        }

        ScrollView scrollView = null;
        View rootView = createView(config.getNode(), context, guiShard);
        if (rootView != null) {
            scrollView = new ScrollView(context);
            scrollView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            scrollView.addView(rootView);
        }

        return scrollView;
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
                currentView = createLinearLayout(element, context, guiShard);
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

        if (element.getPort() != null) {
            if (element.getId() == null) {
                element.setId(guiShard.getIdResourceManager().getNewId(element));
            }
            view.setId(element.getId());
        } else {
            Log.e(TAG, "Form element doesn't have port set");
        }

        if (element.getText() != null) {
            view.setText(element.getText());
        } else {
            view.setHint("Enter " + (element.getRole() != null ? element.getRole() : "value"));
        }

        return view;
    }

    private static View createLabel(Element element, Context context, AndroidGuiShard guiShard) {
        TextView view = new TextView(context);

        if (element.getPort() != null) {
            if (element.getId() == null) {
                element.setId(guiShard.getIdResourceManager().getNewId(element));
            }
            view.setId(element.getId());
        } else {
            Log.e(TAG, "Label element doesn't have port set");
        }

        if (element.getText() != null) {
            view.setText(element.getText());
        }

        if (element.getProperties().containsKey("align") &&
                element.getProperties().get("align").equals("center")) {
            view.setGravity(Gravity.CENTER);
        }
        return view;
    }

    @SuppressLint("SetTextI18n")
    private static View createButton(Element element, Context context, AndroidGuiShard guiShard) {
        Button button = new Button(context);
        if (element.getPort() != null) {
            if (element.getId() == null) {
                element.setId(guiShard.getIdResourceManager().getNewId(element));
            }
            button.setId(element.getId());
        } else {
            Log.e(TAG, "Button element doesn't have port set");
        }

        button.setOnClickListener(v -> {
            Toast.makeText(context, "Sending message...", Toast.LENGTH_LONG).show();
            backendHandler.post(() ->
                    guiShard.onActiveInput(element.getId(), element.getRole(), element.getPort())
            );
        });

        if (element.getText() != null) {
            button.setText(element.getText());
        } else {
            button.setText(element.getRole() + " port " + element.getPort());
        }

        return button;
    }

    private static View createLinearLayout(Element element, Context context, AndroidGuiShard guiShard) {
        LinearLayout linearLayout = new LinearLayout(context);

        if (element.getPort() != null) {
            if (element.getId() == null) {
                element.setId(guiShard.getIdResourceManager().getNewId(element));
            }
            element.setId(linearLayout.getId());
        } else {
            Log.e(TAG, "BLOCK element doesn't have port set");
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
