package com.penn.jba;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jakewharton.rxbinding2.view.RxView;
import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IProfile;
import com.mikepenz.materialdrawer.util.AbstractDrawerImageLoader;
import com.mikepenz.materialdrawer.util.DrawerImageLoader;
import com.penn.jba.databinding.ActivityTabsBinding;
import com.penn.jba.footprint.FootprintFragment;
import com.penn.jba.model.realm.CurrentUser;
import com.penn.jba.model.realm.Footprint;
import com.penn.jba.model.realm.Pic;
import com.penn.jba.util.FootprintStatus;
import com.penn.jba.util.PPHelper;
import com.penn.jba.util.PPJSONObject;
import com.penn.jba.util.PPRetrofit;
import com.penn.jba.util.PicStatus;
import com.qiniu.android.http.ResponseInfo;
import com.qiniu.android.storage.Configuration;
import com.qiniu.android.storage.UpCompletionHandler;
import com.qiniu.android.storage.UploadManager;
import com.squareup.picasso.Picasso;

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

import de.jonasrottmann.realmbrowser.RealmBrowser;
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
import io.realm.RealmConfiguration;
import io.realm.RealmList;
import io.realm.RealmResults;

import static android.R.attr.key;

public class TabsActivity extends AppCompatActivity implements Drawer.OnDrawerItemClickListener, TuSdkComponent.TuSdkComponentDelegate {
    private Context activityContext;

    private ActivityTabsBinding binding;

    private ArrayList<Disposable> disposableList = new ArrayList<Disposable>();

    private FragmentPagerAdapter adapterViewPager;

    private Drawer drawerResult;

    private AccountHeader headerResult;

    private ProfileDrawerItem profileDrawerItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //common
        activityContext = this;
        binding = DataBindingUtil.setContentView(this, R.layout.activity_tabs);
        binding.setPresenter(this);
        //end common

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
        adapterViewPager = new MyPagerAdapter(getSupportFragmentManager());
        binding.mainVp.setAdapter(adapterViewPager);

        binding.mainVp.setSwipeable(false);
        binding.mainVp.setCurrentItem(0);
        binding.tl.setTitle(adapterViewPager.getPageTitle(0));

        setSupportActionBar(binding.tl);

        //if you want to update the items at a later time it is recommended to keep it in a variable
        PrimaryDrawerItem item1 = new PrimaryDrawerItem().withIdentifier(1).withName(R.string.footprint).withIcon(R.drawable.ic_collections_black_24dp);
        PrimaryDrawerItem item2 = new PrimaryDrawerItem().withIdentifier(2).withName(R.string.nearby).withIcon(R.drawable.ic_near_me_black_24dp);
        PrimaryDrawerItem item3 = new PrimaryDrawerItem().withIdentifier(3).withName("test").withIcon(R.drawable.ic_near_me_black_24dp);

        PrimaryDrawerItem item0 = new PrimaryDrawerItem().withIdentifier(0).withName(R.string.logout).withIcon(R.drawable.ic_eject_black_24dp);

        DrawerImageLoader.init(new AbstractDrawerImageLoader() {
            @Override
            public void set(ImageView imageView, Uri uri, Drawable placeholder) {
                Picasso.with(imageView.getContext()).load(uri).placeholder(placeholder).into(imageView);
            }

            @Override
            public void cancel(ImageView imageView) {
                Picasso.with(imageView.getContext()).cancelRequest(imageView);
            }
        });

        profileDrawerItem = new ProfileDrawerItem();

        // Create the AccountHeader
        headerResult = new AccountHeaderBuilder()
                .withActivity(this)
                .withHeaderBackground(R.drawable.header)
                .addProfiles(
                        //new ProfileDrawerItem().withName("Mike Penz").withIcon(R.drawable.profile)
                        profileDrawerItem
                )
                .withOnAccountHeaderListener(new AccountHeader.OnAccountHeaderListener() {
                    @Override
                    public boolean onProfileChanged(View view, IProfile profile, boolean currentProfile) {
                        return false;
                    }
                })
                .build();

        //create the drawer and remember the `Drawer` result object
        drawerResult = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(binding.tl)
                .withAccountHeader(headerResult)
                .addDrawerItems(
                        item1,
                        new DividerDrawerItem(),
                        item2,
                        item3
                )
                .withOnDrawerItemClickListener(this)
                .build();

        drawerResult.addStickyFooterItem(item0);
        //更换个性化背景图
        try (Realm realm = Realm.getDefaultInstance()) {
            CurrentUser currentUser = realm.where(CurrentUser.class).findFirst();
            if (currentUser.getPics().size() > 0) {
                Picasso.with(activityContext)
                        .load(PPHelper.get80ImageUrl(currentUser.getPics().get(0).getNetFileName()))
                        .into(headerResult.getHeaderBackgroundView());
            }
        }

        updateProfile();

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

    @Override
    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
        switch ((int) drawerItem.getIdentifier()) {
            case 0:
                //logout
                PPHelper.setPrefBooleanValue("autoLogin", false);
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
            default:
        }
        // do something with the clicked item :D
        return true;
    }

    private void updateProfile() {
        try (Realm realm = Realm.getDefaultInstance()) {
            CurrentUser currentUser = realm.where(CurrentUser.class).findFirst();
            String avatar = currentUser.getHead();
            if (TextUtils.isEmpty(avatar)) {
                //pptodo 默认头像路径
                avatar = "";
            }
            String avatarNetFileName = PPHelper.get80ImageUrl(avatar);

            String nickname = currentUser.getNickname();
            int follows = currentUser.getFollows();
            int fans = currentUser.getFans();

            profileDrawerItem = profileDrawerItem.withIcon(avatarNetFileName)
                    .withName(nickname)
                    .withEmail("Follows:" + follows + ", " + "Fans:" + fans);
            headerResult.updateProfile(profileDrawerItem);
        }
    }

    @Override
    public void onComponentFinished(TuSdkResult result, Error error, TuFragment tuFragment) {
        TLog.d("PackageComponentSample onComponentFinished: %s | %s", result.images, error);
        //新建moment
        long now = System.currentTimeMillis();
        String key = "" + now + "_3_" + PPHelper.currentUserId + "_" + true;
        try (Realm realm = Realm.getDefaultInstance()) {
            //pptodo improve to clear former record with "PREPARE" status
            Footprint ft = new Footprint();
            ft.setKey(key);
            ft.setCreateTime(now);
            ft.setStatus(FootprintStatus.PREPARE);
            ft.setType(3);
            ft.setMine(true);

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
                pic.setKey(key + "_" + i);
                pic.setNetFileName(key + "_" + i);
                pic.setStatus(PicStatus.LOCAL);
                pic.setLocalData(data);
                ft.getPics().add(pic);
            }

            realm.beginTransaction();
            realm.copyToRealm(ft);
            realm.commitTransaction();
        }
        Intent intent = new Intent(activityContext, CreateMomentActivity.class);
        intent.putExtra("key", key);
        startActivity(intent);
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
        comp.componentOption().albumMultipleComponentOption().albumListOption().setMaxSelection(9);

        // 多功能编辑组件配置项
        // 设置最大编辑数量
        comp.componentOption().editMultipleComponentOption().setMaxEditImageCount(9);

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
}
