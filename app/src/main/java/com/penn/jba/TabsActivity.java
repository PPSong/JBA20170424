package com.penn.jba;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;

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
import com.penn.jba.util.PPHelper;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import de.jonasrottmann.realmbrowser.RealmBrowser;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmConfiguration;

public class TabsActivity extends AppCompatActivity implements Drawer.OnDrawerItemClickListener {
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
                                //pptodo
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