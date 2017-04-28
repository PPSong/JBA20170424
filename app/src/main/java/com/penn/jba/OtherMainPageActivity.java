package com.penn.jba;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.google.gson.JsonArray;
import com.penn.jba.databinding.ActivityLoginBinding;
import com.penn.jba.databinding.ActivityOtherMainPageBinding;
import com.penn.jba.util.PPHelper;
import com.penn.jba.util.PPJSONObject;
import com.penn.jba.util.PPRetrofit;
import com.penn.jba.util.PPValueType;
import com.penn.jba.util.PPWarn;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function3;
import io.reactivex.schedulers.Schedulers;

import static android.R.attr.targetId;
import static com.penn.jba.R.string.sb_follow_to_me;
import static com.penn.jba.util.PPHelper.ppWarning;

public class OtherMainPageActivity extends AppCompatActivity {
    private Context activityContext;

    private ActivityOtherMainPageBinding binding;

    private ArrayList<Disposable> disposableList = new ArrayList<Disposable>();

    //custom
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

                                           binding.userRv.setVisibility(View.VISIBLE);
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
}
