package com.penn.jba;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.databinding.DataBindingUtil;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.penn.jba.databinding.ActivityLoginBinding;
import com.penn.jba.util.PPHelper;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class LoginActivity extends AppCompatActivity {
    private Context activityContext;

    private ActivityLoginBinding binding;

    private ArrayList<Disposable> disposableList = new ArrayList<Disposable>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //common
        activityContext = this;
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login);
        binding.setPresenter(this);
        //end common

        //设置键盘返回键的快捷方式
        binding.passwordEt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.sign_in_ime || id == EditorInfo.IME_NULL) {
                    signIn();
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

    public void setup() {
        //登录按钮监控
        Observable<Object> signInButtonObservable = RxView.clicks(binding.signInBt)
                .debounce(200, TimeUnit.MILLISECONDS);

        disposableList.add(signInButtonObservable
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                signIn();
                            }
                        }
                )
        );

        //手机号码输入监控
        Observable<String> phoneInputObservable = RxTextView.textChanges(binding.phoneEt)
                .skip(1)
                .map(new Function<CharSequence, String>() {
                    @Override
                    public String apply(CharSequence charSequence) throws Exception {
                        return PPHelper.isPhoneValid(activityContext, charSequence.toString());
                    }
                }).doOnNext(
                        new Consumer<String>() {
                            @Override
                            public void accept(String error) throws Exception {
                                binding.phoneTil.setError(TextUtils.isEmpty(error) ? null : error);
                            }
                        }
                );

        //密码输入监控
        Observable<String> passwordInputObservable = RxTextView.textChanges(binding.passwordEt)
                .skip(1)
                .map(new Function<CharSequence, String>() {
                    @Override
                    public String apply(CharSequence charSequence) throws Exception {
                        return PPHelper.isPasswordValid(activityContext, charSequence.toString());
                    }
                }).doOnNext(
                        new Consumer<String>() {
                            @Override
                            public void accept(String error) throws Exception {
                                binding.passwordTil.setError(TextUtils.isEmpty(error) ? null : error);
                            }
                        }
                );

        //登录按钮是否可用
        disposableList.add(Observable.combineLatest(
                phoneInputObservable,
                passwordInputObservable,
                new BiFunction<String, String, Boolean>() {
                    @Override
                    public Boolean apply(String s1, String s2) throws Exception {
                        return TextUtils.isEmpty(s1) && TextUtils.isEmpty(s2);
                    }
                })
                .subscribeOn(Schedulers.io())
                .distinctUntilChanged()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        binding.signInBt.setEnabled(aBoolean);
                    }
                })
        );

        if (PPHelper.getPrefBooleanValue("autoLogin")) {
            autoLogin();
        }

        //忘记密码按钮监控
        Observable<Object> forgetPasswordButtonObservable = RxView.clicks(binding.forgetPasswordBt)
                .debounce(200, TimeUnit.MILLISECONDS);

        disposableList.add(forgetPasswordButtonObservable
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.io())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                goForgetPassword();
                            }
                        }
                )
        );

        //注册按钮监控
        Observable<Object> createNewAccountButtonObservable = RxView.clicks(binding.createNewAccountBt)
                .debounce(200, TimeUnit.MILLISECONDS);

        disposableList.add(createNewAccountButtonObservable
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.io())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                goSignUp();
                            }
                        }
                )
        );
    }

    //-----help-----
    private void autoLogin() {
        binding.phoneEt.setText(PPHelper.getPrefStringValue("phone", ""));
        binding.passwordEt.setText(PPHelper.getPrefStringValue("pwd", ""));
        signIn();
    }


    private void signIn() {
        PPHelper.showLoading(activityContext);
        try {
            final String phone = binding.phoneEt.getText().toString();
            final String pwd = binding.passwordEt.getText().toString();
            PPHelper.signIn(phone, pwd)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<String>() {
                        @Override
                        public void accept(String s) throws Exception {
                            PPHelper.setPrefBooleanValue("autoLogin", true);
                            PPHelper.setPrefStringValue("phone", phone);
                            PPHelper.setPrefStringValue("pwd", pwd);

                            Intent intent = new Intent(activityContext, TabsActivity.class);
                            startActivity(intent);

                            PPHelper.endLoading();
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            PPHelper.setPrefBooleanValue("autoLogin", false);
                            PPHelper.ppShowError(throwable.toString());
                            PPHelper.endLoading();
                        }
                    });
        } catch (Exception e) {
            PPHelper.ppShowError(e.toString());
            e.printStackTrace();
        }
    }

    private void goForgetPassword() {
        Intent intent = new Intent(this, ForgetPasswordActivity.class);
        startActivity(intent);
    }

    private void goSignUp() {
        Intent intent = new Intent(this, SignUp1Activity.class);
        startActivity(intent);
    }
}
