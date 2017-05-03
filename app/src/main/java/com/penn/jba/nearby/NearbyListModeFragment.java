package com.penn.jba.nearby;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.JsonArray;
import com.penn.jba.MomentDetailActivity;
import com.penn.jba.R;
import com.penn.jba.databinding.FragmentFootprintBinding;
import com.penn.jba.databinding.FragmentNearbyListModeBinding;
import com.penn.jba.footprint.FootprintFragment;
import com.penn.jba.model.Geo;
import com.penn.jba.util.CollectMomentAdapter;
import com.penn.jba.util.PPHelper;
import com.penn.jba.util.PPJSONObject;
import com.penn.jba.util.PPRetrofit;
import com.penn.jba.util.PPValueType;
import com.penn.jba.util.PPWarn;

import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import it.moondroid.coverflow.components.ui.containers.FeatureCoverFlow;
import me.crosswall.lib.coverflow.CoverFlow;
import me.crosswall.lib.coverflow.core.PageItemClickListener;

import static com.penn.jba.R.id.container;
import static com.penn.jba.util.PPHelper.ppFromString;
import static com.penn.jba.util.PPHelper.ppWarning;

public class NearbyListModeFragment extends Fragment {
    private Context activityContext;

    private FragmentNearbyListModeBinding binding;

    private ArrayList<Disposable> disposableList = new ArrayList<Disposable>();

    //custom
    private JsonArray data;

    private Menu menu;

    public NearbyListModeFragment() {
        // Required empty public constructor
    }

    public static NearbyListModeFragment newInstance() {
        NearbyListModeFragment fragment = new NearbyListModeFragment();

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityContext = getActivity();
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //common
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_nearby_list_mode, container, false);
        View view = binding.getRoot();
        binding.setPresenter(this);
        //end common

        setup();

        return view;
    }

    public void setup() {
        loadContent();
    }

    //-----helper-----

    public void loadContent() {
        binding.mainPc.setVisibility(View.INVISIBLE);
        binding.pb.setVisibility(View.VISIBLE);
        PPJSONObject jBody = new PPJSONObject();

        Geo tmpGeo = PPHelper.getLatestGeo();

        jBody
                .put("geo", tmpGeo.lon + "," + tmpGeo.lat)
                //pptodo 需要把以下true改为false
                .put("refresh", "true");

        final Observable<String> apiResult = PPRetrofit.getInstance()
                .api("moment.searchWithUserGroup", jBody.getJSONObject());

        disposableList.add(
                apiResult
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doFinally(new Action() {
                            @Override
                            public void run() throws Exception {
                                binding.mainPc.setVisibility(View.VISIBLE);
                                binding.pb.setVisibility(View.INVISIBLE);
                            }
                        })
                        .subscribe(
                                new Consumer<String>() {
                                    @Override
                                    public void accept(String s) throws Exception {
                                        PPWarn ppWarn1 = ppWarning(s);
                                        if (ppWarn1 != null) {
                                            throw new Exception(ppWarn1.msg);
                                        }

                                        final JsonArray userGroups = ppFromString(s, "data.list", PPValueType.ARRAY).getAsJsonArray();
                                        data = userGroups;
                                        binding.mainVp.setAdapter(new MyPagerAdapter(getChildFragmentManager()));

                                        new CoverFlow.Builder()
                                                .with(binding.mainVp)
                                                .scale(0.1f)
                                                .rotationY(0f)
                                                .build();

                                        binding.mainVp.setOffscreenPageLimit(3);
                                    }
                                },
                                new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable t) throws Exception {
                                        Log.v("pplog", "error:" + t);
                                        PPHelper.ppShowError(t.toString());
                                    }
                                })
        );
    }

    private class MyPagerAdapter extends FragmentPagerAdapter {

        public MyPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        // Returns total number of pages
        @Override
        public int getCount() {
            return data.size();
        }

        // Returns the fragment to display for that page
        @Override
        public Fragment getItem(int position) {
            return NearbyListMomentGroupItemFragment.newInstance(data.get(position).toString());
        }

        // Returns the page title for the top indicator
        @Override
        public CharSequence getPageTitle(int position) {
            return getResources().getString(R.string.nearby);
        }
    }
}
