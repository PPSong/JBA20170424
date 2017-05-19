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

import com.github.florent37.materialviewpager.header.MaterialViewPagerHeaderDecorator;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.penn.jba.R;
import com.penn.jba.databinding.FragmentReportListBinding;

import java.util.ArrayList;

import io.reactivex.disposables.Disposable;

public class ReportListFragment extends Fragment {
    private Context activityContext;

    private ArrayList<Disposable> disposableList = new ArrayList<Disposable>();

    private String type;
    private String data = "";

    //private FootprintAdapter footprintAdapter;

    private FragmentReportListBinding binding;

    public ReportListFragment() {
        // Required empty public constructor
    }

    public static ReportListFragment newInstance(String type, String data) {
        ReportListFragment fragment = new ReportListFragment();
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
                inflater, R.layout.fragment_report_list, container, false);
        View view = binding.getRoot();
        binding.setPresenter(this);
        //end common

        setup();

        return view;
    }

    private void setup() {
        binding.mainRv.setLayoutManager(new LinearLayoutManager(activityContext));
        binding.mainRv.setHasFixedSize(true);
        JsonArray tmpData = new Gson().fromJson(data, JsonArray.class);
        Log.v("pplog121", tmpData.toString());
        binding.mainRv.addItemDecoration(new MaterialViewPagerHeaderDecorator());
        binding.mainRv.setAdapter(new ReportListAdapter(activityContext, type, tmpData));
    }
}
