package com.penn.jba;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;
import android.widget.Toast;

import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.penn.jba.databinding.ActivityForgetPasswordBinding;
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
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Function3;
import io.reactivex.functions.Function4;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;

public class ForgetPasswordActivity extends AppCompatActivity {
    private Context activityContext;

    private ActivityForgetPasswordBinding binding;

    private ArrayList<Disposable> disposableList = new ArrayList<Disposable>();

    private BehaviorSubject<Boolean> jobProcessing = BehaviorSubject.<Boolean>create();

    private BehaviorSubject<Boolean> timeLeftProcessing = BehaviorSubject.<Boolean>create();

    private Observable<Long> timeLeftObservable;

    private Observable<String> phoneInputObservable;

    private Observable<String> verifyCodeInputObservable;

    private Observable<String> passwordInputObservable;

    private Observable<Object> requestVerifyCodeButtonObservable;

    private Observable<Object> resetPasswordButtonObservable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activityContext = this;

        binding = DataBindingUtil.setContentView(this, R.layout.activity_forget_password);
        binding.setPresenter(this);

        //设置键盘返回键的快捷方式
        binding.newPasswordInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.reset_password || id == EditorInfo.IME_NULL) {
                    resetPassword();
                    return true;
                }
                return false;
            }
        });

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

    //-----helper-----
    private void setup() {
        //先发送个初始事件,便于判断按钮是否可用
        jobProcessing.onNext(false);
        timeLeftProcessing.onNext(false);

        //手机号码输入监控
        phoneInputObservable = RxTextView.textChanges(binding.phoneInput)
                .skip(1)
                .map(
                        new Function<CharSequence, String>() {
                            @Override
                            public String apply(CharSequence charSequence) throws Exception {
                                return PPHelper.isPhoneValid(activityContext, charSequence.toString());
                            }
                        }
                )
                .doOnNext(
                        new Consumer<String>() {
                            @Override
                            public void accept(String error) throws Exception {
                                binding.phoneInputLayout.setError(TextUtils.isEmpty(error) ? null : error);
                            }
                        }
                );

        //获取验证码按钮是否可用
        disposableList.add(
                Observable
                        .combineLatest(
                                phoneInputObservable,
                                jobProcessing,
                                timeLeftProcessing,
                                new Function3<String, Boolean, Boolean, Boolean>() {
                                    @Override
                                    public Boolean apply(String s, Boolean aBoolean, Boolean bBoolean) throws Exception {
                                        return TextUtils.isEmpty(s) && !aBoolean && !bBoolean;
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

        //密码输入监控
        passwordInputObservable = RxTextView.textChanges(binding.newPasswordInput)
                .skip(1)
                .map(
                        new Function<CharSequence, String>() {
                            @Override
                            public String apply(CharSequence charSequence) throws Exception {
                                return PPHelper.isPasswordValid(activityContext, charSequence.toString());
                            }
                        }
                )
                .doOnNext(
                        new Consumer<String>() {
                            @Override
                            public void accept(String error) throws Exception {
                                binding.newPasswordInputLayout.setError(TextUtils.isEmpty(error) ? null : error);
                            }
                        }
                );

        //重置密码按钮是否可用
        disposableList.add(
                Observable
                        .combineLatest(
                                phoneInputObservable,
                                verifyCodeInputObservable,
                                passwordInputObservable,
                                jobProcessing,
                                new Function4<String, String, String, Boolean, Boolean>() {
                                    @Override
                                    public Boolean apply(String s, String s2, String s3, Boolean aBoolean) throws Exception {
                                        return TextUtils.isEmpty(s) && TextUtils.isEmpty(s2) && TextUtils.isEmpty(s3) && !aBoolean;
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
                                        binding.resetPasswordButton.setEnabled(aBoolean);
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
                                Log.v("ppLog", "doOnNext");
                            }
                        }
                )
                .takeWhile(
                        new Predicate<Long>() {
                            @Override
                            public boolean test(Long aLong) throws Exception {
                                Log.v("ppLog", "takeWhile");
                                boolean b = (System.currentTimeMillis() / 1000) - PPHelper.getLastVerifyCodeRequestTime() <= PPHelper.REQUEST_VERIFY_CODE_INTERVAL;
                                return b;
                            }
                        }
                )
                .map(
                        new Function<Long, Long>() {
                            @Override
                            public Long apply(Long aLong) throws Exception {
                                Log.v("ppLog", "map");
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

        //重设密码按钮监控
        resetPasswordButtonObservable = RxView.clicks(binding.resetPasswordButton)
                .debounce(200, TimeUnit.MILLISECONDS);

        disposableList.add(resetPasswordButtonObservable
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.io())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                resetPassword();
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
                                Log.v("ppLog", "long:" + aLong);
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
                                Log.v("ppLog", "finished");
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
                .put("phone", binding.phoneInput.getText().toString());

        final Observable<String> apiResult = PPRetrofit.getInstance().api("user.sendForgotPassCheckCode", jBody.getJSONObject());
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

    private void resetPassword() {
        jobProcessing.onNext(true);
        PPJSONObject jBody = new PPJSONObject();
        jBody
                .put("phone", binding.phoneInput.getText().toString())
                .put("checkCode", binding.verifyCodeInput.getText().toString())
                .put("pwd", binding.newPasswordInput.getText().toString());

        final Observable<String> apiResult = PPRetrofit.getInstance().api("user.changePassByCheckCode", jBody.getJSONObject());
        apiResult
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String>() {
                            public void accept(String s) {
                                jobProcessing.onNext(false);

                                PPWarn ppWarn = PPHelper.ppWarning(s);
                                if (ppWarn != null) {
                                    PPHelper.showPPToast(activityContext, ppWarn.msg, Toast.LENGTH_SHORT);

                                    return;
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
