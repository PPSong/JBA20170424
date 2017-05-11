package com.penn.jba.util;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
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
import com.penn.jba.message.MessageActivity;
import com.penn.jba.model.realm.CurrentUser;
import com.penn.jba.model.realm.Footprint;
import com.squareup.picasso.Picasso;

import java.util.List;

import io.realm.Realm;

/**
 * Created by penn on 14/04/2017.
 */

public abstract class PPLoadAdapter<T> extends RecyclerView.Adapter {
    private final int VIEW_PROG = -1;
    public static final int TYPE_HEADER = -2;

    public List<T> data;

    public FootprintBelong footprintBelong;

    private View mHeaderView;

    private CurrentUser currentUser;

    private static Context activityContext;

    public PPLoadAdapter(Context context, List<T> data, FootprintBelong footprintBelong) {
        this.data = data;
        this.footprintBelong = footprintBelong;
        this.activityContext =context;
    }

    //HeaderView和FooterView的get和set函数
    public View getHeaderView() {
        return mHeaderView;
    }

    public void setHeaderView(View headerView) {
        mHeaderView = headerView;
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
        //RecyclerView.ViewHolder vh;

        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        if (mHeaderView != null && viewType == TYPE_HEADER) {
            FootprintProfileBinding binding = FootprintProfileBinding.inflate(layoutInflater, parent, false);
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
            try (Realm realm = Realm.getDefaultInstance()) {
                realm.beginTransaction();
                currentUser = realm.where(CurrentUser.class).findFirst();
                realm.commitTransaction();
            }
            ((ProfileHeaderViewHolder) holder).bind(currentUser);
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
        private final FootprintProfileBinding binding;

        public ProfileHeaderViewHolder(FootprintProfileBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(CurrentUser cu) {
            binding.setPresenter(cu);
            binding.executePendingBindings();
            Picasso.with(PPApplication.getContext())
                    .load(PPHelper.get80ImageUrl(cu.getHead()))
                    .placeholder(R.drawable.pictures_no).into(binding.headerCiv);

            Picasso.with(PPApplication.getContext())
                    .load(PPHelper.getSlimImageUrl(cu.getBanner()))
                    .placeholder(R.drawable.pictures_no).into(binding.mybannerIv);

            binding.unreadMessageTv.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    Intent intent1 = new Intent(activityContext, MessageActivity.class);
                    activityContext.startActivity(intent1);
                    return false;
                }
            });
        }
    }

    abstract public int getRealItemViewType(T t);

    abstract public RecyclerView.ViewHolder onCreateRealViewHolder(ViewGroup parent, int viewType);

    abstract public void onBindRealViewHolder(RecyclerView.ViewHolder holder, int position);
}
