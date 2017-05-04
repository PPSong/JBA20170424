package com.penn.jba.message;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;

import com.ogaclejapan.smarttablayout.SmartTabLayout;
import com.penn.jba.R;
import com.penn.jba.dailyReport.ReportListFragment;
import com.penn.jba.databinding.ActivityLoginBinding;
import com.penn.jba.databinding.ActivityMessageBinding;
import com.penn.jba.databinding.PpTabBinding;
import com.penn.jba.model.MessageEvent;
import com.penn.jba.util.InfoType;
import com.penn.jba.util.MessageType;
import com.penn.jba.util.PPHelper;
import com.penn.jba.util.PPJSONObject;
import com.penn.jba.util.PPRetrofit;
import com.penn.jba.util.PPWarn;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;

import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function3;
import io.reactivex.schedulers.Schedulers;

import static com.penn.jba.util.PPHelper.ppWarning;

public class MessageActivity extends AppCompatActivity {
    private Context activityContext;

    private ActivityMessageBinding binding;

    private ArrayList<Disposable> disposableList = new ArrayList<Disposable>();

    //custom
    private PpTabBinding ppTabBinding1;
    private PpTabBinding ppTabBinding2;
    private PpTabBinding ppTabBinding3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //common
        activityContext = this;
        binding = DataBindingUtil.setContentView(this, R.layout.activity_message);
        //end common

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        EventBus.getDefault().register(this);

        setup();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (Disposable d : disposableList) {
            if (!d.isDisposed()) {
                d.dispose();
            }
        }

        EventBus.getDefault().unregister(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void setup() {
        binding.mainVp.setAdapter(new FragmentStatePagerAdapter(getSupportFragmentManager()) {

            @Override
            public Fragment getItem(int position) {
                Log.v("pplog121", "getItem");
                switch (position) {
                    case 0:
                        return MessageListFragment.newInstance(MessageType.MOMENT);
                    case 1:
                        return MessageListFragment.newInstance(MessageType.FRIEND);
                    case 2:
                        return MessageListFragment.newInstance(MessageType.SYSTEM);
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
                        return getString(R.string.moment);
                    case 1:
                        return getString(R.string.friend);
                    case 2:
                        return getString(R.string.system);
                }
                return "";
            }
        });

        binding.mainTl.setupWithViewPager(binding.mainVp);
        binding.mainVp.setOffscreenPageLimit(3);

        setupTabs();
    }

    //-----helper-----
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        if (event.type == "updateMomentMessageBadge") {
            ppTabBinding1.mainBd.setNumber(Integer.parseInt(event.data));
        } else if (event.type == "updateFriendMessageBadge") {
            ppTabBinding2.mainBd.setNumber(Integer.parseInt(event.data));
        } else if (event.type == "updateSystemMessageBadge") {
            ppTabBinding3.mainBd.setNumber(Integer.parseInt(event.data));
        }
    }

    private void setupTabs() {
        ppTabBinding1 = DataBindingUtil.inflate(getLayoutInflater(), R.layout.pp_tab, null, false);
        ppTabBinding1.mainTv.setText(getString(R.string.moment));
        binding.mainTl.getTabAt(0).setCustomView(ppTabBinding1.getRoot());

        ppTabBinding2 = DataBindingUtil.inflate(getLayoutInflater(), R.layout.pp_tab, null, false);
        ppTabBinding2.mainTv.setText(getString(R.string.friend));
        binding.mainTl.getTabAt(1).setCustomView(ppTabBinding2.getRoot());

        ppTabBinding3 = DataBindingUtil.inflate(getLayoutInflater(), R.layout.pp_tab, null, false);
        ppTabBinding3.mainTv.setText(getString(R.string.system));
        binding.mainTl.getTabAt(2).setCustomView(ppTabBinding3.getRoot());
    }
}
