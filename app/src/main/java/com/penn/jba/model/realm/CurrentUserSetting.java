package com.penn.jba.model.realm;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by penn on 09/04/2017.
 */

public class CurrentUserSetting extends RealmObject {
    @PrimaryKey
    private String userId;

    //记录是否在足迹页面当前显示的是我的moment
    private boolean footprintMine;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isFootprintMine() {
        return footprintMine;
    }

    public void setFootprintMine(boolean footprintMine) {
        this.footprintMine = footprintMine;
    }
}
