package com.penn.jba.footprint;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.penn.jba.R;
import com.penn.jba.databinding.FragmentFootprintMineBinding;

public class FootprintMineFragment extends Fragment {
    private Context activityContext;

    private FragmentFootprintMineBinding binding;

    public FootprintMineFragment() {
        // Required empty public constructor
    }

    public static FootprintMineFragment newInstance() {
        FootprintMineFragment fragment = new FootprintMineFragment();

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityContext = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //common
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_footprint_mine, container, false);
        View view = binding.getRoot();
        binding.setPresenter(this);
        //end common

        setup();

        return view;
    }

    public void setup() {

    }
}