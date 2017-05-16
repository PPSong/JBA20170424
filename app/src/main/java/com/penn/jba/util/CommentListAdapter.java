package com.penn.jba.util;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.penn.jba.OtherMainPageActivity;
import com.penn.jba.PPApplication;
import com.penn.jba.R;
import com.penn.jba.databinding.ListCommentRecordBinding;
import com.penn.jba.databinding.ListReportRecordBinding;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import static com.penn.jba.util.PPValueType.STRING;

/**
 * Created by penn on 27/04/2017.
 */

public class CommentListAdapter extends RecyclerView.Adapter<CommentListAdapter.CommentRecord> {
    private JsonArray data;
    private Context activityContext;

    public CommentListAdapter() {
        super();
    }

    public CommentListAdapter(Context activityContext, JsonArray data) {
        super();

        this.data = data;
        this.activityContext = activityContext;
    }

    @Override
    public CommentRecord onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

        ListCommentRecordBinding listCommentRecordBinding = ListCommentRecordBinding.inflate(layoutInflater, parent, false);
        return new CommentRecord(listCommentRecordBinding);

    }

    @Override
    public void onBindViewHolder(CommentRecord holder, int position) {
        holder.bind(data.get(position).toString());
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void fake(JsonObject fakeRecord) {
        JsonArray tmpData = new JsonArray();
        tmpData.add(fakeRecord);
        tmpData.addAll(data);

        data = tmpData;

        notifyItemInserted(0);
    }

    public void removeFirstItem() {
        data.remove(0);
        notifyItemRemoved(0);
    }

    public void loadMore(JsonArray moreData) {
        data.addAll(moreData);
        notifyItemRangeInserted(data.size(), moreData.size());
    }

    public class CommentRecord extends RecyclerView.ViewHolder {
        private final ListCommentRecordBinding binding;

        public CommentRecord(ListCommentRecordBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(final String recordStr) {
            Log.v("pplog141", recordStr);
            Picasso.with(PPApplication.getContext())
                    .load(PPHelper.get80ImageUrl(PPHelper.ppFromString(recordStr, "_creator.head", STRING).getAsString()))
                    .placeholder(R.drawable.profile)
                    .into(binding.avatarCiv);

            String referNickname = PPHelper.ppFromString(recordStr, "refer.nickname", STRING).getAsString();
            String creatorNickname = PPHelper.ppFromString(recordStr, "_creator.nickname").getAsString();

            String startStr = creatorNickname;

            if (!TextUtils.isEmpty(referNickname)) {
                startStr += activityContext.getResources().getString(R.string.reply_to) + referNickname + ":";
            } else {
                startStr += ":";
            }

            binding.line1Tv.setText(startStr + PPHelper.ppFromString(recordStr, "content", STRING).getAsString());

            try {
                DateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
                String tmpStr = sdf.format(PPHelper.ppFromString(recordStr, "createTime").getAsLong());
                binding.line2Tv.setText(tmpStr);
            } catch (Exception e) {
                Log.v("pplog", "error:" + e);
            }
        }
    }
}
