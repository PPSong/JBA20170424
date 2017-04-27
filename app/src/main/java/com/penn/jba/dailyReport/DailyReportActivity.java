package com.penn.jba.dailyReport;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.WebViewFragment;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.florent37.materialviewpager.MaterialViewPager;
import com.github.florent37.materialviewpager.header.HeaderDesign;
import com.penn.jba.R;
import com.penn.jba.databinding.ActivityDailyReportBinding;
import com.penn.jba.model.realm.CurrentUser;
import com.penn.jba.util.PPHelper;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.util.ArrayList;

import io.reactivex.disposables.Disposable;
import io.realm.Realm;

public class DailyReportActivity extends AppCompatActivity {
    private Context activityContext;

    private ActivityDailyReportBinding binding;

    private ArrayList<Disposable> disposableList = new ArrayList<Disposable>();

    //custom
    private String dailyReportId;

    private ArrayList<String> profilePics = new ArrayList();

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

    private void setup() {
        try (Realm realm = Realm.getDefaultInstance()) {
            CurrentUser currentUser = realm.where(CurrentUser.class).findFirst();
            Picasso.with(activityContext)
                    .load(PPHelper.get80ImageUrl(currentUser.getHead()))
                    .placeholder(R.drawable.profile)
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

        binding.materialViewPager.getViewPager().setAdapter(new FragmentStatePagerAdapter(getSupportFragmentManager()) {

            @Override
            public Fragment getItem(int position) {
                switch (position) {
                    case 0:
                        return ReportListFragment.newInstance();
                    case 1:
                        return ReportListFragment.newInstance();
                    case 2:
                        return ReportListFragment.newInstance();
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

        binding.materialViewPager.getViewPager().setOffscreenPageLimit(binding.materialViewPager.getViewPager().getAdapter().getCount());
        binding.materialViewPager.getPagerTitleStrip().setViewPager(binding.materialViewPager.getViewPager());

        binding.materialViewPager.getViewPager().addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    setSubTitle("今天我被" + 1 + "人迹录了片刻");
                } else if (position == 1) {
                    setSubTitle("今天我迹录了" + 1 + "人片刻");
                } else if (position == 2) {
                    setSubTitle("今天我增加了" + 1 + "个新粉丝");
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        setSubTitle("今天我被" + 1 + "人迹录了片刻");
    }

    //-----helper-----
    private void setSubTitle(String str) {
        ((TextView) binding.materialViewPager.findViewById(R.id.main_tv)).setText(str);
    }
}
