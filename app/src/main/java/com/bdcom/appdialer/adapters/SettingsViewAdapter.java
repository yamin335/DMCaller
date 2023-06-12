package com.bdcom.appdialer.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bdcom.appdialer.R;

import java.util.List;

public class SettingsViewAdapter extends ArrayAdapter<String> {

    private final LayoutInflater mInflater;
    private final Context mContext;
    private final List<String> items;

    String amountStr = "";

    public SettingsViewAdapter(@NonNull Context context, @NonNull List<String> objects) {
        super(context, 0, objects);

        mContext = context;
        mInflater = LayoutInflater.from(context);
        items = objects;
    }

    @Override
    public View getDropDownView(
            int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createItemView(position, convertView, parent);
    }

    @Override
    public @NonNull View getView(
            int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createItemView(position, convertView, parent);
    }

    private View createItemView(int position, View convertView, ViewGroup parent) {

        final View view = mInflater.inflate(R.layout.item_setting, parent, false);
        TextView tvTitle = (TextView) view.findViewById(R.id.textViewName);
        TextView tvAmount = (TextView) view.findViewById(R.id.textViewBalance);
        ImageView ivNext = (ImageView) view.findViewById(R.id.imageViewNext);

        tvTitle.setText(items.get(position));

//        if (position == (items.size() - 3)) {
//            tvAmount.setVisibility(View.VISIBLE);
//            ivNext.setVisibility(View.VISIBLE);
//            tvAmount.setText("BDT: " + amountStr);
//        } else {
//            tvAmount.setVisibility(View.GONE);
//            ivNext.setVisibility(View.GONE);
//        }

        return view;
    }

    public void setAmount(String amountTxt) {
        try {
            Float amount = Float.valueOf(amountTxt);
            amountStr = String.format("%.02f", amount);
            //            DecimalFormat decimalFormat = new DecimalFormat("##.##");
            //            float twoDigitsF = Float.valueOf(decimalFormat.format(amount));
            //            amountStr = String.valueOf(twoDigitsF);
        } catch (Exception e) {
            e.printStackTrace();
        }

        notifyDataSetChanged();
    }
}
