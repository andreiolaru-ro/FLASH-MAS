package com.flashmas.app.ui.generator;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;

public class UiViewFactory {
    private static final String TAG = "UiViewFactory";
    private static Yaml yamlParser = new Yaml();

    public static Configuration parseYaml(InputStream inputStream) {
        return yamlParser.loadAs(inputStream, Configuration.class);
    }

    public static View createView(Configuration config, Context context) {
        if (config == null) {
            return null;
        }

        if (PlatformType.valueOfLabel(config.getPlatformType()) != PlatformType.ANDROID) {
            Log.e(TAG, "Platform type is not android!!!");
            return null;
        }

        return createView(config.getNode(), context);
    }

    public static View createView(Element element, Context context) {
        if (element == null) {
            return null;
        }

        View currentView, childView;
        switch (ElementType.valueOfLabel(element.getType())) {
            case BLOCK:
                currentView = createLinearLayout(element, context);
                for (Element childElement: element.getChildren()) {
                    childView = createView(childElement, context);
                    if (childView == null)
                        continue;
                    ((LinearLayout) currentView).addView(childView);
                }
                break;
            case BUTTON:
                currentView = createButton(element, context);
                break;
            case LABEL:
                currentView = createLabel(element, context);
                break;
            default:
                currentView = null;
        }

        return currentView;
    }

    private static View createLabel(Element element, Context context) {
        TextView textView = new TextView(context);
        textView.setText(element.getText());
        return textView;
    }

    private static View createButton(Element element, Context context) {
        Button button = new Button(context);
        button.setText(element.getText());
        return button;
    }

    private static View createLinearLayout(Element element, Context context) {
        LinearLayout linearLayout = new LinearLayout(context);

        linearLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

        linearLayout.setOrientation(LinearLayout.VERTICAL);
        if (element.getProperties().containsKey("orientation") &&
                    element.getProperties().get("orientation").equals("horizontal")) {
            linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        }

        return linearLayout;
    }

    public static View parseAndCreateView(InputStream inputStream, Context context) {
        if (inputStream == null || context == null) {
            return null;
        }

        Configuration config = parseYaml(inputStream);

        if (config == null) {
            Log.d(TAG, "parseYaml returned null");
            return null;
        }

        return createView(config, context);
    }
}
