package com.penn.jba.nearby;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.gson.JsonArray;
import com.jakewharton.rxbinding2.view.RxView;
import com.penn.jba.MomentDetailActivity;
import com.penn.jba.PPApplication;
import com.penn.jba.R;
import com.penn.jba.databinding.FragmentFootprintBinding;
import com.penn.jba.databinding.FragmentNearbyListMomentGroupItemBinding;
import com.penn.jba.util.PPHelper;
import com.penn.jba.util.PPJSONObject;
import com.penn.jba.util.PPRetrofit;
import com.penn.jba.util.PPWarn;
import com.squareup.picasso.Picasso;
import com.synnapps.carouselview.ImageClickListener;
import com.synnapps.carouselview.ImageListener;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static com.penn.jba.R.string.i_reply_to_sb;
import static com.penn.jba.util.PPHelper.ppWarning;

public class NearbyListMomentGroupItemFragment extends Fragment {
    private Context activityContext;

    private ArrayList<Disposable> disposableList = new ArrayList<Disposable>();

    private FragmentNearbyListMomentGroupItemBinding binding;

    //custom
    private String momentGroupStr;

    public NearbyListMomentGroupItemFragment() {
        // Required empty public constructor
    }

    public static NearbyListMomentGroupItemFragment newInstance(String momentGroupStr) {
        NearbyListMomentGroupItemFragment fragment = new NearbyListMomentGroupItemFragment();
        Bundle args = new Bundle();
        args.putString("momentGroupStr", momentGroupStr);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityContext = getActivity();
        if (getArguments() != null) {
            momentGroupStr = getArguments().getString("momentGroupStr");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //common
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_nearby_list_moment_group_item, container, false);
        View view = binding.getRoot();
        //end common

        setup();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        for (Disposable d : disposableList) {
            if (!d.isDisposed()) {
                d.dispose();
            }
        }
    }

    private void setup() {
        //follow按钮监控
        Observable<Object> followButtonObservable = RxView.clicks(binding.followTaBt)
                .debounce(200, TimeUnit.MILLISECONDS);

        disposableList.add(followButtonObservable
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                if (binding.followTaBt.getText().equals(getString(R.string.follow))) {
                                    follow();
                                } else {
                                    unFollow();
                                }

                            }
                        }
                )
        );

        Picasso.with(activityContext)
                .load(PPHelper.get80ImageUrl(PPHelper.ppFromString(momentGroupStr, "head").getAsString()))
                .placeholder(R.drawable.pictures_no)
                .into(binding.avatarCiv);

        binding.nicknameTv.setText(PPHelper.ppFromString(momentGroupStr, "nickname").getAsString());
        boolean isFollowed = PPHelper.ppFromString(momentGroupStr, "isFollowed").getAsInt() == 0 ? false : true;
        if (isFollowed) {
            binding.followTaBt.setText(getString(R.string.cancel_follow));
        } else {
            binding.followTaBt.setText(getString(R.string.follow));
        }

        //set moment相关
        final JsonArray moments = PPHelper.ppFromString(momentGroupStr, "moments").getAsJsonArray();
        binding.mainCv.setPageCount(moments.size());

        ImageListener imageListener = new ImageListener() {
            @Override
            public void setImageForPosition(int position, ImageView imageView) {
                Log.v("pplog151", "setImageForPosition");
                Picasso.with(activityContext)
                        .load(PPHelper.get800ImageUrl(PPHelper.ppFromString(momentGroupStr, "moments." + position + ".pics.0").getAsString()))
                        .placeholder(R.drawable.header)
                        .into(imageView);
            }

        };

        binding.mainCv.setImageListener(imageListener);

        binding.mainCv.setImageClickListener(new ImageClickListener() {
            @Override
            public void onClick(int position) {
                String momentId = PPHelper.ppFromString(momentGroupStr, "moments." + position + ".id").getAsString();
                Intent intent = new Intent(activityContext, MomentDetailActivity.class);
                intent.putExtra("momentId", momentId);
                startActivity(intent);
            }
        });

        binding.mainCv.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                setCurMoment(position);

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        setCurMoment(0);
    }

    //-----helper-----
    private void setCurMoment(int position) {
        binding.momentContentTv.setText(PPHelper.ppFromString(momentGroupStr, "moments." + position + ".content").getAsString());
        binding.distanceTv.setText("" + PPHelper.ppFromString(momentGroupStr, "moments." + position + ".location.distance").getAsInt());
    }

    private void follow() {
        binding.followTaBt.setEnabled(false);
        PPJSONObject jBody = new PPJSONObject();
        jBody
                .put("target", PPHelper.ppFromString(momentGroupStr, "id").getAsString())
                .put("isFree", "true");

        final Observable<String> apiResult = PPRetrofit.getInstance().api("friend.follow", jBody.getJSONObject());

        disposableList.add(
                apiResult
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doFinally(new Action() {
                            @Override
                            public void run() throws Exception {
                                binding.followTaBt.setEnabled(true);
                            }
                        })
                        .subscribe(
                                new Consumer<String>() {
                                    @Override
                                    public void accept(String s) throws Exception {
                                        PPWarn ppWarn = ppWarning(s);
                                        if (ppWarn != null) {
                                            throw new Exception(ppWarn.msg);
                                        }

                                        binding.followTaBt.setText(getString(R.string.cancel_follow));
                                    }
                                },
                                new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable t) throws Exception {
                                        PPHelper.ppShowError(t.toString());
                                    }
                                })
        );
    }

    private void unFollow() {
        binding.followTaBt.setEnabled(false);
        PPJSONObject jBody = new PPJSONObject();
        jBody
                .put("target", PPHelper.ppFromString(momentGroupStr, "id").getAsString());

        final Observable<String> apiResult = PPRetrofit.getInstance().api("friend.unFollow", jBody.getJSONObject());

        disposableList.add(
                apiResult
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doFinally(new Action() {
                            @Override
                            public void run() throws Exception {
                                binding.followTaBt.setEnabled(true);
                            }
                        })
                        .subscribe(
                                new Consumer<String>() {
                                    @Override
                                    public void accept(String s) throws Exception {
                                        PPWarn ppWarn = ppWarning(s);
                                        if (ppWarn != null) {
                                            throw new Exception(ppWarn.msg);
                                        }

                                        binding.followTaBt.setText(getString(R.string.follow));
                                    }
                                },
                                new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable t) throws Exception {
                                        PPHelper.ppShowError(t.toString());
                                    }
                                })
        );

    }
}
