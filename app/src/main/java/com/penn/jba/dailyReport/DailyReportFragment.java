package com.penn.jba.dailyReport;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.penn.jba.R;
import com.penn.jba.databinding.DailyReportFgmtBinding;
import com.penn.jba.model.realm.CurrentUser;
import com.penn.jba.util.PPHelper;
import com.penn.jba.view.MyDecoration;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import io.reactivex.disposables.Disposable;
import io.realm.Realm;

/**
 * Created by raighne on 5/17/17.
 */

public class DailyReportFragment extends Fragment {
    private Context activityContext;

    private ArrayList<Disposable> disposableList = new ArrayList<Disposable>();

    private Realm realm;
    private CurrentUser currentUser;

    private String type;
    private String data = "";

    private DailyReportFgmtBinding binding;

    public DailyReportFragment() {
        // Required empty public constructor
    }

    public static DailyReportFragment newInstance(String type, String data) {
        DailyReportFragment fragment = new DailyReportFragment();
        Bundle args = new Bundle();
        args.putString("type", type);
        args.putString("data", data);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityContext = getActivity();
        if (getArguments() != null) {
            type = getArguments().getString("type");
            data = getArguments().getString("data");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //common
        binding = DataBindingUtil.inflate(
                inflater, R.layout.daily_report_fgmt, container, false);
        View view = binding.getRoot();
        binding.setPresenter(this);
        //end common

        setup();

        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        realm.close();
        for (Disposable d : disposableList) {
            if (!d.isDisposed()) {
                d.dispose();
            }
        }
    }

    private void setup() {
        String tmpHeader = "";
        realm = Realm.getDefaultInstance();
        currentUser = realm.where(CurrentUser.class).findFirst();
        tmpHeader = currentUser.getBanner();

        Picasso.with(activityContext)
                .load(PPHelper.get80ImageUrl(tmpHeader))
                .into(binding.headerCiv);

        JsonArray tmpData = new Gson().fromJson(data, JsonArray.class);
        if (type.equals("fans")) {
            binding.headerTv1.setText("今天我增加了");
            binding.headerTv2.setText(tmpData.size() + "个新粉丝");
        } else if (type.equals("collects")) {
            binding.headerTv1.setText("今天我迹录了");
            binding.headerTv2.setText(tmpData.size() + "次他人片刻");
        } else {
            binding.headerTv1.setText("今天我被");
            binding.headerTv2.setText(tmpData.size() + "人迹录了片刻");
        }

        binding.mainRv.setLayoutManager(new LinearLayoutManager(activityContext));
        binding.mainRv.setHasFixedSize(true);
        Log.v("pplog121", tmpData.toString());
        binding.mainRv.addItemDecoration(new MyDecoration(activityContext));
        binding.mainRv.setAdapter(new ReportListAdapter(activityContext, type, tmpData));
    }
}
