package com.penn.jba;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.penn.jba.databinding.ActivityCreateMomentBinding;
import com.penn.jba.model.Geo;
import com.penn.jba.model.realm.Footprint;
import com.penn.jba.util.FootprintStatus;
import com.penn.jba.util.PPHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;

public class CreateMomentActivity extends AppCompatActivity {
    private Context activityContext;

    private ActivityCreateMomentBinding binding;

    private ArrayList<Disposable> disposableList = new ArrayList<Disposable>();

    private Footprint footprint;

    private Geo geo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //common
        activityContext = this;
        binding = DataBindingUtil.setContentView(this, R.layout.activity_create_moment);
        binding.setPresenter(this);
        //end common

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

    //-----helper-----
    private void setup() {
        //内容输入监控
        Observable<String> momentContentInputObservable = RxTextView.textChanges(binding.contentEt)
                .skip(1)
                .map(new Function<CharSequence, String>() {
                    @Override
                    public String apply(CharSequence charSequence) throws Exception {
                        return PPHelper.isMomentContentValid(activityContext, charSequence.toString());
                    }
                }).doOnNext(
                        new Consumer<String>() {
                            @Override
                            public void accept(String error) throws Exception {
                                binding.contentEt.setError(TextUtils.isEmpty(error) ? null : error);
                            }
                        }
                );

        //地址输入监控
        Observable<String> placeInputObservable = RxTextView.textChanges(binding.placeEt)
                .skip(1)
                .map(new Function<CharSequence, String>() {
                    @Override
                    public String apply(CharSequence charSequence) throws Exception {
                        return PPHelper.isPlaceValid(activityContext, charSequence.toString());
                    }
                }).doOnNext(
                        new Consumer<String>() {
                            @Override
                            public void accept(String error) throws Exception {
                                binding.placeEt.setError(TextUtils.isEmpty(error) ? null : error);
                            }
                        }
                );

        //发布按钮是否可用
        disposableList.add(Observable.combineLatest(
                momentContentInputObservable,
                placeInputObservable,
                new BiFunction<String, String, Boolean>() {
                    @Override
                    public Boolean apply(String s, String s2) throws Exception {
                        return TextUtils.isEmpty(s) && TextUtils.isEmpty(s2);
                    }
                })
                .subscribeOn(Schedulers.io())
                .distinctUntilChanged()
                .debounce(200, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        Log.v("pplog23", "test");
                        binding.publishBt.setEnabled(aBoolean);
                    }
                })
        );

        //登录按钮监控
        Observable<Object> publishButtonObservable = RxView.clicks(binding.publishBt)
                .debounce(200, TimeUnit.MILLISECONDS);

        disposableList.add(publishButtonObservable
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.io())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                publishMoment();
                            }
                        }
                )
        );
    }

    private void publishMoment() {
        String content = binding.contentEt.getText().toString();
        String place = binding.placeEt.getText().toString();

        if (TextUtils.isEmpty(content) || TextUtils.isEmpty(place)) {
            PPHelper.ppShowError(activityContext.getResources().getString(R.string.moment_content_place_required));

            return;
        }

        try {
            JSONArray arr = new JSONArray();
            String geo = PPHelper.getPrefStringValue("geo", "121.52619934082031,31.216968536376953");
            String[] tmpGeo = geo.split(",");
            float lon = Float.parseFloat(tmpGeo[0]);
            float lat = Float.parseFloat(tmpGeo[1]);
            arr.put(lon);
            arr.put(lat);

            JSONObject body = new JSONObject()
                    .put("detail", new JSONObject()
                            .put("content", content)
                            .put("location", new JSONObject()
                                    .put("geo", arr)
                                    .put("detail", place)
                            )
                    );

            long now = System.currentTimeMillis();
            footprint = new Footprint();
            footprint.setKey("" + now + "_3_" + PPHelper.currentUserId + "_" + true);
            footprint.setCreateTime(now);
            footprint.setStatus(FootprintStatus.LOCAL);
            footprint.setType(3);
            footprint.setBody(body.toString());
            footprint.setMine(true);

            try (Realm realm = Realm.getDefaultInstance()) {
                realm.beginTransaction();
                final Footprint ft = realm.copyToRealm(footprint);
                realm.commitTransaction();
            }

            finish();

        } catch (JSONException e) {
            Log.v("ppLog", "api data error:" + e);
            PPHelper.ppShowError(e.toString());

            return;
        }
    }
}
