package com.penn.jba.model.realm;

import com.penn.jba.R;
import com.penn.jba.util.FootprintStatus;
import com.penn.jba.util.PPHelper;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

import static android.R.attr.type;
import static com.penn.jba.util.PPHelper.ppFromString;

/**
 * Created by penn on 23/04/2017.
 */

public class Footprint extends RealmObject {
    @PrimaryKey
    private String key; //createTime+"_"+type+"_"+createdBy+"_"+isMine
    private String hash;
    private long createTime;
    private String id;
    private String status;
    private int type;
    private String body;
    private boolean isMine;

    private RealmList<Pic> pics;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(FootprintStatus status) {
        this.status = status.toString();
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public boolean isMine() {
        return isMine;
    }

    public void setMine(boolean mine) {
        isMine = mine;
    }

    public RealmList<Pic> getPics() {
        return pics;
    }

    public void setPics(RealmList<Pic> pics) {
        this.pics = pics;
    }

    public String getContent() {
        //pptodo
        return "";
    }

    public String getAvatarNetFileName() {
        //pptodo
        return "";
    }

    public String getPlace() {
        //pptodo
        return "";
    }
}
