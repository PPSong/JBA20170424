package com.penn.jba.dailyReport;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.load.engine.Resource;
import com.google.gson.JsonArray;
import com.penn.jba.OtherMainPageActivity;
import com.penn.jba.PPApplication;
import com.penn.jba.R;
import com.penn.jba.databinding.ListReportRecordBinding;
import com.penn.jba.util.PPHelper;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import static android.R.attr.type;
import static com.penn.jba.util.PPValueType.STRING;

/**
 * Created by penn on 27/04/2017.
 */

public class ReportListAdapter extends RecyclerView.Adapter<ReportListAdapter.ReportRecord> {
    private JsonArray data;
    private String desWords;
    private Context activityContext;

    public ReportListAdapter() {
        super();
    }

    public ReportListAdapter(Context activityContext, String type, JsonArray data) {
        super();
        if (type == "fans") {
            desWords = PPApplication.getContext().getResources().getString(R.string.be_your_fans);
        } else if (type == "collects") {
            desWords = PPApplication.getContext().getResources().getString(R.string.collect_ta_moment);
        } else if (type == "beCollecteds") {
            desWords = PPApplication.getContext().getResources().getString(R.string.collect_your_moment);
        }

        this.data = data;
        this.activityContext = activityContext;
    }

    @Override
    public ReportRecord onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

        ListReportRecordBinding listReportRecordBinding = ListReportRecordBinding.inflate(layoutInflater, parent, false);
        return new ReportRecord(listReportRecordBinding);

    }

    @Override
    public void onBindViewHolder(ReportRecord holder, int position) {
        Log.v("pplog122", "" + data.get(position).toString());
        holder.bind(data.get(position).toString());
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public class ReportRecord extends RecyclerView.ViewHolder {
        private final ListReportRecordBinding binding;

        public ReportRecord(ListReportRecordBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(final String recordStr) {
            Picasso.with(PPApplication.getContext())
                    .load(PPHelper.get80ImageUrl(PPHelper.ppFromString(recordStr, "head", STRING).getAsString()))
                    .placeholder(R.drawable.pictures_no)
                    .into(binding.avatarIv);

            binding.nicknameTv.setText(PPHelper.ppFromString(recordStr, "nickname", STRING).getAsString());
            try {
                DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                String tmpStr = sdf.format(PPHelper.ppFromString(recordStr, "time").getAsLong()) + desWords;
                binding.desTv.setText(tmpStr);
            } catch (Exception e) {
                Log.v("pplog", "error:" + e);
            }

            binding.goBt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(activityContext, OtherMainPageActivity.class);
                    intent.putExtra("targetId", PPHelper.ppFromString(recordStr, "id", STRING).getAsString());
                    activityContext.startActivity(intent);
                }
            });
        }
    }
}
