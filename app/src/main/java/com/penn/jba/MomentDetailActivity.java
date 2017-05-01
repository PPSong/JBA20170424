package com.penn.jba;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.penn.jba.databinding.ActivityLoginBinding;
import com.penn.jba.databinding.ActivityMomentDetailBinding;
import com.penn.jba.util.PPHelper;
import com.penn.jba.util.PPJSONObject;
import com.penn.jba.util.PPRetrofit;
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

import static com.penn.jba.util.PPHelper.ppWarning;

public class MomentDetailActivity extends AppCompatActivity {
    private Context activityContext;

    private ActivityMomentDetailBinding binding;

    private ArrayList<Disposable> disposableList = new ArrayList<Disposable>();

    //custom
    private String momentId;
    private String momentStr;
    private String commentsStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //common
        activityContext = this;
        binding = DataBindingUtil.setContentView(this, R.layout.activity_moment_detail);
        //end common

        momentId = getIntent().getStringExtra("momentId");

        setup();
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
        getMomentDetailAndComment();
    }

    private void getMomentDetailAndComment() {
        PPJSONObject jBody1 = new PPJSONObject();
        jBody1
                .put("id", momentId)
                .put("checkFollow", 1)
                .put("checkLike", 1);

        final Observable<String> apiResult1 = PPRetrofit.getInstance()
                .api("moment.detail", jBody1.getJSONObject());

        PPJSONObject jBody2 = new PPJSONObject();
        jBody2
                .put("id", momentId)
                .put("beforeTime", "");

        final Observable<String> apiResult2 = PPRetrofit.getInstance()
                .api("moment.getReplies", jBody2.getJSONObject());

        disposableList.add(
                Observable
                        .zip(
                                apiResult1,
                                apiResult2,
                                new BiFunction<String, String, String>() {
                                    @Override
                                    public String apply(String s, String s2) throws Exception {
                                        PPWarn ppWarn1 = ppWarning(s);
                                        if (ppWarn1 != null) {
                                            throw new Exception(ppWarn1.msg);
                                        }

                                        momentStr = s;
                                        Log.v("pplog140", "s:" + s);

                                        PPWarn ppWarn2 = ppWarning(s2);
                                        if (ppWarn2 != null) {
                                            throw new Exception(ppWarn2.msg);
                                        }

                                        commentsStr = s2;
                                        Log.v("pplog140", "s2:" + s2);

                                        return "OK";
                                    }
                                }
                        )
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doFinally(new Action() {
                            @Override
                            public void run() throws Exception {
                                binding.pb.setVisibility(View.INVISIBLE);
                            }
                        })
                        .subscribe(
                                new Consumer<String>() {
                                    @Override
                                    public void accept(String s) throws Exception {
                                        loadContent();
                                        Log.v("pplog140", s);
                                    }
                                },
                                new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable t) throws Exception {
                                        Log.v("pplog140", t.toString());
                                        PPHelper.ppShowError(t.toString());
                                    }
                                })
        );
    }

    private void loadContent() {
        Picasso.with(activityContext)
                .load(PPHelper.get80ImageUrl(PPHelper.ppFromString(momentStr, "data._creator.head").getAsString()))
                .placeholder(R.drawable.profile)
                .into(binding.avatarCiv);

        binding.line1Tv.setText(PPHelper.ppFromString(momentStr, "data._creator.nickname").getAsString());
        binding.line2Tv.setText(PPHelper.ppFromString(momentStr, "data.location.geo").getAsJsonArray().toString());
        binding.mainTv.setText(PPHelper.ppFromString(momentStr, "data.pics").getAsJsonArray().toString());
        binding.contentTv.setText(PPHelper.ppFromString(momentStr, "data.content").getAsString());
        binding.createTimeRttv.setReferenceTime(PPHelper.ppFromString(momentStr, "data.createTime").getAsLong());
        binding.placeTv.setText(PPHelper.ppFromString(momentStr, "data.location.detail").getAsString());
    }
}
