package com.penn.jba.dailyReport;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.penn.jba.R;
import com.penn.jba.databinding.FragmentReportListBinding;
import com.penn.jba.footprint.FootprintAdapter;
import com.penn.jba.model.realm.CurrentUser;
import com.penn.jba.model.realm.Footprint;

import java.util.ArrayList;

import io.reactivex.disposables.Disposable;
import io.realm.Realm;
import io.realm.RealmResults;

public class ReportListFragment extends Fragment {
    private Context activityContext;

    private ArrayList<Disposable> disposableList = new ArrayList<Disposable>();

    //private RealmResults<Footprint> footprints;

    //private FootprintAdapter footprintAdapter;

    private FragmentReportListBinding binding;

    public ReportListFragment() {
        // Required empty public constructor
    }

    public static ReportListFragment newInstance() {
        ReportListFragment fragment = new ReportListFragment();

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        return  view;
    }

    private void setup() {

    }
}
