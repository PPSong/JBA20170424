package com.penn.jba.otherMainPage;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.penn.jba.FootprintBelong;
import com.penn.jba.PPApplication;
import com.penn.jba.R;
import com.penn.jba.databinding.FootprintProfileBinding;
import com.penn.jba.databinding.OtherMainpageProfileBinding;
import com.penn.jba.message.MessageActivity;
import com.penn.jba.model.realm.CurrentUser;
import com.penn.jba.model.realm.Footprint;
import com.penn.jba.util.PPHelper;
import com.penn.jba.util.PPValueType;
import com.squareup.picasso.Picasso;

import java.util.List;

import io.realm.Realm;

/**
 * Created by penn on 14/04/2017.
 */

public abstract class OtherPageLoadAdapter<T> extends RecyclerView.Adapter {
    private final int VIEW_PROG = -1;
    public static final int TYPE_HEADER = -2;

    public List<T> data;

    public FootprintBelong footprintBelong;

    private View mHeaderView;
    private String userInfoStr;

    private static Context activityContext;

    public OtherPageLoadAdapter(Context context, List<T> data, FootprintBelong footprintBelong) {
        this.data = data;
        this.footprintBelong = footprintBelong;
        this.activityContext = context;
    }

    //HeaderView和FooterView的get和set函数
    public View getHeaderView() {
        return mHeaderView;
    }

    public void setHeaderView(View headerView, String userInfo) {
        mHeaderView = headerView;
        this.userInfoStr = userInfo;
        notifyItemInserted(0);
    }

    public void needLoadMoreCell() {
        try (Realm realm = Realm.getDefaultInstance()) {
            Footprint footprint = new Footprint();
            footprint.setKey("loadMore");
            footprint.setFootprintBelong(footprintBelong);
            footprint.setType(VIEW_PROG);

            realm.beginTransaction();
            realm.copyToRealmOrUpdate(footprint);
            realm.commitTransaction();
        }
    }

    public void cancelLoadMoreCell() {
        try (Realm realm = Realm.getDefaultInstance()) {
            final Footprint ft = realm.where(Footprint.class)
                    .equalTo("key", "loadMore")
                    .equalTo("footprintBelong", footprintBelong.toString())
                    .findFirst();

            realm.beginTransaction();
            ft.deleteFromRealm();
            realm.commitTransaction();
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (mHeaderView == null) {
            return getRealItemViewType(data.get(position));
        }
        if (position == 0) {
            return TYPE_HEADER;
        } else {
            return getRealItemViewType(data.get(position - 1));
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                      int viewType) {

        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        if (mHeaderView != null && viewType == TYPE_HEADER) {
            OtherMainpageProfileBinding binding = OtherMainpageProfileBinding.inflate(layoutInflater, parent, false);
            return new ProfileHeaderViewHolder(binding);
        }
        if (viewType == VIEW_PROG) {
            View v = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.progress_item, parent, false);

            return new ProgressViewHolder(v);
        }
        return onCreateRealViewHolder(parent, viewType);

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ProgressViewHolder) {
            //pptodo try to remove below two lines
            ((ProgressViewHolder) holder).progressBar.setIndeterminate(true);
            ((ProgressViewHolder) holder).bind();
        } else if (holder instanceof ProfileHeaderViewHolder) {
            ((ProfileHeaderViewHolder) holder).bind(userInfoStr);
        } else {
            if (mHeaderView != null) {
                onBindRealViewHolder(holder, position - 1);
            } else {
                onBindRealViewHolder(holder, position);
            }
        }
    }

    @Override
    public int getItemCount() {
        int i = data == null ? 0 : data.size();
        if (mHeaderView == null) {
            return i;
        } else {
            return i + 1;
        }
    }

    public static class ProgressViewHolder extends RecyclerView.ViewHolder {
        public ProgressBar progressBar;
        public TextView tv;

        public ProgressViewHolder(View v) {
            super(v);
            progressBar = (ProgressBar) v.findViewById(R.id.pb);
        }

        public void bind() {
        }
    }

    public static class ProfileHeaderViewHolder extends RecyclerView.ViewHolder {
        private final OtherMainpageProfileBinding binding;

        public ProfileHeaderViewHolder(OtherMainpageProfileBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(String userInfoStr) {

            String tmpBanner = PPHelper.ppFromString(userInfoStr, "data.profile.banner", PPValueType.STRING).getAsString();
            String tmpHeader = PPHelper.ppFromString(userInfoStr, "data.profile.head", PPValueType.STRING).getAsString();
            String nickname = PPHelper.ppFromString(userInfoStr, "data.profile.nickname", PPValueType.STRING).getAsString();
            int age = PPHelper.ppFromString(userInfoStr, "data.profile.age", PPValueType.INT).getAsInt();
            int genger = PPHelper.ppFromString(userInfoStr, "data.profile.gender", PPValueType.INT).getAsInt();
            int follow = PPHelper.ppFromString(userInfoStr, "data.stats.fans", PPValueType.INT).getAsInt();
            int like = PPHelper.ppFromString(userInfoStr, "data.stats.momentBeLiked", PPValueType.INT).getAsInt();

            if (tmpHeader == "") {
                if (genger == 1) {
                    tmpHeader = "pic_head_man.png";
                } else {
                    tmpHeader = "pic_head_woman.png";
                }
            }
            Picasso.with(PPApplication.getContext())
                    .load(PPHelper.get80ImageUrl(tmpHeader))
                    .placeholder(R.drawable.pictures_no).into(binding.headerCiv);

            if (tmpBanner == "") {
                tmpBanner = "default_banner.jpg";
            }
            Picasso.with(PPApplication.getContext())
                    .load(PPHelper.getSlimImageUrl(tmpBanner))
                    .placeholder(R.drawable.pictures_no).into(binding.mybannerIv);

            binding.nicknameTv.setText(nickname);
            binding.ageTv.setText("(" + age + ")岁");
            binding.followNumTv.setText("" + follow);
            binding.likeNumTv.setText("" + like);

        }
    }

    abstract public int getRealItemViewType(T t);

    abstract public RecyclerView.ViewHolder onCreateRealViewHolder(ViewGroup parent, int viewType);

    abstract public void onBindRealViewHolder(RecyclerView.ViewHolder holder, int position);
}
