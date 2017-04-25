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

    private Configuration config = new Configuration.Builder().build();

    private UploadManager uploadManager = new UploadManager(config);

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

            uploadMoment();

            finish();

        } catch (JSONException e) {
            Log.v("ppLog", "api data error:" + e);
            PPHelper.ppShowError(e.toString());

            return;
        }
    }

    private Observable<String> uploadSingleImage(final byte[] data, final String key, final String token) {
        return Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(final ObservableEmitter<String> emitter) throws Exception {
                uploadManager.put(data, key, token,
                        new UpCompletionHandler() {
                            @Override
                            public void complete(String key, ResponseInfo info, JSONObject res) {
                                //res包含hash、key等信息，具体字段取决于上传策略的设置
                                if (info.isOK()) {
                                    Log.i("qiniu", "Upload Success:" + key);
                                    //修改本地数据库对应图片状态为NET
                                    try (Realm realm = Realm.getDefaultInstance()) {
                                        realm.beginTransaction();
                                        Pic pic = realm.where(Pic.class).equalTo("key", key).findFirst();
                                        if (pic != null) {
                                            pic.setStatus(PicStatus.NET);
                                        } else {
                                            Exception apiError = new Exception("七牛上传:" + key + "失败", new Throwable("realm中没有找到指定图片"));
                                            emitter.onError(apiError);
                                        }
                                        realm.commitTransaction();
                                    }
                                    emitter.onNext(key);
                                    emitter.onComplete();
                                } else {
                                    Log.i("qiniu", "Upload Fail");
                                    //如果失败，这里可以把info信息上报自己的服务器，便于后面分析上传错误原因
                                    Exception apiError = new Exception("七牛上传:" + key + "失败", new Throwable(info.error.toString()));
                                    emitter.onError(apiError);
                                }
                                Log.i("qiniu", key + ",\r\n " + info + ",\r\n " + res);
                            }
                        }, null);

            }
        });
    }

    public void uploadMoment() {
        ArrayList<Observable<String>> obsList = new ArrayList();

        PPJSONObject jBody0 = new PPJSONObject();

        try (Realm realm = Realm.getDefaultInstance()) {
            final Footprint ft = realm.where(Footprint.class).equalTo("key", key).findFirst();
            //上传图片
            RealmList<Pic> pics = ft.getPics();

            for (int i = 0; i < pics.size(); i++) {
                final Pic item = pics.get(i);
                if (item.getStatus().equals(PicStatus.NET.toString())) {
                    continue;
                }
                final byte[] tmpData = item.getLocalData();
                final String key = item.getKey();
                PPJSONObject jBody = new PPJSONObject();
                jBody
                        .put("type", "public")
                        .put("filename", key);

                final Observable<String> apiResult1 = PPRetrofit.getInstance().api("system.generateUploadToken", jBody.getJSONObject());

                obsList.add(apiResult1.flatMap(new Function<String, ObservableSource<String>>() {
                    @Override
                    public ObservableSource<String> apply(String tokenMsg) throws Exception {
                        PPWarn ppWarn = ppWarning(tokenMsg);
                        if (ppWarn != null) {
                            throw new Exception("ppError:" + ppWarn.msg + ":" + key);
                        }
                        String token = PPHelper.ppFromString(tokenMsg, "data.token").getAsString();
                        return uploadSingleImage(tmpData, key, token);
                    }
                }));
            }

            JSONArray jsonArrayPics = new JSONArray();

            for (Pic pic : pics) {
                jsonArrayPics.put(pic.getKey());
            }
            jBody0
                    .put("pics", jsonArrayPics)
                    .put("address", ft.getPlace())
                    .put("geo", geoJsonArray)
                    .put("content", ft.getContent())
                    .put("createTime", ft.getCreateTime());
        }

        final Observable<String> apiResult = PPRetrofit.getInstance()
                .api("moment.publish", jBody0.getJSONObject());

        disposableList.add(
                Observable.mergeDelayError(obsList)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .concatWith(apiResult)
                        .observeOn(Schedulers.io())
                        .subscribe(new Consumer<String>() {
                                       @Override
                                       public void accept(String s) throws Exception {
                                           Log.v("pplog102", "ok:" + s);
                                       }
                                   },
                                new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable t) throws Exception {
                                        Log.v("pplog102", "error:" + t);
                                        uploadMomentFailed();
                                    }
                                }));
    }

    private void uploadMomentFailed() {
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.beginTransaction();
            realm.where(Footprint.class)
                    .equalTo("key", key)
                    .findFirst()
                    .setStatus(FootprintStatus.FAILED);
            realm.commitTransaction();
        }
    }
}
