package com.penn.jba;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jakewharton.rxbinding2.view.RxView;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.holder.BadgeStyle;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader;
import com.mikepenz.materialdrawer.util.DrawerImageLoader;
import com.penn.jba.databinding.ActivityTabsBinding;
import com.penn.jba.databinding.PpTabBinding;
import com.penn.jba.footprint.FootprintFragment;
import com.penn.jba.message.MessageActivity;
import com.penn.jba.model.MessageEvent;
import com.penn.jba.model.realm.CurrentUser;
import com.penn.jba.model.realm.Footprint;
import com.penn.jba.model.realm.Pic;
import com.penn.jba.nearby.NearbyFragment;
import com.penn.jba.util.FootprintStatus;
import com.penn.jba.util.PPHelper;
import com.penn.jba.util.PPJSONObject;
import com.penn.jba.util.PPRetrofit;
import com.penn.jba.util.PPSocketSingleton;
import com.penn.jba.util.PPValueType;
import com.penn.jba.util.PPWarn;
import com.penn.jba.util.PicStatus;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UploadManager;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lasque.tusdk.core.TuSdkResult;
import org.lasque.tusdk.core.struct.TuSdkSize;
import org.lasque.tusdk.core.utils.TLog;
import org.lasque.tusdk.core.utils.image.BitmapHelper;
import org.lasque.tusdk.core.utils.sqllite.ImageSqlInfo;
import org.lasque.tusdk.geev2.TuSdkGeeV2;
import org.lasque.tusdk.geev2.impl.components.TuRichEditComponent;
import org.lasque.tusdk.impl.activity.TuFragment;
import org.lasque.tusdk.modules.components.TuSdkComponent;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

//import de.jonasrottmann.realmbrowser.RealmBrowser;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmConfiguration;
import io.realm.RealmList;
import io.realm.RealmResults;

import static com.penn.jba.util.PPHelper.ppWarning;

public class TabsActivity extends AppCompatActivity implements Drawer.OnDrawerItemClickListener, TuSdkComponent.TuSdkComponentDelegate {
    private static final int CREATE_MOMENT = 1001;

    private Context activityContext;

    private ActivityTabsBinding binding;

    private ArrayList<Disposable> disposableList = new ArrayList<Disposable>();

    private FragmentPagerAdapter adapterViewPager;

    private Drawer drawerResult;

    private AccountHeader headerResult;

    private ProfileDrawerItem profileDrawerItem;

    private String createMomentKey;

    private Configuration config = new Configuration.Builder().build();

    private UploadManager uploadManager = new UploadManager(config);

    private PrimaryDrawerItem item4;

    private Realm realm;

    private CurrentUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //common
        activityContext = this;
        binding = DataBindingUtil.setContentView(this, R.layout.activity_tabs);
        binding.setPresenter(this);
        //end common

        realm = Realm.getDefaultInstance();
        setup();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        realm.close();
        for (Disposable d : disposableList) {
            if (!d.isDisposed()) {
                d.dispose();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CREATE_MOMENT && resultCode == RESULT_OK) {
            uploadMoment(createMomentKey);
        }
    }

    private void setup() {
        Intent intent = new Intent(this, new PPService().getClass());
        startService(intent);

        tryRepublishMoment();

        adapterViewPager = new MyPagerAdapter(getSupportFragmentManager());
        binding.mainVp.setAdapter(adapterViewPager);

        binding.mainVp.setSwipeable(false);
        binding.mainVp.setCurrentItem(0);
        binding.tl.setTitle(adapterViewPager.getPageTitle(0));

        setSupportActionBar(binding.tl);

        //if you want to update the items at a later time it is recommended to keep it in a variable
        PrimaryDrawerItem item1 = new PrimaryDrawerItem().withIdentifier(1).withName(R.string.footprint).withIcon(R.drawable.ic_collections_black_24dp);
        PrimaryDrawerItem item2 = new PrimaryDrawerItem().withIdentifier(2).withName(R.string.nearby).withIcon(R.drawable.ic_near_me_black_24dp);
        PrimaryDrawerItem item3 = new PrimaryDrawerItem().withIdentifier(3).withName("Database").withIcon(R.drawable.ic_near_me_black_24dp);
        item4 = new PrimaryDrawerItem().withIdentifier(4).withName(R.string.message).withIcon(R.drawable.ic_near_me_black_24dp);

        PrimaryDrawerItem item0 = new PrimaryDrawerItem().withIdentifier(0).withName(R.string.logout).withIcon(R.drawable.ic_eject_black_24dp);

//        DrawerImageLoader.init(new AbstractDrawerImageLoader() {
//            @Override
//            public void set(ImageView imageView, Uri uri, Drawable placeholder) {
//                Picasso.with(imageView.getContext()).load(uri).placeholder(placeholder).into(imageView);
//            }
//
//            @Override
//            public void cancel(ImageView imageView) {
//                Picasso.with(imageView.getContext()).cancelRequest(imageView);
//            }
//        });

        profileDrawerItem = new ProfileDrawerItem();

        // Create the AccountHeader
//        headerResult = new AccountHeaderBuilder()
//                .withActivity(this)
//                .withHeaderBackground(R.drawable.header)
//                .addProfiles(
//                        //new ProfileDrawerItem().withName("Mike Penz").withIcon(R.drawable.profile)
//                        profileDrawerItem
//                )
//                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
//                    @Override
//                    public boolean onProfileChanged(View view, IProfile profile, boolean currentProfile) {
//                        return false;
//                    }
//                })
//                .build();

        //create the drawer and remember the `Drawer` result object
        drawerResult = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(binding.tl)
                .withAccountHeader(headerResult)
                .addDrawerItems(
                        item1,
                        new DividerDrawerItem(),
                        item2,
                        item3,
                        item4
                )
                .withOnDrawerItemClickListener(this)
                .build();

        drawerResult.addStickyFooterItem(item0);

//        //更换个性化背景图
//        try (Realm realm = Realm.getDefaultInstance()) {
//            CurrentUser currentUser = realm.where(CurrentUser.class).findFirst();
//            if (currentUser.getPics().size() > 0) {
//                Picasso.with(activityContext)
//                        .load(PPHelper.get80ImageUrl(currentUser.getPics().get(0).getNetFileName()))
//                        .into(headerResult.getHeaderBackgroundView());
//            }
//        }

//      updateProfile();

        // 获取用户未读消息
        try (Realm realm = Realm.getDefaultInstance()) {
            currentUser = realm.where(CurrentUser.class).findFirstAsync();
        }

        currentUser.addChangeListener(new RealmChangeListener<CurrentUser>() {
            @Override
            public void onChange(CurrentUser element) {
                Log.d("weng", "12343");
                int num = element.getUnreadMessageFriend() + element.getUnreadMessageSystem() + element.getUnreadMessageMoment();
                updateMessageBadge(String.valueOf(num));
            }
        });

        //创建moment按钮监控
        Observable<Object> createMomentButtonObservable = RxView.clicks(binding.createMomentBt)
                .debounce(200, TimeUnit.MILLISECONDS);

        disposableList.add(createMomentButtonObservable
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.io())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                takePhoto();
                            }
                        }
                )
        );
    }

    private void updateMessageBadge(String num) {
        //modify an item of the drawer
        if (num.equals("0")) {
            binding.bdTv.setVisibility(View.INVISIBLE);
        } else {
            binding.bdTv.setText(num);
            binding.bdTv.setVisibility(View.VISIBLE);
        }

        item4.withBadge(num).withBadgeStyle(new BadgeStyle().withTextColor(Color.WHITE).withColorRes(R.color.md_red_700));
        drawerResult.updateItem(item4);
    }

    @Override
    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
        switch ((int) drawerItem.getIdentifier()) {
            case 0:
                //logout
                PPHelper.setPrefBooleanValue("autoLogin", false);
                PPSocketSingleton.close();
                stopService(new Intent(activityContext, PPService.class));
                Intent intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                break;
            case 1:
                //足迹
                binding.mainVp.setCurrentItem(0, false);
                binding.tl.setTitle(adapterViewPager.getPageTitle(0));
                drawerResult.closeDrawer();
                break;
            case 2:
                //迹伴
                binding.mainVp.setCurrentItem(1, false);
                binding.tl.setTitle(adapterViewPager.getPageTitle(1));
                drawerResult.closeDrawer();
                break;
            case 3:
                //test
                PPHelper.startRealmModelsActivity();
                drawerResult.closeDrawer();
                break;
            case 4:
                //test
                Intent intent1 = new Intent(activityContext, MessageActivity.class);
                startActivity(intent1);
                drawerResult.closeDrawer();
                break;
            default:
        }
        // do something with the clicked item :D
        return true;
    }

//    private void updateProfile() {
//        try (Realm realm = Realm.getDefaultInstance()) {
//            String avatar = currentUser.getHead();
//            if (TextUtils.isEmpty(avatar)) {
//                //pptodo 默认头像路径
//                avatar = "";
//            }
//            String avatarNetFileName = PPHelper.get80ImageUrl(avatar);
//
//            String nickname = currentUser.getNickname();
//            int follows = currentUser.getFollows();
//            int fans = currentUser.getFans();
//
//            profileDrawerItem = profileDrawerItem.withIcon(avatarNetFileName)
//                    .withName(nickname)
//                    .withEmail("Follows:" + follows + ", " + "Fans:" + fans);
//            headerResult.updateProfile(profileDrawerItem);
//        }
//    }

    @Override
    public void onComponentFinished(TuSdkResult result, Error error, TuFragment tuFragment) {
        TLog.d("PackageComponentSample onComponentFinished: %s | %s", result.images, error);
        //新建moment
        long now = System.currentTimeMillis();
        createMomentKey = "" + now + "_3_" + PPHelper.currentUserId + "_" + true;
        try (Realm realm = Realm.getDefaultInstance()) {
            //pptodo improve to clear former record with "PREPARE" status
            Footprint ft = new Footprint();
            ft.setKey(createMomentKey);
            ft.setCreateTime(now);
            ft.setStatus(FootprintStatus.PREPARE);
            ft.setType(3);
            ft.setFootprintBelong(FootprintBelong.MINE);

            ft.setPics(new RealmList<Pic>());

            int i = 0;
            for (ImageSqlInfo info : result.images) {
                i++;
                // 方式1：直接通过 ImageSqlInfo 生成 Bitmap
                //pptodo 优化下原始图片
                Bitmap mImage = BitmapHelper.getBitmap(info, true);
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                mImage.compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] data = stream.toByteArray();

                Pic pic = new Pic();
                pic.setKey(createMomentKey + "_" + i);
                pic.setNetFileName(createMomentKey + "_" + i);
                pic.setStatus(PicStatus.LOCAL);
                pic.setLocalData(data);
                ft.getPics().add(pic);
            }

            realm.beginTransaction();
            realm.copyToRealm(ft);
            realm.commitTransaction();
        }
        Intent intent = new Intent(activityContext, CreateMomentActivity.class);
        intent.putExtra("key", createMomentKey);
        startActivityForResult(intent, CREATE_MOMENT);
    }

    public void takePhoto() {
        TuRichEditComponent comp = TuSdkGeeV2.richEditCommponent(this, this);

        // 组件选项配置
        // 设置是否启用图片编辑 默认 true
        // comp.componentOption().setEnableEditMultiple(true);

        // 相机组件配置
        // 设置拍照后是否预览图片 默认 true
        // comp.componentOption().cameraOption().setEnablePreview(true);

        // 多选相册组件配置
        // 设置相册最大选择数量
        comp.componentOption().albumMultipleComponentOption().albumListOption().setMaxSelection(1);

        // 多功能编辑组件配置项
        // 设置最大编辑数量
        comp.componentOption().editMultipleComponentOption().setMaxEditImageCount(1);

        // 设置没有改变的图片是否保存(默认 false)
        // comp.componentOption().editMultipleComponentOption().setEnableAlwaysSaveEditResult(false);

        // 设置编辑时是否支持追加图片 默认 true
        // comp.componentOption().editMultipleComponentOption().setEnableAppendImage(true);

        // 设置照片排序方式
        // comp.componentOption().albumMultipleComponentOption().albumListOption().setPhotosSortDescriptor(PhotoSortDescriptor.Date_Added);

        // 设置最大支持的图片尺寸 默认：8000 * 8000
        comp.componentOption().albumMultipleComponentOption().albumListOption().setMaxSelectionImageSize(new TuSdkSize(1000, 1000));

        // 操作完成后是否自动关闭页面
        comp.setAutoDismissWhenCompleted(true)
                // 显示组件
                .showComponent();
    }

    private class MyPagerAdapter extends FragmentPagerAdapter {
        private final int NUM_ITEMS = 2;

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
                    return FootprintFragment.newInstance();
                case 1: // Fragment # 0 - This will show FirstFragment different title
                    return NearbyFragment.newInstance();
                default:
                    return null;
            }
        }

        // Returns the page title for the top indicator
        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getResources().getString(R.string.footprint);
                case 1:
                    return getResources().getString(R.string.nearby);
                default:
                    return "";
            }
        }
    }

    private void tryRepublishMoment() {
        try (Realm realm = Realm.getDefaultInstance()) {
            RealmResults<Footprint> fts = realm.where(Footprint.class)
                    .equalTo("status", FootprintStatus.LOCAL.toString())
                    .findAll();

            for (Footprint ft : fts) {
                uploadMoment(ft.getKey());
            }
        }
    }

    private void uploadMoment(final String needUploadKey) {
        ArrayList<Observable<String>> obsList = new ArrayList();

        PPJSONObject jBody0 = new PPJSONObject();

        try (Realm realm = Realm.getDefaultInstance()) {
            final Footprint ft = realm.where(Footprint.class).equalTo("key", needUploadKey).findFirst();
            //上传图片
            RealmList<Pic> pics = ft.getPics();

            for (int i = 0; i < pics.size(); i++) {
                final Pic item = pics.get(i);
                if (item.getStatus().equals(PicStatus.NET.toString())) {
                    continue;
                }
                final byte[] tmpData = item.getLocalData();
                final String key = item.getKey();
                PPJSONObject jBody = new PPJSONObject();
                jBody
                        .put("type", "public")
                        .put("filename", key);

                final Observable<String> apiResult1 = PPRetrofit.getInstance().api("system.generateUploadToken", jBody.getJSONObject());

                obsList.add(apiResult1.flatMap(new Function<String, ObservableSource<String>>() {
                    @Override
                    public ObservableSource<String> apply(String tokenMsg) throws Exception {
                        PPWarn ppWarn = ppWarning(tokenMsg);
                        if (ppWarn != null) {
                            throw new Exception("ppError:" + ppWarn.msg + ":" + key);
                        }
                        String token = PPHelper.ppFromString(tokenMsg, "data.token").getAsString();
                        return uploadSingleImage(tmpData, key, token);
                    }
                }));
            }

            JSONArray jsonArrayPics = new JSONArray();

            for (Pic pic : pics) {
                jsonArrayPics.put(pic.getKey());
            }

            JSONArray geoJsonArray = new JSONArray();
            String tmpBody = ft.getBody();
            JsonArray geo = PPHelper.ppFromString(tmpBody, "detail.location.geo").getAsJsonArray();

            try {
                geoJsonArray.put(geo.get(0).getAsFloat());
                geoJsonArray.put(geo.get(1).getAsFloat());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            jBody0
                    .put("pics", jsonArrayPics)
                    .put("address", ft.getPlace())
                    .put("geo", geoJsonArray)
                    .put("content", ft.getContent())
                    .put("createTime", ft.getCreateTime());
        }

        final Observable<String> apiResult = PPRetrofit.getInstance()
                .api("moment.publish", jBody0.getJSONObject());

        disposableList.add(
                Observable.mergeDelayError(obsList)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .concatWith(apiResult)
                        .observeOn(Schedulers.io())
                        .subscribe(new Consumer<String>() {
                                       @Override
                                       public void accept(String s) throws Exception {
                                           Log.v("pplog102", "ok:" + s);
                                           //判断是moment.publish返回的结果
                                           if (PPHelper.ppFromString(s, "code", PPValueType.INT).getAsInt() != 0) {
                                               PPWarn ppWarn = ppWarning(s);
                                               if (ppWarn != null) {
                                                   Log.v("pplog", "error:" + ppWarn.msg);
                                                   uploadMomentFailed(needUploadKey);
                                                   return;
                                               }

                                               uploadMomentOK(needUploadKey);
                                           }
                                       }
                                   },
                                new Consumer<Throwable>() {
                                    @Override
                                    public void accept(Throwable t) throws Exception {
                                        Log.v("pplog102", "error:" + t);
                                        uploadMomentFailed(needUploadKey);
                                    }
                                }));
    }

    private void uploadMomentFailed(String key) {
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.beginTransaction();
            realm.where(Footprint.class)
                    .equalTo("key", key)
                    .findFirst()
                    .setStatus(FootprintStatus.FAILED);
            realm.commitTransaction();
        }
    }

    private void uploadMomentOK(String key) {
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.beginTransaction();
            realm.where(Footprint.class)
                    .equalTo("key", key)
                    .findFirst()
                    .setStatus(FootprintStatus.NET);
            realm.commitTransaction();
        }
    }

    private Observable<String> uploadSingleImage(final byte[] data, final String key, final String token) {
        return Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(final ObservableEmitter<String> emitter) throws Exception {
                uploadManager.put(data, key, token,
                        new UpCompletionHandler() {
                            @Override
                            public void complete(String key, ResponseInfo info, JSONObject res) {
                                //res包含hash、key等信息，具体字段取决于上传策略的设置
                                if (info.isOK()) {
                                    Log.i("qiniu", "Upload Success:" + key);
                                    //修改本地数据库对应图片状态为NET
                                    try (Realm realm = Realm.getDefaultInstance()) {
                                        realm.beginTransaction();
                                        Pic pic = realm.where(Pic.class).equalTo("key", key).findFirst();
                                        if (pic != null) {
                                            pic.setStatus(PicStatus.NET);
                                        } else {
                                            Exception apiError = new Exception("七牛上传:" + key + "失败", new Throwable("realm中没有找到指定图片"));
                                            emitter.onError(apiError);
                                        }
                                        realm.commitTransaction();
                                    }
                                    emitter.onNext(key);
                                    emitter.onComplete();
                                } else {
                                    Log.i("qiniu", "Upload Fail");
                                    //如果失败，这里可以把info信息上报自己的服务器，便于后面分析上传错误原因
                                    Exception apiError = new Exception("七牛上传:" + key + "失败", new Throwable(info.error.toString()));
                                    emitter.onError(apiError);
                                }
                                Log.i("qiniu", key + ",\r\n " + info + ",\r\n " + res);
                            }
                        }, null);

            }
        });
    }
}
