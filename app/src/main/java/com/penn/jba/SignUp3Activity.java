package com.penn.jba;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.Toast;

import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxCompoundButton;
import com.jakewharton.rxbinding2.widget.RxRadioGroup;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.penn.jba.databinding.ActivitySignUp3Binding;
import com.penn.jba.util.PPHelper;
import com.penn.jba.util.PPJSONObject;
import com.penn.jba.util.PPRetrofit;
import com.penn.jba.util.PPWarn;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Function6;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.realm.Realm;

public class SignUp3Activity extends AppCompatActivity {
    private static BehaviorSubject<String> birthdayString = BehaviorSubject.create();

    private Context activityContext;

    private ActivitySignUp3Binding binding;

    private Realm realm;

    private String phone;

    private String verifyCode;

    private ArrayList<Disposable> disposableList = new ArrayList<Disposable>();

    private BehaviorSubject<Boolean> jobProcessing = BehaviorSubject.<Boolean>create();

    private Observable<String> passwordInputObservable;

    private Observable<String> nicknameInputObservable;

    private Observable<String> birthdayInputObservable;

    private Observable<String> sexInputObservable;

    private Observable<String> agreeInputObservable;

    private Observable<Object> randomNicknameButtonObservable;

    private Observable<Object> signUpButtonObservable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activityContext = this;

        phone = getIntent().getStringExtra("phone");
        verifyCode = getIntent().getStringExtra("verifyCode");

        binding = DataBindingUtil.setContentView(this, R.layout.activity_sign_up3);
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

        //密码输入监控
        passwordInputObservable = RxTextView.textChanges(binding.passwordInput)
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
                                binding.passwordInputLayout.setError(TextUtils.isEmpty(error) ? null : error);
                            }
                        }
                );

        //昵称输入监控
        nicknameInputObservable = RxTextView.textChanges(binding.nicknameInput)
                .skip(1)
                .map(
                        new Function<CharSequence, String>() {
                            @Override
                            public String apply(CharSequence charSequence) throws Exception {
                                return PPHelper.isNicknameValid(activityContext, charSequence.toString());
                            }
                        }
                )
                .doOnNext(
                        new Consumer<String>() {
                            @Override
                            public void accept(String error) throws Exception {
                                binding.nicknameInputLayout.setError(TextUtils.isEmpty(error) ? null : error);
                            }
                        }
                );

        //生日输入监控
        birthdayInputObservable = RxTextView.textChanges(binding.birthdayInput)
                .skip(1)
                .map(
                        new Function<CharSequence, String>() {
                            @Override
                            public String apply(CharSequence charSequence) throws Exception {
                                return PPHelper.isBirthdayValid(activityContext, charSequence.toString());
                            }
                        }
                )
                .doOnNext(
                        new Consumer<String>() {
                            @Override
                            public void accept(String error) throws Exception {
                                binding.birthdayInputLayout.setError(TextUtils.isEmpty(error) ? null : error);
                            }
                        }
                );

        //pptodo 改成Rxbinding
        binding.birthdayInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    binding.birthdayInput.setError(null);
                    DialogFragment newFragment = new DatePickerFragment();
                    newFragment.show(getSupportFragmentManager(), "datePicker");
                } else {
                }
            }
        });

        //性别输入监控
        sexInputObservable = RxRadioGroup.checkedChanges(binding.sexInput)
                .skip(1)
                .map(
                        new Function<Integer, String>() {

                            @Override
                            public String apply(Integer integer) throws Exception {
                                String error = "";
                                Log.v("ppLog", "RxRadioGroup:" + integer);
                                if (integer < 0) {
                                    error = getString(R.string.must_agree);
                                }
                                return error;
                            }
                        }
                )
                .doOnNext(
                        new Consumer<String>() {
                            @Override
                            public void accept(String error) throws Exception {
                                binding.sexInputLayout.setError(TextUtils.isEmpty(error) ? null : error);
                            }
                        }
                );

        //同意守则勾选监控
        agreeInputObservable = RxCompoundButton.checkedChanges(binding.agreeCheck)
                .skip(1)
                .map(
                        new Function<Boolean, String>() {

                            @Override
                            public String apply(Boolean aBoolean) throws Exception {
                                String error = "";
                                if (!aBoolean) {
                                    error = getString(R.string.must_agree);
                                }
                                return error;
                            }
                        }
                )
                .doOnNext(
                        new Consumer<String>() {
                            @Override
                            public void accept(String error) throws Exception {
                                Log.v("ppLog", "RxCompoundButton");
                                binding.agreeInputLayout.setError(TextUtils.isEmpty(error) ? null : error);
                            }
                        }
                );

        //注册按钮是否可用
        disposableList.add(
                Observable
                        .combineLatest(
                                jobProcessing,
                                passwordInputObservable,
                                nicknameInputObservable,
                                birthdayInputObservable,
                                sexInputObservable,
                                agreeInputObservable,
                                new Function6<Boolean, String, String, String, String, String, Boolean>() {

                                    @Override
                                    public Boolean apply(Boolean aBoolean, String s, String s2, String s3, String s4, String s5) throws Exception {
                                        return !aBoolean && TextUtils.isEmpty(s) && TextUtils.isEmpty(s2) && TextUtils.isEmpty(s3) && TextUtils.isEmpty(s4) && TextUtils.isEmpty(s5);
                                    }
                                }
                        )
                        .subscribeOn(AndroidSchedulers.mainThread())
                        .distinctUntilChanged()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                new Consumer<Boolean>() {
                                    @Override
                                    public void accept(Boolean aBoolean) throws Exception {
                                        binding.signUpButton.setEnabled(aBoolean);
                                    }
                                }
                        )
        );

        //获取随机昵称按钮监控
        randomNicknameButtonObservable = RxView.clicks(binding.getRandomNicknameButton)
                .debounce(200, TimeUnit.MILLISECONDS);

        disposableList.add(randomNicknameButtonObservable
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.io())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                requestRandomNickname();
                            }
                        }
                )
        );

        //注册按钮监控
        signUpButtonObservable = RxView.clicks(binding.signUpButton)
                .debounce(200, TimeUnit.MILLISECONDS);

        disposableList.add(signUpButtonObservable
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.io())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                signUp();
                            }
                        }
                )
        );

        //进度条是否可见, 随机生成昵称按钮是否可用
        disposableList.add(jobProcessing
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Boolean>() {
                            @Override
                            public void accept(Boolean aBoolean) throws Exception {
                                binding.jobProgress.setVisibility(aBoolean ? View.VISIBLE : View.INVISIBLE);
                                binding.getRandomNicknameButton.setEnabled(!aBoolean);
                            }
                        }
                )
        );

        //选择生日对话框
        disposableList.add(birthdayString
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String>() {
                            @Override
                            public void accept(String s) throws Exception {
                                binding.birthdayInput.setText(s);
                            }
                        }
                )
        );
    }

    private void requestRandomNickname() {
        jobProcessing.onNext(true);
        PPJSONObject jBody = new PPJSONObject();

        final Observable<String> apiResult = PPRetrofit.getInstance().api("user.randomNickName", jBody.getJSONObject());
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

                                String nickname = PPHelper.ppFromString(s, "data.nickname").getAsString();
                                binding.nicknameInput.setText(nickname);
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

    private void signUp() {
        jobProcessing.onNext(true);

        final String tmpPhone = phone;
        final String tmpPassword = binding.passwordInput.getText().toString();

        PPJSONObject jBody = new PPJSONObject();

        int sex = binding.sexInput.getCheckedRadioButtonId() == R.id.male_radio ? 1 : 2;

        jBody
                .put("phone", tmpPhone)
                .put("pwd", tmpPassword)
                .put("gender", sex)
                .put("checkCode", verifyCode)
                .put("nickname", binding.nicknameInput.getText().toString())
                .put("birthday", binding.birthdayInput.getText().toString());

        final Observable<String> apiResult = PPRetrofit.getInstance().api("user.register", jBody.getJSONObject());
        apiResult
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMap(
                        new Function<String, Observable<String>>() {
                            @Override
                            public Observable<String> apply(String s) throws Exception {
                                Log.v("ppLog", "pp test:" + s);

                                //如果有错误通过throw new Exception传递到subscribe的onError
                                PPWarn ppWarn = PPHelper.ppWarning(s);
                                if (ppWarn != null) {
                                    throw new Exception(ppWarn.msg);
                                }

                                PPJSONObject jBody = new PPJSONObject();

                                jBody
                                        .put("phone", tmpPhone)
                                        .put("pwd", tmpPassword);

                                final Observable<String> apiResult = PPRetrofit.getInstance().api("user.login", jBody.getJSONObject());
                                return apiResult;
                            }
                        }
                )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String>() {
                            public void accept(String s) {
                                Log.v("ppLog", "get result:" + s);
                                jobProcessing.onNext(false);

                                PPWarn ppWarn = PPHelper.ppWarning(s);
                                if (ppWarn != null) {
                                    Toast.makeText(activityContext, ppWarn.msg, Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                loginOK(s);
                            }
                        },
                        new Consumer<Throwable>() {
                            public void accept(Throwable t1) {
                                jobProcessing.onNext(false);

                                Toast.makeText(activityContext, t1.getMessage(), Toast.LENGTH_SHORT).show();
                                Log.v("ppLog", "pp error:" + t1.toString());
                                t1.printStackTrace();
                            }
                        }
                );

    }

    private void loginOK(String result) {
        Intent intent = new Intent(activityContext, LoginActivity.class);
        intent.putExtra("signInResult", result);
        startActivity(intent);
    }

    public static class DatePickerFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Use the current date - 18years as the default date in the picker
            final Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR) - 18;
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            // Create a new instance of DatePickerDialog and return it
            DatePickerDialog dialogDatePicker = new DatePickerDialog(getActivity(), R.style.CustomDatePickerDialogTheme, this, year, month, day);
            return dialogDatePicker;
        }

        @Override
        public void onDateSet(DatePicker view, int year, int month, int day) {
            // Do something with the date chosen by the user
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, year);
            cal.set(Calendar.DAY_OF_MONTH, day);
            cal.set(Calendar.MONTH, month);
            String dateString = new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
            birthdayString.onNext(dateString);
        }
    }
}
