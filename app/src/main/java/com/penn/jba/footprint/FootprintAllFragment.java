package com.penn.jba.footprint;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.penn.jba.R;
import com.penn.jba.databinding.FragmentFootprintAllBinding;
import com.penn.jba.databinding.FragmentFootprintBinding;

public class FootprintAllFragment extends Fragment {
    private Context activityContext;

    private FragmentFootprintAllBinding binding;

    public FootprintAllFragment() {
        // Required empty public constructor
    }

    public static FootprintAllFragment newInstance() {
        FootprintAllFragment fragment = new FootprintAllFragment();

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
                inflater, R.layout.fragment_footprint_all, container, false);
        View view = binding.getRoot();
        binding.setPresenter(this);
        //end common

        setup();

        return view;
    }

    public void setup() {

    }
}
