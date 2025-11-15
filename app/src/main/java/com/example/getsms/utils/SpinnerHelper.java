package com.example.getsms.utils;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.example.getsms.RuleEditorActivity.SpinnerItem;

public class SpinnerHelper {

    public static void setup(Context context, Spinner spinner, SpinnerItem[] items) {
        ArrayAdapter<SpinnerItem> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item,
                items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    public static void setValue(Spinner spinner, String value) {
        if (value == null) return;

        ArrayAdapter<SpinnerItem> adapter = (ArrayAdapter<SpinnerItem>) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            SpinnerItem item = adapter.getItem(i);
            if (item != null && item.getValue().equals(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    public static String getValue(Spinner spinner) {
        SpinnerItem selected = (SpinnerItem) spinner.getSelectedItem();
        return selected != null ? selected.getValue() : null;
    }

    public static SpinnerItem getSelectedItem(Spinner spinner) {
        return (SpinnerItem) spinner.getSelectedItem();
    }
}