package com.penn.jba.util;

/**
 * Created by penn on 30/04/2017.
 */

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.penn.jba.MomentDetailActivity;
import com.penn.jba.R;
import com.penn.jba.databinding.CollectMomentFlowViewBinding;
import com.penn.jba.databinding.FragmentNearbyListItemBinding;
import com.penn.jba.model.CollectMoment;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import static android.R.attr.data;
import static android.R.string.no;

public class NearbyMomentAdapter extends BaseAdapter {

    private JsonArray data;
    private Context context;

    public NearbyMomentAdapter(Context context, JsonArray data) {
        this.context = context;

        this.data = data;

    }

    public void resetData(JsonArray data) {
        this.data = data;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        Log.v("pplog149", "getCount");
        return data == null ? 0 : data.size();
    }

    @Override
    public JsonElement getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        FragmentNearbyListItemBinding binding = FragmentNearbyListItemBinding.inflate(layoutInflater, parent, false);

        viewHolder = new ViewHolder(binding);

        viewHolder.bind(data.get(position));

        viewHolder.binding.getRoot().setOnClickListener(onClickListener(position));

        return viewHolder.binding.getRoot();
    }

    private View.OnClickListener onClickListener(final int position) {
        return new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.v("pplog", "position:" + position + "clicked");

            }
        };
    }

    private class ViewHolder {
        private FragmentNearbyListItemBinding binding;

        public ViewHolder(FragmentNearbyListItemBinding binding) {
            this.binding = binding;
        }

        public void bind(JsonElement item) {

            if (item.toString().equals("empty")) {
                //渲染loading
                binding.mainTv.setText("加载中...");
            } else {
                binding.mainTv.setText(PPHelper.ppFromString(item.toString(), "nickname", PPValueType.STRING).getAsString());
            }
        }
    }
}