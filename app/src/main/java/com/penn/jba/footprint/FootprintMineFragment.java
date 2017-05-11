package com.penn.jba.footprint;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.penn.jba.FootprintBelong;
import com.penn.jba.R;
import com.penn.jba.databinding.FragmentFootprintMineBinding;
import com.penn.jba.model.realm.CurrentUser;
import com.penn.jba.model.realm.Footprint;
import com.penn.jba.model.realm.Pic;
import com.penn.jba.util.FootprintStatus;
import com.penn.jba.util.PPHelper;
import com.penn.jba.util.PPJSONObject;
import com.penn.jba.util.PPLoadAdapter;
import com.penn.jba.util.PPRefreshLoadController;
import com.penn.jba.util.PPRetrofit;
import com.penn.jba.util.PPWarn;
import com.penn.jba.util.PicStatus;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

import static com.penn.jba.footprint.FastBlurUtil.doBlur;

public class FootprintMineFragment extends Fragment {
    private final static int pageSize = 15;

    private Context activityContext;

    private ArrayList<Disposable> disposableList = new ArrayList<Disposable>();

    private Realm realm;
    private CurrentUser currentUser;

    private RealmResults<Footprint> footprints;

    private FootprintAdapter footprintAdapter;

    private FragmentFootprintMineBinding binding;

    private InnerPPRefreshLoadController ppRefreshLoadController;

    public FootprintMineFragment() {
        // Required empty public constructor
    }

    public static FootprintMineFragment newInstance() {
        FootprintMineFragment fragment = new FootprintMineFragment();

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityContext = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //common
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_footprint_mine, container, false);
        View view = binding.getRoot();
        binding.setPresenter(this);
        //end common

        setup();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        realm.close();
        for (Disposable d : disposableList) {
            if (!d.isDisposed()) {
                d.dispose();
            }
        }
    }

    //-----helper-----
    public void setup() {
        // rv background blur
        String tmpBanner="";
        try (Realm realm = Realm.getDefaultInstance()) {
            currentUser = realm.where(CurrentUser.class).findFirst();
            tmpBanner=currentUser.getBanner();
        }

        Target target = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                Log.d("weng", "onBitmapLoaded");
                Bitmap image2 = doBlur(bitmap, 60, false);
                Drawable background = new BitmapDrawable(getResources(), image2);
                binding.mainRv.setBackgroundDrawable(background);
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                Log.d("weng", "onBitmapFailed");
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {
                Log.d("weng", "onPrepareLoad");
            }
        };

        Picasso.with(activityContext)
                .load(PPHelper.get80ImageUrl(tmpBanner))
                .into(target);

        binding.mainRv.setTag(target);

        realm = Realm.getDefaultInstance();
        footprints = realm.where(Footprint.class)
                .equalTo("footprintBelong", FootprintBelong.MINE.toString())
                .notEqualTo("status", FootprintStatus.PREPARE.toString())
                .findAllSorted("createTime", Sort.DESCENDING);
        footprints.addChangeListener(changeListener);

        binding.mainRv.setLayoutManager(new LinearLayoutManager(getActivity()));
        footprintAdapter = new FootprintAdapter(activityContext, footprints, FootprintBelong.MINE);

        footprintAdapter.setHeaderView(binding.mainRv);

        binding.mainRv.setAdapter(footprintAdapter);

        binding.mainRv.setHasFixedSize(true);

        ppRefreshLoadController = new InnerPPRefreshLoadController(binding.mainSwipeRefreshLayout, binding.mainRv);
    }

    private final OrderedRealmCollectionChangeListener<RealmResults<Footprint>> changeListener = new OrderedRealmCollectionChangeListener<RealmResults<Footprint>>() {
        @Override
        public void onChange(RealmResults<Footprint> collection, OrderedCollectionChangeSet changeSet) {
            // `null`  means the async query returns the first time.
            if (changeSet == null) {
                footprintAdapter.notifyDataSetChanged();
                return;
            }
            // For deletions, the adapter has to be notified in reverse order.
            OrderedCollectionChangeSet.Range[] deletions = changeSet.getDeletionRanges();
            for (int i = deletions.length - 1; i >= 0; i--) {
                OrderedCollectionChangeSet.Range range = deletions[i];
                footprintAdapter.notifyItemRangeRemoved(range.startIndex + 1, range.length);
            }

            OrderedCollectionChangeSet.Range[] insertions = changeSet.getInsertionRanges();
            for (OrderedCollectionChangeSet.Range range : insertions) {
                footprintAdapter.notifyItemRangeInserted(range.startIndex + 1, range.length);
            }

            OrderedCollectionChangeSet.Range[] modifications = changeSet.getChangeRanges();
            for (OrderedCollectionChangeSet.Range range : modifications) {
                footprintAdapter.notifyItemRangeChanged(range.startIndex + 1, range.length);
            }
        }
    };


    private int processFootprintMine(String s, boolean refresh) {
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.beginTransaction();

            if (refresh) {
//                realm.where(Footprint.class)
//                        .equalTo("isMine", true)
//                        .equalTo("status", FootprintStatus.NET.toString())
//                        .findAll().deleteAllFromRealm();

                RealmResults<Footprint> r = realm.where(Footprint.class)
                        .equalTo("footprintBelong", FootprintBelong.MINE.toString())
                        .equalTo("status", FootprintStatus.NET.toString())
                        .findAll();
                for (Footprint f : r) {
                    f.getPics().deleteAllFromRealm();
                }
                r.deleteAllFromRealm();
            }

            JsonArray ja = PPHelper.ppFromString(s, "data").getAsJsonArray();

            int realNum = 0;
            for (int i = 0; i < ja.size(); i++) {

                //防止loadmore是查询到已有的记录
                long createTime = 0;
                int type = -100;

                //createTime+"_"+type+"_"+createdBy+"_"+isMine
                createTime = PPHelper.ppFromString(s, "data." + i + ".createTime").getAsLong();
                type = PPHelper.ppFromString(s, "data." + i + ".type").getAsInt();
                String createdBy = PPHelper.ppFromString(s, "data." + i + ".createdBy").getAsString();
                String key = "" + createTime + "_" + type + "_" + createdBy + "_" + FootprintBelong.MINE.toString();

                Footprint ft = realm.where(Footprint.class)
                        .equalTo("key", key)
                        .findFirst();

                if (ft == null) {
                    ft = realm.createObject(Footprint.class, key);
                    realNum++;
                }

                String hash = PPHelper.ppFromString(s, "data." + i + ".hash").getAsString();

                ft.setCreateTime(createTime);
                ft.setId(PPHelper.ppFromString(s, "data." + i + ".id").getAsString());
                ft.setStatus(FootprintStatus.NET);
                ft.setType(type);
                ft.setHash(hash);
                ft.setFootprintBelong(FootprintBelong.MINE);
                ft.setBody(PPHelper.ppFromString(s, "data." + i + "").getAsJsonObject().toString());

                //处理图片s
                if (type == 3) {
                    JsonArray pics = PPHelper.ppFromString(s, "data." + i + ".detail.pics").getAsJsonArray();
                    //如果是重复记录, 先删除当前的pics, 要不然会出现重复图片
                    if (ft.getPics().size() > 0) {
                        Log.v("pplog103", ft.getHash());
                    }
                    ft.getPics().deleteAllFromRealm();
                    for (JsonElement item : pics) {
                        Pic pic = new Pic();
                        pic.setKey(item.getAsString());
                        pic.setNetFileName(item.getAsString());
                        pic.setStatus(PicStatus.NET);
                        ft.getPics().add(pic);
                    }
                }
            }

            realm.commitTransaction();

            return realNum;
        }
    }

    private class InnerPPRefreshLoadController extends PPRefreshLoadController {

        public InnerPPRefreshLoadController(SwipeRefreshLayout swipeRefreshLayout, RecyclerView recyclerView) {
            super(swipeRefreshLayout, recyclerView);
        }

        @Override
        public void doRefresh() {
            PPJSONObject jBody = new PPJSONObject();
            jBody
                    .put("beforeThan", "")
                    .put("afterThan", "");

            final Observable<String> apiResult = PPRetrofit.getInstance().api("footprint.myMoment", jBody.getJSONObject());

            disposableList.add(
                    apiResult
                            .subscribeOn(Schedulers.io())
                            .map(new Function<String, String>() {
                                @Override
                                public String apply(String s) throws Exception {
                                    PPWarn ppWarn = PPHelper.ppWarning(s);

                                    if (ppWarn != null) {
                                        return ppWarn.msg;
                                    } else {
                                        processFootprintMine(s, true);
                                        return "OK";
                                    }
                                }
                            })
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    new Consumer<String>() {
                                        public void accept(String s) {
                                            if (s != "OK") {
                                                PPHelper.ppShowError(s);

                                                return;
                                            }
                                            swipeRefreshLayout.setRefreshing(false);
                                            end();
                                            reset();
                                        }
                                    },
                                    new Consumer<Throwable>() {
                                        public void accept(Throwable t1) {
                                            PPHelper.ppShowError(t1.toString());

                                            swipeRefreshLayout.setRefreshing(false);
                                            end();

                                            t1.printStackTrace();
                                        }
                                    }
                            )
            );
        }

        @Override
        public void doLoadMore() {
            PPJSONObject jBody = new PPJSONObject();

            if (footprints.size() == 1) {
                final PPLoadAdapter tmp = ((PPLoadAdapter) (recyclerView.getAdapter()));
                tmp.cancelLoadMoreCell();
                end();
                return;
            }

            jBody
                    //因为最后一条记录为"loadmore"的fake记录
                    .put("before", "" + footprints.get(footprints.size() - 2).getCreateTime())
                    .put("after", "");

            final Observable<String> apiResult = PPRetrofit.getInstance().api("footprint.myMoment", jBody.getJSONObject());

            disposableList.add(
                    apiResult
                            .subscribeOn(Schedulers.io())
                            .map(new Function<String, String>() {
                                @Override
                                public String apply(String s) throws Exception {
                                    Log.v("pplog5", s);

                                    PPWarn ppWarn = PPHelper.ppWarning(s);

                                    if (ppWarn != null) {
                                        return ppWarn.msg;
                                    } else {
                                        if (processFootprintMine(s, false) < pageSize) {
                                            noMore();
                                        }

                                        return "OK";
                                    }
                                }
                            })
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    new Consumer<String>() {
                                        public void accept(String s) {
                                            if (s != "OK") {
                                                PPHelper.ppShowError(s);

                                                return;
                                            }

                                            final PPLoadAdapter tmp = ((PPLoadAdapter) (recyclerView.getAdapter()));
                                            tmp.cancelLoadMoreCell();
                                            end();
                                        }
                                    },
                                    new Consumer<Throwable>() {
                                        public void accept(Throwable t1) {
                                            PPHelper.ppShowError(t1.getMessage());
                                            t1.printStackTrace();

                                            PPLoadAdapter tmp = ((PPLoadAdapter) (recyclerView.getAdapter()));
                                            tmp.cancelLoadMoreCell();
                                            end();
                                        }
                                    }
                            )
            );
        }
    }
}