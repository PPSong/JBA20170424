package com.penn.jba;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jakewharton.rxbinding2.view.RxView;
import com.penn.jba.databinding.ActivityLoginBinding;
import com.penn.jba.databinding.ActivityMomentDetailBinding;
import com.penn.jba.footprint.FootprintAdapter;
import com.penn.jba.util.CommentListAdapter;
import com.penn.jba.util.PPHelper;
import com.penn.jba.util.PPJSONObject;
import com.penn.jba.util.PPRetrofit;
import com.penn.jba.util.PPValueType;
import com.penn.jba.util.PPWarn;
import com.squareup.picasso.Picasso;
import com.synnapps.carouselview.ImageListener;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function3;
import io.reactivex.schedulers.Schedulers;

import static com.penn.jba.util.PPHelper.ppFromString;
import static com.penn.jba.util.PPHelper.ppWarning;

public class MomentDetailActivity extends AppCompatActivity {
    private static final int pageSize = 10;

    private Context activityContext;

    private ActivityMomentDetailBinding binding;

    private ArrayList<Disposable> disposableList = new ArrayList<Disposable>();

    //custom
    private String momentId;
    private String momentStr;
    private String commentsStr;
    private long oldestTimeStamp = System.currentTimeMillis();

    private CommentListAdapter commentListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //common
        activityContext = this;
        binding = DataBindingUtil.setContentView(this, R.layout.activity_moment_detail);
        //end common

        momentId = getIntent().getStringExtra("momentId");

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (Disposable d : disposableList) {
            if (!d.isDisposed()) {
                d.dispose();
            }
        }
    }

    private void setup() {
        getMomentDetailAndComment();
    }

    private void loadMoreComment() {
        binding.loadMoreBt.setEnabled(false);

        PPJSONObject jBody = new PPJSONObject();

        jBody
                .put("id", momentId)
                .put("beforeTime", oldestTimeStamp);

        final Observable<String> apiResult = PPRetrofit.getInstance()
                .api("moment.getReplies", jBody.getJSONObject());

        disposableList.add(
                apiResult
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doFinally(new Action() {
                            @Override
                            public void run() throws Exception {
                                if (binding.loadMoreBt.getText() != getString(R.string.no_more_data)) {
                                    binding.loadMoreBt.setEnabled(true);
                                }
                            }
                        })
                        .subscribe(
                                new Consumer<String>() {
                                    @Override
                                    public void accept(String s) throws Exception {
                                        Log.v("pplog142", s);
                                        PPWarn ppWarn1 = ppWarning(s);
                                        if (ppWarn1 != null) {
                                            throw new Exception(ppWarn1.msg);
                                        }

                                        JsonArray comments = ppFromString(s, "data.list", PPValueType.ARRAY).getAsJsonArray();

                                        int size = comments.size();

                                        if (size > 0) {
                                            Log.v("pplog142", "oldestTimeStamp before:" + oldestTimeStamp);
                                            oldestTimeStamp = ppFromString(s, "data.list." + (size - 1) + ".createTime").getAsLong();
                                            Log.v("pplog142", "oldestTimeStamp after:" + oldestTimeStamp);
                                        }

                                        if (size < pageSize) {
                                            setNoMore();
                                        }

                                        int lastCount = commentListAdapter.getItemCount();

                                        commentListAdapter.loadMore(comments);

                                        if (comments.size() > 0) {
                                            binding.mainRv.scrollToPosition(lastCount);
                                        }
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

    private void sendComment() {
        //检查内容合法性
        String content = binding.replyEt.getText().toString();
        if (TextUtils.isEmpty(content) == true) {
            PPHelper.ppShowError(getString(R.string.content_can_not_be_empty));
            return;
        }

        //清空输入框内容
        binding.replyEt.setText("");
        binding.privateCb.setChecked(false);

        binding.sendBt.setEnabled(false);

        boolean isPrivate = binding.privateCb.isChecked();

        //延迟补偿
        JsonObject tmpReply = new JsonObject();
        tmpReply.addProperty("content", content);
        tmpReply.addProperty("isPrivate", isPrivate ? 1 : 0);
        tmpReply.addProperty("createTime", System.currentTimeMillis());

        JsonObject tmpCreator = new JsonObject();
        tmpCreator.addProperty("nickname", PPHelper.currentUserNickname);
        tmpCreator.addProperty("head", PPHelper.getCurrentUserHead);

        tmpReply.add("_creator", tmpCreator);

        Log.v("pplog141", tmpReply.toString());
        commentListAdapter.fake(tmpReply);
        binding.mainSv.scrollTo(0, 0);
        binding.mainRv.scrollToPosition(0);
        //end 延迟补偿

        PPJSONObject jBody = new PPJSONObject();

        jBody
                .put("id", momentId)
                .put("content", content)
                .put("isPrivate", "" + isPrivate);

        final Observable<String> apiResult = PPRetrofit.getInstance()
                .api("moment.reply", jBody.getJSONObject());

        disposableList.add(
                apiResult
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doFinally(new Action() {
                            @Override
                            public void run() throws Exception {
                                binding.sendBt.setEnabled(true);
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
                                    }
                                },
                                new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable t) throws Exception {
                                        Log.v("pplog", "error:" + t);
                                        PPHelper.ppShowError(t.toString());
                                        //清除延迟补偿数据
                                        commentListAdapter.removeFirstItem();
                                        //end 清除延迟补偿数据
                                    }
                                })
        );
    }

    private void setNoMore() {
        binding.loadMoreBt.setText(getString(R.string.no_more_data));
        binding.loadMoreBt.setEnabled(false);
    }

    private void getMomentDetailAndComment() {
        PPJSONObject jBody1 = new PPJSONObject();
        jBody1
                .put("id", momentId)
                .put("checkFollow", 1)
                .put("checkLike", 1);

        final Observable<String> apiResult1 = PPRetrofit.getInstance()
                .api("moment.detail", jBody1.getJSONObject());

        PPJSONObject jBody2 = new PPJSONObject();
        jBody2
                .put("id", momentId)
                .put("beforeTime", "");

        final Observable<String> apiResult2 = PPRetrofit.getInstance()
                .api("moment.getReplies", jBody2.getJSONObject());

        disposableList.add(
                Observable
                        .zip(
                                apiResult1,
                                apiResult2,
                                new BiFunction<String, String, String>() {
                                    @Override
                                    public String apply(String s, String s2) throws Exception {
                                        PPWarn ppWarn1 = ppWarning(s);
                                        if (ppWarn1 != null) {
                                            throw new Exception(ppWarn1.msg);
                                        }

                                        momentStr = s;
                                        Log.v("pplog140", "s:" + s);

                                        PPWarn ppWarn2 = ppWarning(s2);
                                        if (ppWarn2 != null) {
                                            throw new Exception(ppWarn2.msg);
                                        }

                                        commentsStr = s2;
                                        Log.v("pplog140", "s2:" + s2);

                                        return "OK";
                                    }
                                }
                        )
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doFinally(new Action() {
                            @Override
                            public void run() throws Exception {
                                binding.pb.setVisibility(View.INVISIBLE);
                                binding.loadMoreBt.setVisibility(View.VISIBLE);
                            }
                        })
                        .subscribe(
                                new Consumer<String>() {
                                    @Override
                                    public void accept(String s) throws Exception {
                                        loadContent();
                                        Log.v("pplog140", s);
                                    }
                                },
                                new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable t) throws Exception {
                                        Log.v("pplog140", t.toString());
                                        PPHelper.ppShowError(t.toString());
                                    }
                                })
        );
    }

    private void loadContent() {
        Picasso.with(activityContext)
                .load(PPHelper.get80ImageUrl(ppFromString(momentStr, "data._creator.head").getAsString()))
                .placeholder(R.drawable.pictures_no)
                .into(binding.avatarCiv);

        binding.line1Tv.setText(ppFromString(momentStr, "data._creator.nickname").getAsString());
        binding.line2Tv.setText(ppFromString(momentStr, "data.location.geo").getAsJsonArray().toString());
        binding.contentTv.setText(ppFromString(momentStr, "data.content").getAsString());
        binding.createTimeRttv.setReferenceTime(ppFromString(momentStr, "data.createTime").getAsLong());
        binding.placeTv.setText(ppFromString(momentStr, "data.location.detail").getAsString());

        final JsonArray pics = ppFromString(momentStr, "data.pics").getAsJsonArray();

        binding.mainCv.setImageListener(new ImageListener() {
            @Override
            public void setImageForPosition(int position, ImageView imageView) {
                Picasso.with(activityContext)
                        .load(PPHelper.get800ImageUrl(pics.get(position).getAsString()))
                        .placeholder(R.drawable.header)
                        .into(imageView);
            }
        });

        binding.mainCv.setPageCount(pics.size());

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(activityContext);
        binding.mainRv.setLayoutManager(linearLayoutManager);

        JsonArray comments = ppFromString(commentsStr, "data.list", PPValueType.ARRAY).getAsJsonArray();
        commentListAdapter = new CommentListAdapter(activityContext, comments);
        int size = comments.size();
        if (size > 0) {
            oldestTimeStamp = ppFromString(commentsStr, "data.list." + (size - 1) + ".createTime").getAsLong();
        }

        if (size < pageSize) {
            setNoMore();
        }

        binding.mainRv.setAdapter(commentListAdapter);

        binding.mainRv.setHasFixedSize(true);

        //loadMore按钮监控
        Observable<Object> loadMoreButtonObservable = RxView.clicks(binding.loadMoreBt)
                .debounce(200, TimeUnit.MILLISECONDS);

        disposableList.add(loadMoreButtonObservable
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                loadMoreComment();
                            }
                        }
                )
        );

        //发送按钮监控
        Observable<Object> sendButtonObservable = RxView.clicks(binding.sendBt)
                .debounce(200, TimeUnit.MILLISECONDS);

        disposableList.add(sendButtonObservable
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                sendComment();
                            }
                        }
                )
        );
    }
}
