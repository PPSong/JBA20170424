package com.penn.jba.nearby;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;

import com.penn.jba.R;
import com.penn.jba.databinding.FragmentFootprintBinding;
import com.penn.jba.databinding.FragmentNearbyBinding;
import com.penn.jba.footprint.FootprintAllFragment;
import com.penn.jba.footprint.FootprintMineFragment;

public class NearbyFragment extends Fragment {
    private Context activityContext;

    private FragmentNearbyBinding binding;

    private FragmentPagerAdapter adapterViewPager;

    private Menu menu;

    public NearbyFragment() {
        // Required empty public constructor
    }

    // TODO: Rename and change types and number of parameters
    public static NearbyFragment newInstance() {
        NearbyFragment fragment = new NearbyFragment();

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
                inflater, R.layout.fragment_nearby, container, false);
        View view = binding.getRoot();
        binding.setPresenter(this);
        //end common

        setup();

        return view;
    }

    public void setup() {
        adapterViewPager = new MyPagerAdapter(getChildFragmentManager());
        binding.mainViewPager.setAdapter(adapterViewPager);
        binding.mainViewPager.setSwipeable(false);
    }

    //-----helper-----
    private class MyPagerAdapter extends FragmentPagerAdapter {
        private final int NUM_ITEMS = 1;

        public MyPagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
        }

        // Returns total number of pages
        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        // Returns the fragment to display for that page
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: // Fragment # 0 - This will show FirstFragment
                    return NearbyListModeFragment.newInstance();
                default:
                    return null;
            }
        }

        // Returns the page title for the top indicator
        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: // Fragment # 0 - This will show FirstFragment
                    return getResources().getString(R.string.app_name);
                default:
                    return "";
            }
        }
    }
}
