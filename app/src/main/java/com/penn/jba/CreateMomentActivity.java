package com.penn.jba;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.penn.jba.databinding.ActivityCreateMomentBinding;
import com.penn.jba.model.Geo;
import com.penn.jba.model.realm.Footprint;
import com.penn.jba.model.realm.Pic;
import com.penn.jba.util.FootprintStatus;
import com.penn.jba.util.MomentImagePreviewAdapter;
import com.penn.jba.util.PPHelper;
import com.penn.jba.util.PPJSONObject;
import com.penn.jba.util.PPRetrofit;
import com.penn.jba.util.PPWarn;
import com.penn.jba.util.PicStatus;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UploadManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmList;

import static com.penn.jba.util.PPHelper.ppWarning;
import static io.reactivex.Observable.mergeDelayError;
import static io.reactivex.Observable.zip;

public class CreateMomentActivity extends AppCompatActivity {
    private Context activityContext;

    private ActivityCreateMomentBinding binding;

    private ArrayList<Disposable> disposableList = new ArrayList<Disposable>();

    private Footprint footprint;

    private JSONArray geoJsonArray;

    private Realm realm;

    private String key;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //common
        activityContext = this;
        binding = DataBindingUtil.setContentView(this, R.layout.activity_create_moment);
        binding.setPresenter(this);
        //end common

        realm = Realm.getDefaultInstance();

        key = getIntent().getStringExtra("key");

        Gson gson = new Gson();

        String geoStr = PPHelper.getPrefStringValue("geo", gson.toJson(Geo.getDefaultGeo()));

        Geo geo = gson.fromJson(geoStr, Geo.class);

        geoJsonArray = new JSONArray();

        try {
            geoJsonArray.put(geo.lon);
            geoJsonArray.put(geo.lat);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        setup();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realm.close();
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
                        binding.publishBt.setEnabled(aBoolean);
                    }
                })
        );

        //发布按钮监控
        Observable<Object> publishButtonObservable = RxView.clicks(binding.publishBt)
                .debounce(200, TimeUnit.MILLISECONDS);

        disposableList.add(publishButtonObservable
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                publishMoment();
                            }
                        }
                )
        );

        footprint = realm.where(Footprint.class).equalTo("status", FootprintStatus.PREPARE.toString()).findFirst();
        int widthDp = 64;
        final float scale = activityContext.getResources().getDisplayMetrics().density;
        int width = activityContext.getResources().getDisplayMetrics().widthPixels;
        int pixels = (int) (widthDp * scale + 0.5f);
        int cols = width / pixels;
        binding.imagePreviewGv.setNumColumns(cols);
        MomentImagePreviewAdapter momentImagePreviewAdapter = new MomentImagePreviewAdapter(activityContext, footprint.getPics(), pixels);
        binding.imagePreviewGv.setAdapter(momentImagePreviewAdapter);
    }

    private void publishMoment() {
        String content = binding.contentEt.getText().toString();
        String place = binding.placeEt.getText().toString();

        if (TextUtils.isEmpty(content) || TextUtils.isEmpty(place)) {
            PPHelper.ppShowError(activityContext.getResources().getString(R.string.moment_content_place_required));

            return;
        }

        try {
            JSONObject body = new JSONObject()
                    .put("detail", new JSONObject()
                            .put("content", content)
                            .put("location", new JSONObject()
                                    .put("geo", geoJsonArray)
                                    .put("detail", place)
                            )
                    );

            realm.beginTransaction();
            footprint.setBody(body.toString());
            footprint.setStatus(FootprintStatus.LOCAL);
            realm.commitTransaction();

            setResult(RESULT_OK);
            finish();

        } catch (JSONException e) {
            Log.v("ppLog", "api data error:" + e);
            PPHelper.ppShowError(e.toString());

            return;
        }
    }
}
