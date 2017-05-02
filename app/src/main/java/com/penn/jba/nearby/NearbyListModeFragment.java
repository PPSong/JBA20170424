package com.penn.jba.nearby;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.JsonArray;
import com.penn.jba.R;
import com.penn.jba.databinding.FragmentFootprintBinding;
import com.penn.jba.databinding.FragmentNearbyListModeBinding;
import com.penn.jba.model.Geo;
import com.penn.jba.util.CollectMomentAdapter;
import com.penn.jba.util.NearbyMomentAdapter;
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

import static com.penn.jba.util.PPHelper.ppFromString;
import static com.penn.jba.util.PPHelper.ppWarning;

public class NearbyListModeFragment extends Fragment {
    private Context activityContext;

    private FragmentNearbyListModeBinding binding;

    private ArrayList<Disposable> disposableList = new ArrayList<Disposable>();

    //custom
    private NearbyMomentAdapter nearbyMomentAdapter;

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
        JsonArray tmp = new JsonArray();
        tmp.add("empty");
        setupList(tmp);
        loadContent();
    }

    //-----helper-----

    private void loadContent() {
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

                                        JsonArray userGroups = ppFromString(s, "data.list", PPValueType.ARRAY).getAsJsonArray();

                                        nearbyMomentAdapter.resetData(userGroups);
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

    private void setupList(JsonArray userGroups) {
        nearbyMomentAdapter = new NearbyMomentAdapter(activityContext, userGroups);
        binding.mainFcf.setAdapter(nearbyMomentAdapter);

        binding.mainFcf.setOnScrollPositionListener(onScrollListener());
    }

    private FeatureCoverFlow.OnScrollPositionListener onScrollListener() {
        return new FeatureCoverFlow.OnScrollPositionListener() {
            @Override
            public void onScrolledToPosition(int position) {
                Log.v("NearbyListModeFragment", "position: " + position);
            }

            @Override
            public void onScrolling() {
                Log.i("NearbyListModeFragment", "scrolling");
            }
        };
    }
}
