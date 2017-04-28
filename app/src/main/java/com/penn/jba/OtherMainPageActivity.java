package com.penn.jba;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.penn.jba.databinding.ActivityLoginBinding;
import com.penn.jba.databinding.ActivityOtherMainPageBinding;
import com.penn.jba.footprint.FootprintAdapter;
import com.penn.jba.model.realm.Footprint;
import com.penn.jba.model.realm.Pic;
import com.penn.jba.util.FootprintStatus;
import com.penn.jba.util.PPHelper;
import com.penn.jba.util.PPJSONObject;
import com.penn.jba.util.PPLoadAdapter;
import com.penn.jba.util.PPRefreshLoadController;
import com.penn.jba.util.PPRetrofit;
import com.penn.jba.util.PPValueType;
import com.penn.jba.util.PPWarn;
import com.penn.jba.util.PicStatus;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Function3;
import io.reactivex.schedulers.Schedulers;
import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

import static android.R.attr.targetId;
import static com.penn.jba.R.string.sb_follow_to_me;
import static com.penn.jba.util.PPHelper.ppWarning;

public class OtherMainPageActivity extends AppCompatActivity {
    private final static int pageSize = 15;

    private Context activityContext;

    private ActivityOtherMainPageBinding binding;

    private ArrayList<Disposable> disposableList = new ArrayList<Disposable>();

    //custom
    private Realm realm;

    private RealmResults<Footprint> footprints;

    private FootprintAdapter footprintAdapter;

    private InnerPPRefreshLoadController ppRefreshLoadController;

    private String targetId;

    private String userInfoStr;

    private String friendshipStr;

    private String userViewStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //common
        activityContext = this;
        binding = DataBindingUtil.setContentView(this, R.layout.activity_other_main_page);
        binding.setPresenter(this);
        //end common

        targetId = getIntent().getStringExtra("targetId");

        setup();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (Disposable d : disposableList) {
            if (!d.isDisposed()) {
                d.dispose();
            }
        }
    }

    private void setup() {
        setSupportActionBar(binding.tb);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getUserInfoAndFriendship();
    }
    //-----help-----

    private void getUserInfoAndFriendship() {
        PPJSONObject jBody1 = new PPJSONObject();
        jBody1
                .put("target", targetId);

        final Observable<String> apiResult1 = PPRetrofit.getInstance()
                .api("user.info", jBody1.getJSONObject());

        PPJSONObject jBody2 = new PPJSONObject();
        jBody2
                .put("target", targetId);

        final Observable<String> apiResult2 = PPRetrofit.getInstance()
                .api("friend.relationship", jBody2.getJSONObject());

        PPJSONObject jBody3 = new PPJSONObject();
        jBody3
                .put("target", targetId);
        final Observable<String> apiResult3 = PPRetrofit.getInstance()
                .api("user.view", jBody3.getJSONObject());

        disposableList.add(
                Observable
                        .zip(
                                apiResult1,
                                apiResult2,
                                apiResult3,
                                new Function3<String, String, String, String>() {
                                    @Override
                                    public String apply(String s, String s2, String s3) throws Exception {
                                        PPWarn ppWarn1 = ppWarning(s);
                                        if (ppWarn1 != null) {
                                            throw new Exception(ppWarn1.msg);
                                        }

                                        userInfoStr = s;

                                        PPWarn ppWarn2 = ppWarning(s2);
                                        if (ppWarn2 != null) {
                                            throw new Exception(ppWarn2.msg);
                                        }

                                        friendshipStr = s2;

                                        PPWarn ppWarn3 = ppWarning(s3);
                                        if (ppWarn3 != null) {
                                            throw new Exception(ppWarn3.msg);
                                        }

                                        userViewStr = s3;

                                        return "OK";
                                    }
                                }
                        )
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                new Consumer<String>() {
                                    @Override
                                    public void accept(String s) throws Exception {
                                        loadContent();
                                    }
                                },
                                new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable t) throws Exception {
                                        binding.pb.setVisibility(View.INVISIBLE);
                                        Log.v("pplog131", t.toString());
                                        PPHelper.ppShowError(t.toString());
                                    }
                                })
        );
    }

    private void loadContent() {
        //处理userInfo
        Log.v("pplog131", userInfoStr);
        String avatar = PPHelper.ppFromString(userInfoStr, "data.profile.head", PPValueType.STRING).getAsString();
        String nickname = PPHelper.ppFromString(userInfoStr, "data.profile.nickname", PPValueType.STRING).getAsString();
        int age = PPHelper.ppFromString(userInfoStr, "data.profile.age", PPValueType.INT).getAsInt();
        int fans = PPHelper.ppFromString(userInfoStr, "data.stats.fans", PPValueType.INT).getAsInt();
        int momentBeLiked = PPHelper.ppFromString(userInfoStr, "data.stats.momentBeLiked", PPValueType.INT).getAsInt();
        JsonArray photos = PPHelper.ppFromString(userInfoStr, "data.profile.photos", PPValueType.ARRAY).getAsJsonArray();

        binding.ctbl.setTitle(nickname);
        Picasso.with(activityContext)
                .load(PPHelper.get80ImageUrl(avatar))
                .placeholder(R.drawable.profile)
                .into(binding.avatarCiv);
        binding.line1Tv.setText("" + age + activityContext.getResources().getString(R.string.years_old));
        binding.line2Tv.setText(activityContext.getResources().getString(R.string.follow) + ":" + fans + " " + activityContext.getResources().getString(R.string.like) + ":" + momentBeLiked);

        //处理和我的朋友关系
        int isFans = PPHelper.ppFromString(friendshipStr, "data.isFans", PPValueType.INT).getAsInt();
        int isFollowed = PPHelper.ppFromString(friendshipStr, "data.isFollowed", PPValueType.INT).getAsInt();
        int isFriend = PPHelper.ppFromString(friendshipStr, "data.isFriend", PPValueType.INT).getAsInt();
        int block = PPHelper.ppFromString(friendshipStr, "data.block", PPValueType.INT).getAsInt();

        if (isFriend != 1 && isFollowed != 1 && isFans != 1) {
            //陌生人
            loadStrangerContent();
        } else {
            loadTargetContent();
        }
    }

    private void loadStrangerContent() {
        PPJSONObject jBody = new PPJSONObject();
        jBody
                .put("target", targetId);

        final Observable<String> apiResult = PPRetrofit.getInstance()
                .api("user.view", jBody.getJSONObject());

        disposableList.add(
                apiResult
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doFinally(new Action() {
                            @Override
                            public void run() throws Exception {
                                binding.pb.setVisibility(View.INVISIBLE);
                            }
                        })
                        .subscribe(new Consumer<String>() {
                                       @Override
                                       public void accept(String s) throws Exception {
                                           PPWarn ppWarn = ppWarning(s);
                                           if (ppWarn != null) {
                                               throw new Exception(ppWarn.msg);
                                           }
                                           //处理userView
                                           int meets = PPHelper.ppFromString(userViewStr, "data.meets", PPValueType.INT).getAsInt();
                                           String tmpStr1 = String.format(activityContext.getResources().getString(R.string.we_meet_shoulder_n_times), meets);
                                           binding.strangerLine1Tv.setText(tmpStr1);

                                           int beCollecteds = PPHelper.ppFromString(userViewStr, "data.beCollecteds", PPValueType.INT).getAsInt();
                                           String tmpStr2 = String.format(activityContext.getResources().getString(R.string.ta_meet_your_n_moments), beCollecteds);
                                           binding.strangerLine2Tv.setText(tmpStr2);

                                           int collects = PPHelper.ppFromString(userViewStr, "data.collects", PPValueType.INT).getAsInt();
                                           String tmpStr3 = String.format(activityContext.getResources().getString(R.string.i_meet_ta_n_moments), beCollecteds);
                                           binding.strangerLine3Tv.setText(tmpStr3);

                                           binding.userNsv.setVisibility(View.VISIBLE);
                                       }
                                   },
                                new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable t) throws Exception {
                                        Log.v("pplog131", t.toString());
                                        PPHelper.ppShowError(t.toString());
                                    }
                                })
        );
    }

    private void loadTargetContent() {
        loadTargetContent(0);
    }

    private void loadTargetContent(long before) {
        PPJSONObject jBody = new PPJSONObject();
        jBody
                .put("target", targetId);
        if (before != 0) {
            jBody.put("before", before);
        }

        final Observable<String> apiResult = PPRetrofit.getInstance()
                .api("footprint.withSomeone", jBody.getJSONObject());

        disposableList.add(
                apiResult
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doFinally(new Action() {
                            @Override
                            public void run() throws Exception {
                                binding.pb.setVisibility(View.INVISIBLE);
                            }
                        })
                        .subscribe(new Consumer<String>() {
                                       @Override
                                       public void accept(String s) throws Exception {
                                           PPWarn ppWarn = ppWarning(s);
                                           if (ppWarn != null) {
                                               throw new Exception(ppWarn.msg);
                                           }
                                           setupUserRv();
                                           binding.mainSwipeRefreshLayout.setVisibility(View.VISIBLE);
                                       }
                                   },
                                new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable t) throws Exception {
                                        Log.v("pplog131", t.toString());
                                        PPHelper.ppShowError(t.toString());
                                    }
                                })
        );
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

    private int processFootprintOther(String s, boolean refresh) {
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.beginTransaction();

            if (refresh) {
                RealmResults<Footprint> r = realm.where(Footprint.class).equalTo("footprintBelong", FootprintBelong.OTHER.toString()).findAll();
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
                String key = "" + createTime + "_" + type + "_" + createdBy + "_" + FootprintBelong.OTHER.toString();

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
                ft.setFootprintBelong(FootprintBelong.OTHER);
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

    private void setupUserRv() {
        realm = Realm.getDefaultInstance();
        footprints = realm.where(Footprint.class).equalTo("footprintBelong", FootprintBelong.OTHER.toString()).findAllSorted("createTime", Sort.DESCENDING);
        footprints.addChangeListener(changeListener);

        binding.userRv.setLayoutManager(new LinearLayoutManager(activityContext));
        footprintAdapter = new FootprintAdapter(activityContext, footprints, FootprintBelong.OTHER);
        binding.userRv.setAdapter(footprintAdapter);

        binding.userRv.setHasFixedSize(true);

        ppRefreshLoadController = new InnerPPRefreshLoadController(binding.mainSwipeRefreshLayout, binding.userRv);
    }

    private class InnerPPRefreshLoadController extends PPRefreshLoadController {

        public InnerPPRefreshLoadController(SwipeRefreshLayout swipeRefreshLayout, RecyclerView recyclerView) {
            super(swipeRefreshLayout, recyclerView);
        }

        @Override
        public void doRefresh() {
            PPJSONObject jBody = new PPJSONObject();
            jBody
                    .put("target", targetId);

            final Observable<String> apiResult = PPRetrofit.getInstance().api("footprint.withSomeone", jBody.getJSONObject());
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
                                        processFootprintOther(s, true);
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
            jBody
                    //因为最后一条记录为"loadmore"的fake记录
                    .put("target", targetId)
                    .put("before", "" + footprints.get(footprints.size() - 2).getCreateTime());

            final Observable<String> apiResult = PPRetrofit.getInstance().api("footprint.withSomeone", jBody.getJSONObject());

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
                                        if (processFootprintOther(s, false) < pageSize) {
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
