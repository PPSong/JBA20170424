package com.penn.jba;

import android.app.Activity;
import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;
import com.penn.jba.databinding.ActivityCollectDetailBinding;
import com.penn.jba.model.CollectMoment;
import com.penn.jba.util.CollectMomentAdapter;
import com.penn.jba.util.PPHelper;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import it.moondroid.coverflow.components.ui.containers.FeatureCoverFlow;

public class CollectDetailActivity extends AppCompatActivity {
    private Context activityContext;

    private ActivityCollectDetailBinding binding;

    //custom
    private String avatarStr;
    private String nickname;
    private String content;
    private JsonArray geoJsonArray;

    private CollectMomentAdapter adapter;
    private String collectMomentsStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //common
        activityContext = this;
        binding = DataBindingUtil.setContentView(this, R.layout.activity_collect_detail);
        //end common

        avatarStr = getIntent().getStringExtra("avatarStr");
        nickname = getIntent().getStringExtra("nickname");
        content = getIntent().getStringExtra("content");
        geoJsonArray = new Gson().fromJson(getIntent().getStringExtra("geoStr"), JsonArray.class);

        collectMomentsStr = getIntent().getStringExtra("collectMomentsStr");

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setup();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void setup() {
        Picasso.with(activityContext)
                .load(PPHelper.get80ImageUrl(avatarStr))
                .placeholder(R.drawable.pictures_no)
                .into(binding.avatarCiv);

        binding.nicknameTv.setText(nickname);

        binding.contentTv.setText(content);

        Picasso.with(activityContext)
                .load(PPHelper.getBaiduMap(geoJsonArray))
                .placeholder(R.drawable.pictures_no)
                .into(binding.mapIv);

        ArrayList<CollectMoment> collectMoments = new Gson().fromJson(
                collectMomentsStr
                ,
                new TypeToken<ArrayList<CollectMoment>>() {
                }.getType()
        );
        adapter = new CollectMomentAdapter(this, collectMoments);
        Log.v("pplog140", "size:" + collectMoments.size());
        binding.mainFcf.setAdapter(adapter);
        binding.mainFcf.setOnScrollPositionListener(onScrollListener());
    }

    private FeatureCoverFlow.OnScrollPositionListener onScrollListener() {
        return new FeatureCoverFlow.OnScrollPositionListener() {
            @Override
            public void onScrolledToPosition(int position) {
                Log.v("CollectDetailActivity", "position: " + position);
            }

            @Override
            public void onScrolling() {
                Log.i("CollectDetailActivity", "scrolling");
            }
        };
    }
}
