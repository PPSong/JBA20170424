package com.penn.jba.footprint;

import android.content.Context;
import android.databinding.DataBindingUtil;
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
import com.penn.jba.R;
import com.penn.jba.databinding.FragmentFootprintMineBinding;
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

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class FootprintMineFragment extends Fragment {
    private final static int pageSize = 15;

    private Context activityContext;

    private Realm realm;

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

    public void setup() {
        realm = Realm.getDefaultInstance();
        footprints = realm.where(Footprint.class)
                .equalTo("isMine", true)
                .notEqualTo("status", FootprintStatus.PREPARE.toString())
                .findAllSorted("createTime", Sort.DESCENDING);
        footprints.addChangeListener(changeListener);

        binding.mainRv.setLayoutManager(new LinearLayoutManager(getActivity()));
        footprintAdapter = new FootprintAdapter(activityContext, footprints, true);
        binding.mainRv.setAdapter(footprintAdapter);

        binding.mainRv.setHasFixedSize(true);

        ppRefreshLoadController = new InnerPPRefreshLoadController(binding.mainSwipeRefreshLayout, binding.mainRv);
    }

    //-----helper-----
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
                footprintAdapter.notifyItemRangeRemoved(range.startIndex, range.length);
            }

            OrderedCollectionChangeSet.Range[] insertions = changeSet.getInsertionRanges();
            for (OrderedCollectionChangeSet.Range range : insertions) {
                footprintAdapter.notifyItemRangeInserted(range.startIndex, range.length);
            }

            OrderedCollectionChangeSet.Range[] modifications = changeSet.getChangeRanges();
            for (OrderedCollectionChangeSet.Range range : modifications) {
                footprintAdapter.notifyItemRangeChanged(range.startIndex, range.length);
            }
        }
    };


    private int processFootprintMine(String s, boolean refresh) {
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.beginTransaction();

            if (refresh) {
                realm.where(Footprint.class)
                        .equalTo("isMine", true)
                        .equalTo("status", FootprintStatus.NET.toString())
                        .findAll().deleteAllFromRealm();
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
                String key = "" + createTime + "_" + type + "_" + createdBy + "_" + true;

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
                ft.setMine(true);
                ft.setBody(PPHelper.ppFromString(s, "data." + i + "").getAsJsonObject().toString());

                //处理图片s
                if (type == 3) {
                    JsonArray pics = PPHelper.ppFromString(s, "data." + i + ".detail.pics").getAsJsonArray();
                    for (JsonElement item : pics) {
                        Pic pic = new Pic();
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
                    );
        }

        @Override
        public void doLoadMore() {
            PPJSONObject jBody = new PPJSONObject();
            jBody
                    //因为最后一条记录为"loadmore"的fake记录
                    .put("beforeThan", "" + footprints.get(footprints.size() - 2).getHash())
                    .put("afterThan", "");

            final Observable<String> apiResult = PPRetrofit.getInstance().api("footprint.myMoment", jBody.getJSONObject());
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
                    );
        }
    }
}