package com.penn.jba.util;

/**
 * Created by penn on 30/04/2017.
 */

import android.app.Dialog;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.penn.jba.R;
import com.penn.jba.databinding.CollectMomentFlowViewBinding;
import com.penn.jba.model.CollectMoment;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class CollectMomentAdapter extends BaseAdapter {

    private ArrayList<CollectMoment> data;
    private Context context;

    public CollectMomentAdapter(Context context, ArrayList<CollectMoment> data) {
        this.context = context;
        this.data = data;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public CollectMoment getItem(int position) {
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
        CollectMomentFlowViewBinding binding = CollectMomentFlowViewBinding.inflate(layoutInflater, parent, false);

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
        private CollectMomentFlowViewBinding binding;

        public ViewHolder(CollectMomentFlowViewBinding binding) {
            this.binding = binding;
        }

        public void bind(CollectMoment collectMoment) {
            Picasso.with(context)
                    .load(PPHelper.get800ImageUrl(collectMoment.imageStr))
                    .placeholder(R.drawable.header)
                    .into(binding.mainIv);
        }
    }
}