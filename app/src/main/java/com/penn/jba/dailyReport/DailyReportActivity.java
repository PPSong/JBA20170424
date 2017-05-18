package com.penn.jba.dailyReport;

import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebViewFragment;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.gson.Gson;
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
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;

import static android.R.attr.key;
import static com.penn.jba.util.PPHelper.ppWarning;

public class DailyReportActivity extends AppCompatActivity {
    private Context activityContext;

    private ActivityDailyReportBinding binding;

    private ArrayList<Disposable> disposableList = new ArrayList<Disposable>();

    //custom
    private String dailyReportId;

    private List<DailyReportFragment> fragments;

    private String fans;
    private String collects;
    private String beCollecteds;

    //导航圆点图片
    private ImageView mImageView;
    //导航圆点数组
    private ImageView[] imageViews;

    private int lastPosition;

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
        binding.tl.setTitle(R.string.daily_report_title);
        setSupportActionBar(binding.tl);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        initData();
    }


    //-----helper-----
    private void initData() {
        // 获取dailyReport数据
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
                                           if (PPHelper.ppFromString(s, "data.detail.fans") != null) {
                                               fans = PPHelper.ppFromString(s, "data.detail.fans").getAsJsonArray().toString();
                                           } else if (PPHelper.ppFromString(s, "data.detail.collects") != null) {
                                               collects = PPHelper.ppFromString(s, "data.detail.collects").getAsJsonArray().toString();
                                           } else if (PPHelper.ppFromString(s, "data.detail.beCollecteds") != null) {
                                               beCollecteds = PPHelper.ppFromString(s, "data.detail.beCollecteds").getAsJsonArray().toString();
                                           }
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
    }

    private void setupPage() {
        fragments = new ArrayList<DailyReportFragment>();

        JsonArray fansData = new Gson().fromJson(fans, JsonArray.class);
        JsonArray collectsData = new Gson().fromJson(collects, JsonArray.class);
        JsonArray beCollectedsData = new Gson().fromJson(beCollecteds, JsonArray.class);

        if (beCollectedsData.size() != 0) {
            fragments.add(DailyReportFragment.newInstance("beCollecteds", beCollecteds));
        }
        if (collectsData.size() != 0) {
            fragments.add(DailyReportFragment.newInstance("collects", collects));
        }
        if (fansData.size() != 0) {
            fragments.add(DailyReportFragment.newInstance("fans", fans));
        }

        MyPagerAdapter myFragmentPagerAdapter = new MyPagerAdapter(getSupportFragmentManager(), fragments);
        binding.mainVp.setAdapter(myFragmentPagerAdapter);
        binding.mainVp.setOnPageChangeListener(new myPageChangeListener());
        binding.mainVp.setOffscreenPageLimit(3);

        binding.loadingPb.setVisibility(View.INVISIBLE);
        initGoodsViewPager();
    }

    class MyPagerAdapter extends FragmentPagerAdapter {
        private List<DailyReportFragment> fragments;

        public MyPagerAdapter(FragmentManager fm, List<DailyReportFragment> fragments) {
            super(fm);
            this.fragments = fragments;

        }

        @Override
        public android.support.v4.app.Fragment getItem(int position) {

            return fragments.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }
    }

    private void initGoodsViewPager() {
        //Log.d("weng090",""+fragments.size());
        imageViews = new ImageView[fragments.size()];

        LinearLayout.LayoutParams margin = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        //每个圆点大小

        // 设置每个小圆点距离左边的间距
        margin.setMargins(5, 0, 0, 0);

        for (int i = 0; i < fragments.size(); i++) {

            ImageView imageView = new ImageView(this);

            // 设置每个小圆点的宽高
            imageView.setLayoutParams(new LinearLayout.LayoutParams(5, 5));
            imageViews[i] = imageView;

            if (i == 0) {
                // 默认选中第一张图片
                imageViews[i]
                        .setBackgroundResource(R.drawable.goods_indicator_focused);
            } else {
                // 其他图片都设置未选中状态
                imageViews[i]
                        .setBackgroundResource(R.drawable.goods_indicator_unfocused);
            }
            binding.layoutDot.addView(imageViews[i], margin);
        }
    }

    private class myPageChangeListener implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageScrollStateChanged(int arg0) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onPageSelected(int arg0) {
            // 遍历数组让当前选中图片下的小圆点设置颜色
            for (int i = 0; i < imageViews.length; i++) {
                imageViews[arg0].setBackgroundResource(R.drawable.goods_indicator_focused);
                if (arg0 != i) {
                    imageViews[i].setBackgroundResource(R.drawable.goods_indicator_unfocused);
                }
            }
        }
    }
}
