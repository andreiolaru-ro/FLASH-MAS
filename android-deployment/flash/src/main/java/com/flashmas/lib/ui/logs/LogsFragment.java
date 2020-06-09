package com.flashmas.lib.ui.logs;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.flashmas.lib.FlashManager;
import com.flashmas.lib.R;

import java.io.ByteArrayOutputStream;


/**
 * A simple {@link Fragment} subclass for the Logs Fragment.
 * Use the {@link LogsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LogsFragment extends Fragment {
    TextView logTextView;
    Thread updater;

    public LogsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment LogsFragment.
     */
    public static LogsFragment newInstance() {
        return new LogsFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_logs, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        logTextView = view.findViewById(R.id.logs_textview);

        updater = new Thread() {
            @Override
            public void run() {
                ByteArrayOutputStream s = FlashManager.getInstance().getLogOutputStream();
                Handler mainHandler = new Handler(Looper.getMainLooper());
                while (true) {
                    if (logTextView.getText().length() != s.size()) {
                        mainHandler.post(() -> logTextView.setText(s.toString()));
                    }
                    try {
                        sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        updater.start();
        ScrollView scroll = view.findViewById(R.id.scroll_view);
        logTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
            }
        });
    }
}
