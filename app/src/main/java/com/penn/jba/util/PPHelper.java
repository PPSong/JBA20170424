package com.penn.jba.util;

import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.penn.jba.PPApplication;
import com.penn.jba.R;
import com.penn.jba.model.Geo;
import com.penn.jba.model.realm.CurrentUser;
import com.penn.jba.model.realm.CurrentUserSetting;
import com.penn.jba.model.realm.Pic;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.regex.Pattern;

import de.jonasrottmann.realmbrowser.RealmBrowser;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.BehaviorSubject;
import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmList;

import static android.R.attr.value;
import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * Created by penn on 02/04/2017.
 */

public class PPHelper {
    //秒
    public static final String AppName = "JBA";

    public static final int REQUEST_VERIFY_CODE_INTERVAL = 5;

    public static final String qiniuBase = "http://7xu8w0.com1.z0.glb.clouddn.com/";

    public static String currentUserId;

    public static Toast ppToast;

    public static final int MomentGridViewWidth = 192;

    public static ProgressDialog dialog;

    //pptodo remove testing block
    public static void startRealmModelsActivity() {
        try (Realm realm = Realm.getDefaultInstance()) {
            RealmConfiguration configuration = realm.getConfiguration();
            RealmBrowser.startRealmModelsActivity(PPApplication.getContext(), configuration);
        }
    }
    //pptodo end testing block

    public static String get80ImageUrl(String imageName) {
        return getImageUrl(imageName, 80);
    }

    public static String get800ImageUrl(String imageName) {
        return getImageUrl(imageName, 800);
    }

    private static String getImageUrl(String imageName, int size) {
        if (imageName.startsWith("http")) {
            return imageName;
        } else {
            String result = qiniuBase + imageName + "?imageView2/1/w/" + size + "/h/" + size + "/interlace/1/";
            return result;
        }
    }

    public static void ppShowError(String msg) {
        if (ppToast != null) {
            ppToast.cancel();
        }

        Log.v("pplog", msg);
        ppToast = Toast.makeText(PPApplication.getContext(), msg, Toast.LENGTH_LONG);
        ppToast.show();
    }

    public static void showLoading(Context context) {
        showMsg(context, context.getResources().getString(R.string.loading));
    }

    public static void showMsg(Context context, String msg) {
        dialog = ProgressDialog.show(context, "", msg, true);
        dialog.show();
    }

    public static void endLoading() {
        dialog.cancel();
    }

    public static Observable<String> signIn(final String phone, String pwd) throws Exception {
        PPJSONObject jBody = new PPJSONObject();
        jBody
                .put("phone", phone)
                .put("pwd", pwd);

        final Observable<String> apiResult = PPRetrofit.getInstance()
                .api("user.login", jBody.getJSONObject());

        return apiResult
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMap(new Function<String, ObservableSource<String>>() {
                    @Override
                    public ObservableSource<String> apply(String s) throws Exception {
                        Log.v("pplog", "1:" + s);
                        PPWarn ppWarn = ppWarning(s);
                        if (ppWarn != null) {
                            throw new Exception(ppWarn.msg);
                        }

                        initRealm(PPApplication.getContext(), phone);
                        try (Realm realm = Realm.getDefaultInstance()) {
                            realm.beginTransaction();

                            CurrentUser currentUser = new CurrentUser();
                            currentUser.setUserId(ppFromString(s, "data.userid").getAsString());
                            currentUser.setToken(ppFromString(s, "data.token").getAsString());
                            currentUser.setTokenTimestamp(ppFromString(s, "data.tokentimestamp").getAsLong());

                            realm.copyToRealmOrUpdate(currentUser);

                            CurrentUserSetting currentUserSetting = realm.where(CurrentUserSetting.class).findFirst();

                            if (currentUserSetting == null) {
                                //新注册用户或者首次在本手机使用, 默认在足迹页面不是显示我的moment
                                currentUserSetting = realm.createObject(CurrentUserSetting.class, currentUser.getUserId());
                                currentUserSetting.setFootprintMine(false);
                            }

                            realm.commitTransaction();

                            //设置PPRetrofit authBody
                            String authBody = new JSONObject()
                                    .put("userid", currentUser.getUserId())
                                    .put("token", currentUser.getToken())
                                    .put("tokentimestamp", currentUser.getTokenTimestamp())
                                    .toString();
                            PPRetrofit.authBody = authBody;
                            currentUserId = currentUser.getUserId();
                        }

                        return PPRetrofit.getInstance().api("user.startup", null);
                    }
                })
                .map(new Function<String, String>() {
                    @Override
                    public String apply(String s) throws Exception {
                        Log.v("pplog", "2:" + s);
                        PPWarn ppWarn = ppWarning(s);
                        if (ppWarn != null) {
                            throw new Exception(ppWarn.msg);
                        }

                        String imToken = ppFromString(s, "data.userInfo.params.im.token").getAsString();

                        //pptodo connect to rongyun
                        try (Realm realm = Realm.getDefaultInstance()) {
                            realm.beginTransaction();

                            CurrentUser currentUser = realm.where(CurrentUser.class)
                                    .findFirst();

                            currentUser.setPhone(ppFromString(s, "data.userInfo.phone").getAsString());
                            currentUser.setNickname(ppFromString(s, "data.userInfo.nickname").getAsString());
                            currentUser.setGender(ppFromString(s, "data.userInfo.gender").getAsInt());
                            currentUser.setBirthday(ppFromString(s, "data.userInfo.birthday").getAsLong());
                            currentUser.setHead(ppFromString(s, "data.userInfo.head").getAsString());
                            currentUser.setBaiduApiUrl(ppFromString(s, "data.settings.geo.api").getAsString());
                            currentUser.setBaiduAkBrowser(ppFromString(s, "data.settings.geo.ak_browser").getAsString());
                            currentUser.setSocketHost(ppFromString(s, "data.settings.socket.host").getAsString());
                            currentUser.setSocketPort(ppFromString(s, "data.settings.socket.port").getAsInt());
                            currentUser.setUnreadMessageMoment(ppFromString(s, "data.stats.message.moment", PPValueType.INT).getAsInt());
                            currentUser.setUnreadMessageIndex(ppFromString(s, "data.stats.message.index", PPValueType.INT).getAsInt());
                            currentUser.setUnreadMessageFriend(ppFromString(s, "data.stats.message.friend", PPValueType.INT).getAsInt());
                            currentUser.setUnreadMessageSystem(ppFromString(s, "data.stats.message.system", PPValueType.INT).getAsInt());
                            currentUser.setFollows(ppFromString(s, "data.stats.follows", PPValueType.INT).getAsInt());
                            currentUser.setNewFriend(ppFromString(s, "data.stats.newFriend", PPValueType.INT).getAsInt());
                            currentUser.setFans(ppFromString(s, "data.stats.fans", PPValueType.INT).getAsInt());
                            currentUser.setNewFans(ppFromString(s, "data.stats.newFans", PPValueType.INT).getAsInt());
                            currentUser.setImToken(imToken);
                            currentUser.setImAppKey(ppFromString(s, "data.userInfo.params.im.appKey").getAsString());

                            //pptodo get im_unread_count_int
                            RealmList<Pic> pics = currentUser.getPics();
                            JsonArray tmpArr = ppFromString(s, "data.userInfo.params.more.pics", PPValueType.ARRAY).getAsJsonArray();
                            for (int i = 0; i < tmpArr.size(); i++) {
                                Pic pic = new Pic();
                                pic.setKey("profile_pic" + i);
                                Log.v("pplog2", "test:" + tmpArr.get(i).getAsString());
                                pic.setNetFileName(tmpArr.get(i).getAsString());
                                pic.setStatus(PicStatus.NET);
                                pics.add(pic);
                            }
                            realm.commitTransaction();
                        }
                        return "OK";
                    }
                });
    }

    public static void initRealm(Context context, String phone) {
        Realm.init(context);
        RealmConfiguration config = new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .name(phone + ".realm")
                .build();
        //清除当前用户的数据文件, 测试用
        boolean clearData = true;
        if (clearData) {
            Realm.deleteRealm(config);
        }

        Realm.setDefaultConfiguration(config);
    }

    public static String isPhoneValid(Context context, String phone) {
        String error = "";
        if (TextUtils.isEmpty(phone)) {
            error = context.getString(R.string.error_field_required);
        } else if (!Pattern.matches("\\d{11}", phone)) {
            error = context.getString(R.string.error_invalid_phone);
        }

        return error;
    }

    public static String isPasswordValid(Context context, String password) {
        String error = "";
        if (TextUtils.isEmpty(password)) {
            error = context.getString(R.string.error_field_required);
        } else if (!Pattern.matches("\\w{6,12}", password.toString())) {
            error = context.getString(R.string.error_invalid_password);
        }

        return error;
    }

    public static String isNicknameValid(Context context, String nickname) {
        String error = "";
        if (TextUtils.isEmpty(nickname)) {
            error = context.getString(R.string.error_field_required);
        } else if (!Pattern.matches("\\w{3,12}", nickname.toString())) {
            error = context.getString(R.string.error_invalid_nickname);
        }

        return error;
    }

    public static String isVerifyCodeValid(Context context, String verifyCode) {
        String error = "";
        if (TextUtils.isEmpty(verifyCode)) {
            error = context.getString(R.string.error_field_required);
        } else if (!Pattern.matches("\\d{6}", verifyCode)) {
            error = context.getString(R.string.error_invalid_verify_code);
        }

        return error;
    }

    public static boolean isValidDate(String inDate) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setLenient(false);
        try {
            dateFormat.parse(inDate.trim());
        } catch (ParseException pe) {
            return false;
        }
        return true;
    }

    public static String isBirthdayValid(Context context, String birthday) {
        String error = "";
        if (TextUtils.isEmpty(birthday)) {
            error = context.getString(R.string.error_field_required);
        } else {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            dateFormat.setLenient(false);
            try {
                dateFormat.parse(birthday.trim());
            } catch (ParseException pe) {
                error = context.getString(R.string.error_invalid_birthday);
            }
        }

        return error;
    }

    public static void setLastVerifyCodeRequestTime() {
        setPrefLongValue("LastVerifyCodeRequestTime", System.currentTimeMillis() / 1000);
    }

    public static long getLastVerifyCodeRequestTime() {
        return getPrefLongValue("LastVerifyCodeRequestTime");
    }

    public static void setPrefStringValue(String key, String value) {
        PPApplication.getContext().getSharedPreferences(AppName, Context.MODE_PRIVATE).edit().putString(key, value).apply();
    }

    public static String getPrefStringValue(String key, String defaultValue) {
        return PPApplication.getContext().getSharedPreferences(AppName, Context.MODE_PRIVATE).getString(key, defaultValue);
    }

    public static void setPrefLongValue(String key, long value) {
        PPApplication.getContext().getSharedPreferences(AppName, Context.MODE_PRIVATE).edit().putLong(key, value).apply();
    }

    public static long getPrefLongValue(String key) {
        return PPApplication.getContext().getSharedPreferences(AppName, Context.MODE_PRIVATE).getLong(key, 0);
    }

    public static void setPrefBooleanValue(String key, boolean value) {
        PPApplication.getContext().getSharedPreferences(AppName, Context.MODE_PRIVATE).edit().putBoolean(key, value).apply();
    }

    public static boolean getPrefBooleanValue(String key) {
        return PPApplication.getContext().getSharedPreferences(AppName, Context.MODE_PRIVATE).getBoolean(key, false);
    }

    public static PPWarn ppWarning(String jServerResponse) {
        int code = ppFromString(jServerResponse, "code").getAsInt();
        if (code != 1) {
            return new PPWarn(jServerResponse);
        } else {
            return null;
        }
    }

    public static JsonElement ppFromString(String json, String path, PPValueType type) {
        JsonElement jsonElement = ppFromString(json, path);
        if (jsonElement == null) {
            switch (type) {
                case ARRAY:
                    return new JsonArray();
                case INT:
                    return new JsonPrimitive(0);
                case STRING:
                    return new JsonPrimitive("");
                default:
                    return null;
            }
        }

        return jsonElement;
    }

    public static JsonElement ppFromString(String json, String path) {
        try {
            JsonParser parser = new JsonParser();
            JsonElement item = parser.parse(json);
            if (path == null || path.length() == 0 || Pattern.matches("\\.+", path)) {
                //Log.v("ppLog", "解析整个json String");
                return item;
            }
            String[] seg = path.split("\\.");
            for (int i = 0; i < seg.length; i++) {
                if (i > 0) {
                    //Log.v("ppLog", "解析完毕:" + seg[i - 1]);
                    //Log.v("ppLog", "-------");
                }
                //Log.v("ppLog", "准备解析:" + seg[i]);
                if (seg[i].length() == 0) {
                    //""情况
                    //Log.v("ppLog", "解析空字符串的path片段, 停止继续解析");
                    return null;
                }
                if (item != null) {
                    //当前path片段item不为null
                    //Log.v("ppLog", "当前path片段item不为null");
                    if (item.isJsonArray()) {
                        //当前path片段item为数组
                        //Log.v("ppLog", "当前path片段item为数组");
                        String regex = "\\d+";
                        if (Pattern.matches("\\d+", seg[i])) {
                            //当前path片段描述为数组格式
                            //Log.v("ppLog", "当前path片段描述为数组格式");
                            item = item.getAsJsonArray().get(Integer.parseInt(seg[i]));
                        } else {
                            //当前path片段描述不为数组格式
                            //Log.v("ppLog", "当前path片段描述不为数组格式");
                            //Log.v("ppLog", "path中间片段描述错误:" + seg[i] + ", 停止继续解析");
                            return null;
                        }
                    } else if (item.isJsonObject()) {
                        //当前path片段item为JsonObject
                        //Log.v("ppLog", "当前path片段item为JsonObject");
                        item = item.getAsJsonObject().get(seg[i]);
                    } else {
                        //当前path片段item为JsonPrimitive
                        //Log.v("ppLog", "当前path片段item为JsonPrimitive");
                        //Log.v("ppLog", "path中间片段取值为JsonPrimitive, 停止继续解析");
                        return null;
                    }
                } else {
                    //当前path片段item为null
                    //Log.v("ppLog", "当前path片段item为null");
                    Log.v("ppLog", path + ":path中间片段取值为null, 停止继续解析");
                    return null;
                }
            }
            return item;
        } catch (Exception e) {
            Log.v("ppLog", "Json解析错误" + e);
            return null;
        }
    }

    public static String isMomentContentValid(Context context, String string) {
        String error = "";
        if (TextUtils.isEmpty(string)) {
            error = context.getString(R.string.error_field_required);
        } else if (string.length() > 120) {
            error = context.getString(R.string.error_invalid_moment_content);
        }

        return error;
    }

    public static String isPlaceValid(Context context, String string) {
        String error = "";
        if (TextUtils.isEmpty(string)) {
            error = context.getString(R.string.error_field_required);
        } else if (string.length() > 70) {
            error = context.getString(R.string.error_invalid_place);
        }

        return error;
    }

    public static Geo getLatestGeo() {
        //pptodo implement it
        return new Geo(121.52619934082031f, 31.216968536376953f);
    }
}
