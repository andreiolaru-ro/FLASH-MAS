package com.flashmas.lib;

import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

public class Utils {
    public static void enableDisableViewGroup(ViewGroup viewGroup, boolean enabled) {
        int childCount = viewGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View view = viewGroup.getChildAt(i);
            view.setEnabled(enabled);
            view.setFocusable(enabled);
            if (view instanceof EditText) {
                ((EditText)view).setTextIsSelectable(enabled);
                view.setFocusableInTouchMode(enabled);
            }
            if (view instanceof ViewGroup) {
                enableDisableViewGroup((ViewGroup) view, enabled);
            }
        }
    }
}
