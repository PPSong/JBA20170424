package com.penn.jba.dailyReport;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.webkit.WebViewFragment;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.florent37.materialviewpager.MaterialViewPager;
import com.github.florent37.materialviewpager.header.HeaderDesign;
import com.google.gson.JsonArray;
import com.penn.jba.R;
import com.penn.jba.databinding.ActivityDailyReportBinding;
import com.penn.jba.model.realm.CurrentUser;
import com.penn.jba.util.PPHelper;
import com.penn.jba.util.PPJSONObject;
import com.penn.jba.util.PPRetrofit;
import com.penn.jba.util.PPValueType;
import com.penn.jba.util.PPWarn;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;

import static android.R.attr.key;
import static com.penn.jba.R.id.materialViewPager;
import static com.penn.jba.util.PPHelper.ppWarning;

public class DailyReportActivity extends AppCompatActivity {
    private Context activityContext;

    private ActivityDailyReportBinding binding;

    private ArrayList<Disposable> disposableList = new ArrayList<Disposable>();

    //custom
    private String dailyReportId;

    private ArrayList<String> profilePics = new ArrayList();

    private String fans;
    private String collects;
    private String beCollecteds;
    private JsonArray beCollectedsJsonArray;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //common
        activityContext = this;
        binding = DataBindingUtil.setContentView(this, R.layout.activity_daily_report);
        binding.setPresenter(this);
        //end common

        dailyReportId = getIntent().getStringExtra("dailyReportId");

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
    public void onDestroy() {
        super.onDestroy();
        for (Disposable d : disposableList) {
            if (!d.isDisposed()) {
                d.dispose();
            }
        }
    }

    private void setup() {
        //获取dailyReport数据
        PPJSONObject jBodyFans = new PPJSONObject();
        jBodyFans
                .put("id", dailyReportId)
                .put("key", new JSONArray().put("fans"));

        final Observable<String> apiResultFans = PPRetrofit.getInstance()
                .api("footprint.readDailyReport", jBodyFans.getJSONObject());

        PPJSONObject jBodyCollects = new PPJSONObject();
        jBodyCollects
                .put("id", dailyReportId)
                .put("key", new JSONArray().put("collects"));

        final Observable<String> apiResultCollects = PPRetrofit.getInstance()
                .api("footprint.readDailyReport", jBodyCollects.getJSONObject());

        PPJSONObject jBodyBeCollecteds = new PPJSONObject();
        jBodyBeCollecteds
                .put("id", dailyReportId)
                .put("key", new JSONArray().put("beCollecteds"));

        final Observable<String> apiResultBeCollecteds = PPRetrofit.getInstance()
                .api("footprint.readDailyReport", jBodyBeCollecteds.getJSONObject());

        ArrayList<Observable<String>> obsList = new ArrayList();

        obsList.add(apiResultFans);
        obsList.add(apiResultCollects);
        obsList.add(apiResultBeCollecteds);

        disposableList.add(
                Observable.merge(obsList)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<String>() {
                                       private int count = 0;

                                       @Override
                                       public void accept(String s) throws Exception {
                                           //pptodo 这里如果有ppwarn的话需要抛出exception
                                           Log.v("pplog121", s);
                                           Log.v("pplog121", "pptest:" + PPHelper.ppFromString(s, "data.detail.fans"));
                                           if (PPHelper.ppFromString(s, "data.detail.fans") != null) {
                                               Log.v("pplog121", "accept1:");
                                               fans = PPHelper.ppFromString(s, "data.detail.fans").getAsJsonArray().toString();
                                               Log.v("pplog121", "accept2:");
                                               Log.v("pplog121", "accept:" + fans);
                                           } else if (PPHelper.ppFromString(s, "data.detail.collects") != null) {
                                               collects = PPHelper.ppFromString(s, "data.detail.collects").getAsJsonArray().toString();
                                               Log.v("pplog121", "accept:" + collects);
                                           } else if (PPHelper.ppFromString(s, "data.detail.beCollecteds") != null) {
                                               beCollecteds = PPHelper.ppFromString(s, "data.detail.beCollecteds").getAsJsonArray().toString();
                                               Log.v("pplog121", "accept:" + beCollecteds);
                                           }

                                           Log.v("pplog121", "end");
                                           if (++count == 3) {
                                               //请求全部结束
                                               setupPage();
                                           }
                                       }
                                   },
                                new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable t) throws Exception {
                                        Log.v("pplog102", "error102:" + t);
                                    }
                                }));

        try (Realm realm = Realm.getDefaultInstance()) {
            CurrentUser currentUser = realm.where(CurrentUser.class).findFirst();
            Picasso.with(activityContext)
                    .load(PPHelper.get80ImageUrl(currentUser.getHead()))
                    .placeholder(R.drawable.pictures_no)
                    .into((ImageView) binding.materialViewPager.findViewById(R.id.avatar_iv));
            int size = currentUser.getPics().size();
            if (size == 0) {
                //pptodo 替换成七牛上的默认图片
                profilePics.add("http://phandroid.s3.amazonaws.com/wp-content/uploads/2014/06/android_google_moutain_google_now_1920x1080_wallpaper_Wallpaper-HD_2560x1600_www.paperhi.com_-640x400.jpg");
            }
            for (int i = 0; i < size; i++) {
                profilePics.add(currentUser.getPics().get(i).getNetFileName());
            }
        }
    }

    //-----helper-----
    private void setupPage() {
        Log.v("pplog121", "setupPage");
        binding.materialViewPager.getViewPager().setAdapter(new FragmentStatePagerAdapter(getSupportFragmentManager()) {

            @Override
            public Fragment getItem(int position) {
                Log.v("pplog121", "getItem");
                switch (position) {
                    case 0:
                        Log.v("pplog121", "pptest1:" + beCollecteds);
                        beCollectedsJsonArray = PPHelper.ppFromString(beCollecteds, "").getAsJsonArray();
                        return ReportListFragment.newInstance("beCollecteds", beCollecteds);
                    case 1:
                        Log.v("pplog121", "pptest2:" + collects);
                        return ReportListFragment.newInstance("collects", collects);
                    case 2:
                        Log.v("pplog121", "pptest3:" + fans);
                        return ReportListFragment.newInstance("fans", fans);
                }

                return null;
            }

            @Override
            public int getCount() {
                return 3;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                switch (position) {
                    case 0:
                        return "被迹录";
                    case 1:
                        return "迹录";
                    case 2:
                        return "新粉丝";
                }
                return "";
            }
        });

        Log.v("pplog121", "setupPage2");

        binding.materialViewPager.setMaterialViewPagerListener(new MaterialViewPager.Listener() {
            @Override
            public HeaderDesign getHeaderDesign(int page) {
                switch (page) {
                    case 0:
                        return HeaderDesign.fromColorResAndUrl(
                                R.color.green,
                                PPHelper.get800ImageUrl(profilePics.get(0 % profilePics.size()))
                        );
                    case 1:
                        return HeaderDesign.fromColorResAndUrl(
                                R.color.blue,
                                PPHelper.get800ImageUrl(profilePics.get(1 % profilePics.size()))
                        );
                    case 2:
                        return HeaderDesign.fromColorResAndUrl(
                                R.color.cyan,
                                PPHelper.get800ImageUrl(profilePics.get(2 % profilePics.size()))
                        );
                }

                return null;
            }
        });

        Log.v("pplog121", "setupPage7");

        binding.materialViewPager.getViewPager().setOffscreenPageLimit(binding.materialViewPager.getViewPager().getAdapter().getCount());
        binding.materialViewPager.getPagerTitleStrip().setViewPager(binding.materialViewPager.getViewPager());

        Log.v("pplog121", "setupPage8");

        binding.materialViewPager.getViewPager().addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    setSubTitle("今天我被" + beCollectedsJsonArray.size() + "人迹录了片刻");
                } else if (position == 1) {
                    JsonArray ja = PPHelper.ppFromString(collects, "").getAsJsonArray();
                    setSubTitle("今天我迹录了" + ja.size() + "人片刻");
                } else if (position == 2) {
                    JsonArray ja = PPHelper.ppFromString(fans, "").getAsJsonArray();
                    setSubTitle("今天我增加了" + ja.size() + "个新粉丝");
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        Log.v("pplog121", "setupPage9");

        setSubTitle("今天我被" + beCollectedsJsonArray.size() + "人迹录了片刻");
        Log.v("pplog121", "setupPage10");

        Toolbar toolbar = binding.materialViewPager.getToolbar();
        if (toolbar != null) {
            setSupportActionBar(toolbar);

            final ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        }
    }

    private void setSubTitle(String str) {
        ((TextView) binding.materialViewPager.findViewById(R.id.main_tv)).setText(str);
    }
}
