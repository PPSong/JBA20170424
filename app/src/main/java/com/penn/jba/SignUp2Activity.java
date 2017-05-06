package com.penn.jba;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.penn.jba.databinding.ActivitySignUp2Binding;
import com.penn.jba.util.PPHelper;
import com.penn.jba.util.PPJSONObject;
import com.penn.jba.util.PPRetrofit;
import com.penn.jba.util.PPWarn;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;

public class SignUp2Activity extends AppCompatActivity {
    private Context activityContext;

    private ActivitySignUp2Binding binding;

    private String phone;

    private ArrayList<Disposable> disposableList = new ArrayList<Disposable>();

    private BehaviorSubject<Boolean> jobProcessing = BehaviorSubject.<Boolean>create();

    private BehaviorSubject<Boolean> timeLeftProcessing = BehaviorSubject.<Boolean>create();

    private Observable<Long> timeLeftObservable;

    private Observable<String> verifyCodeInputObservable;

    private Observable<Object> requestVerifyCodeButtonObservable;

    private Observable<Object> nextButtonObservable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activityContext = this;

        phone = getIntent().getStringExtra("phone");

        binding = DataBindingUtil.setContentView(this, R.layout.activity_sign_up2);
        binding.setPresenter(this);

        setup();
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

    //-----help-----
    private void setup() {
        //先发送个初始事件,便于判断按钮是否可用
        jobProcessing.onNext(false);
        timeLeftProcessing.onNext(false);

        //验证码输入监控
        verifyCodeInputObservable = RxTextView.textChanges(binding.verifyCodeInput)
                .skip(1)
                .map(
                        new Function<CharSequence, String>() {
                            @Override
                            public String apply(CharSequence charSequence) throws Exception {
                                return PPHelper.isVerifyCodeValid(activityContext, charSequence.toString());
                            }
                        }
                )
                .doOnNext(
                        new Consumer<String>() {
                            @Override
                            public void accept(String error) throws Exception {
                                binding.verifyCodeInputLayout.setError(TextUtils.isEmpty(error) ? null : error);
                            }
                        }
                );

        //获取验证码按钮是否可用
        disposableList.add(
                Observable
                        .combineLatest(
                                jobProcessing,
                                timeLeftProcessing,
                                new BiFunction<Boolean, Boolean, Boolean>() {
                                    @Override
                                    public Boolean apply(Boolean aBoolean, Boolean bBoolean) throws Exception {
                                        return !aBoolean && !bBoolean;
                                    }
                                }
                        )
                        .subscribeOn(Schedulers.io())
                        .distinctUntilChanged()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                new Consumer<Boolean>() {
                                    @Override
                                    public void accept(Boolean aBoolean) throws Exception {
                                        binding.requestVerifyCodeButton.setEnabled(aBoolean);
                                    }
                                }
                        )
        );

        //下一步按钮是否可用
        disposableList.add(
                Observable
                        .combineLatest(
                                verifyCodeInputObservable,
                                jobProcessing,
                                new BiFunction<String, Boolean, Boolean>() {
                                    @Override
                                    public Boolean apply(String s, Boolean aBoolean) throws Exception {
                                        return TextUtils.isEmpty(s) && !aBoolean;
                                    }
                                }
                        )
                        .subscribeOn(Schedulers.io())
                        .distinctUntilChanged()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                new Consumer<Boolean>() {
                                    @Override
                                    public void accept(Boolean aBoolean) throws Exception {
                                        binding.nextButton.setEnabled(aBoolean);
                                    }
                                }
                        )
        );

        //控制获取验证码倒计时
        timeLeftObservable = Observable.interval(1, TimeUnit.SECONDS, Schedulers.io())
                .doOnNext(
                        new Consumer<Long>() {
                            @Override
                            public void accept(Long aLong) throws Exception {
                                //Log.v("ppLog", "doOnNext");
                            }
                        }
                )
                .takeWhile(
                        new Predicate<Long>() {
                            @Override
                            public boolean test(Long aLong) throws Exception {
                                //Log.v("ppLog", "takeWhile");
                                boolean b = (System.currentTimeMillis() / 1000) - PPHelper.getLastVerifyCodeRequestTime() <= PPHelper.REQUEST_VERIFY_CODE_INTERVAL;
                                return b;
                            }
                        }
                )
                .map(
                        new Function<Long, Long>() {
                            @Override
                            public Long apply(Long aLong) throws Exception {
                                //Log.v("ppLog", "map");
                                return PPHelper.REQUEST_VERIFY_CODE_INTERVAL - ((System.currentTimeMillis() / 1000) - PPHelper.getLastVerifyCodeRequestTime());
                            }
                        }
                );

        //获取验证码密码按钮监控
        requestVerifyCodeButtonObservable = RxView.clicks(binding.requestVerifyCodeButton)
                .debounce(200, TimeUnit.MILLISECONDS);

        disposableList.add(requestVerifyCodeButtonObservable
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.io())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                requestVerifyCode();
                            }
                        }
                )
        );

        //下一步按钮监控
        nextButtonObservable = RxView.clicks(binding.nextButton)
                .debounce(200, TimeUnit.MILLISECONDS);

        disposableList.add(nextButtonObservable
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.io())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                checkRegisterCheckCode();
                            }
                        }
                )
        );

        //进度条是否可见
        disposableList.add(jobProcessing
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Boolean>() {
                            @Override
                            public void accept(Boolean aBoolean) throws Exception {
                                binding.jobProgress.setVisibility(aBoolean ? View.VISIBLE : View.INVISIBLE);
                            }
                        }
                )
        );

        setRequestVerifyCodeButtonText();
        startTimeLeft();
    }

    private void setRequestVerifyCodeButtonText() {
        long timeLeft = PPHelper.REQUEST_VERIFY_CODE_INTERVAL - ((System.currentTimeMillis() / 1000) - PPHelper.getLastVerifyCodeRequestTime());
        if (timeLeft >= 0) {
            binding.requestVerifyCodeButton.setText("" + timeLeft);
        }
    }

    private void startTimeLeft() {
        timeLeftProcessing.onNext(true);
        disposableList.add(timeLeftObservable
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Long>() {
                            @Override
                            public void accept(Long aLong) throws Exception {
                                binding.requestVerifyCodeButton.setText("" + aLong);
                            }
                        },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(Throwable throwable) throws Exception {

                            }
                        },
                        new Action() {
                            @Override
                            public void run() throws Exception {
                                timeLeftProcessing.onNext(false);
                                binding.requestVerifyCodeButton.setText(getString(R.string.request_verify_code));
                            }
                        }
                )
        );
    }

    private void requestVerifyCode() {
        jobProcessing.onNext(true);
        PPJSONObject jBody = new PPJSONObject();
        jBody
                .put("phone", phone);

        final Observable<String> apiResult = PPRetrofit.getInstance().api("user.sendRegisterCheckCode", jBody.getJSONObject());
        apiResult
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String>() {
                            public void accept(String s) {
                                Log.v("ppLog", "get result:" + s);
                                startTimeLeft();
                                jobProcessing.onNext(false);

                                PPWarn ppWarn = PPHelper.ppWarning(s);
                                if (ppWarn != null) {
                                    PPHelper.showPPToast(activityContext, ppWarn.msg, Toast.LENGTH_SHORT);

                                    return;
                                }

                                PPHelper.setLastVerifyCodeRequestTime();
                            }
                        },
                        new Consumer<Throwable>() {
                            public void accept(Throwable t1) {
                                jobProcessing.onNext(false);

                                PPHelper.showPPToast(activityContext, t1.getMessage(), Toast.LENGTH_SHORT);
                                t1.printStackTrace();
                            }
                        }
                );
    }

    private void checkRegisterCheckCode() {
        jobProcessing.onNext(true);
        PPJSONObject jBody = new PPJSONObject();
        jBody
                .put("checkCode", binding.verifyCodeInput.getText().toString())
                .put("phone", phone);

        final Observable<String> apiResult = PPRetrofit.getInstance().api("user.checkRegisterCheckCode", jBody.getJSONObject());
        apiResult
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String>() {
                            public void accept(String s) {
                                Log.v("ppLog", "get result:" + s);
                                jobProcessing.onNext(false);

                                PPWarn ppWarn = PPHelper.ppWarning(s);
                                if (ppWarn != null) {
                                    PPHelper.showPPToast(activityContext, ppWarn.msg, Toast.LENGTH_SHORT);

                                    return;
                                }

                                if (PPHelper.ppFromString(s, "data.flag").getAsInt() == 1) {
                                    Intent intent = new Intent(activityContext, SignUp3Activity.class);
                                    intent.putExtra("phone", phone);
                                    intent.putExtra("verifyCode", binding.verifyCodeInput.getText().toString());
                                    startActivity(intent);
                                } else {
                                    PPHelper.showPPToast(activityContext, getString(R.string.error_verify_code), Toast.LENGTH_SHORT);
                                }
                            }
                        },
                        new Consumer<Throwable>() {
                            public void accept(Throwable t1) {
                                jobProcessing.onNext(false);

                                PPHelper.showPPToast(activityContext, t1.getMessage(), Toast.LENGTH_SHORT);
                                t1.printStackTrace();
                            }
                        }
                );
    }
}
