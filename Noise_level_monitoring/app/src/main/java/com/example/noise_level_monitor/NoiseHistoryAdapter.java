package com.example.noise_level_monitor;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;//field can be  null

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NoiseHistoryAdapter extends ArrayAdapter<DataAddonHistory> {

    private final List<DataAddonHistory> data;
    private final SimpleDateFormat sdf;

    // Constructor: Ensure this matches the usage in the activity
    public NoiseHistoryAdapter(@NonNull Context context, @NonNull List<DataAddonHistory> data) {
        super(context, android.R.layout.simple_list_item_1, data);
        this.data = data;
        this.sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.getDefault());
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {//cv recycle parameter
        if (convertView == null) {
            convertView = View.inflate(getContext(), android.R.layout.simple_list_item_1, null);
        }

        TextView textView = (TextView) convertView;
        DataAddonHistory item = data.get(position);//tells pos in databsse

        // Format and set text content
        String noiseCategoryLabel = item.getNoiseLevel() > 70 ? "VERY LOUD" : "LOUD";
        String entry = String.format(
                "Time: %s | Noise: %.1f dB | Category: %s | Suitability: %s",
                sdf.format(item.getTimestamp()),
                item.getNoiseLevel(),
                noiseCategoryLabel,
                item.getStudySuitability()
        );
        textView.setText(entry);//formatted string 

        // Customize background color
        if (item.getNoiseLevel() > 70) {
            textView.setBackgroundColor(Color.RED);
        } else {
            textView.setBackgroundColor(Color.GRAY);
        }
        textView.setTextColor(Color.WHITE);

        return convertView;
    }
}
