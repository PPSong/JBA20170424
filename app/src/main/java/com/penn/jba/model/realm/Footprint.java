package com.penn.jba.model.realm;

import com.penn.jba.PPApplication;
import com.penn.jba.R;
import com.penn.jba.util.FootprintStatus;
import com.penn.jba.util.PPHelper;
import com.penn.jba.util.PPValueType;

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
        if (type == 8) {
            String idA = ppFromString(body, "detail.createdBy").getAsString();
            String idB = ppFromString(body, "detail.receivedBy").getAsString();
            String nicknameA = ppFromString(body, "relatedUsers.0.nickname").getAsString();
            String nicknameB = ppFromString(body, "relatedUsers.1.nickname").getAsString();
            if (idA == PPHelper.currentUserId) {
                return PPApplication.getContext().getString(R.string.i_send_a_mail_to) + nicknameB;
            } else {
                return nicknameA + PPApplication.getContext().getString(R.string.send_a_mail_to_me);
            }
        } else if (type == 9) {
            String idA = ppFromString(body, "detail.createdBy").getAsString();
            String idB = ppFromString(body, "detail.receivedBy").getAsString();
            String nicknameA = ppFromString(body, "relatedUsers.0.nickname").getAsString();
            String nicknameB = ppFromString(body, "relatedUsers.1.nickname").getAsString();
            if (idA == PPHelper.currentUserId) {
                String i_reply_to_sb = PPApplication.getContext().getString((R.string.i_reply_to_sb));

                return String.format(i_reply_to_sb, nicknameB);
            } else {
                String sb_reply_to_me = PPApplication.getContext().getString((R.string.sb_reply_to_me));

                return String.format(sb_reply_to_me, nicknameA);
            }
        } else if (type == 1) {
            String idA = ppFromString(body, "relatedUsers.0.id").getAsString();
            String idB = ppFromString(body, "relatedUsers.1.id").getAsString();
            String nicknameA = ppFromString(body, "relatedUsers.0.nickname").getAsString();
            String nicknameB = ppFromString(body, "relatedUsers.1.nickname").getAsString();
            String beFriend = ppFromString(body, "detail.beFriend", PPValueType.INT).getAsInt() == 1 ? PPApplication.getContext().getString(R.string.be_friend) : "";
            if (idA == PPHelper.currentUserId) {
                String i_follow_to_sb = PPApplication.getContext().getString(R.string.i_follow_to_sb);

                return String.format(i_follow_to_sb, nicknameB, beFriend);
            } else {
                String sb_follow_to_me = PPApplication.getContext().getString(R.string.sb_follow_to_me);

                return String.format(sb_follow_to_me, nicknameA, beFriend);
            }
        } else if (type == 3) {
//            String tmp = "";
//            for (Pic pic: pics) {
//                tmp += pic.getKey() + ",";
//            }
            return ppFromString(body, "detail.content").getAsString();
        } else if (type == 10) {
            int fansNum = ppFromString(body, "detail.fansNum").getAsInt();
            int collectNum = ppFromString(body, "detail.collectNum").getAsInt();
            int beCollectedNum = ppFromString(body, "detail.beCollectedNum").getAsInt();
            String result = PPApplication.getContext().getString(R.string.daily_report) + getId();

            return String.format(result, beCollectedNum, collectNum, fansNum);
        } else if (type == 0) {
            return PPApplication.getContext().getString((R.string.welcome));
        }

        return getBody();
    }

    public String getAvatarNetFileName() {
        if (type == 8) {
            String idA = ppFromString(body, "detail.createdBy").getAsString();
            String idB = ppFromString(body, "detail.receivedBy").getAsString();

            if (idA == PPHelper.currentUserId) {
                return ppFromString(body, "relatedUsers.1.head").getAsString();
            } else {
                return ppFromString(body, "relatedUsers.0.head").getAsString();
            }
        } else if (type == 9) {
            String idA = ppFromString(body, "detail.createdBy").getAsString();
            String idB = ppFromString(body, "detail.receivedBy").getAsString();

            if (idA == PPHelper.currentUserId) {
                return ppFromString(body, "relatedUsers.1.head").getAsString();
            } else {
                return ppFromString(body, "relatedUsers.0.head").getAsString();
            }
        } else if (type == 1) {
            String idA = ppFromString(body, "relatedUsers.0.id").getAsString();
            String idB = ppFromString(body, "relatedUsers.1.id").getAsString();

            if (idA == PPHelper.currentUserId) {
                return ppFromString(body, "relatedUsers.1.head").getAsString();
            } else {
                return ppFromString(body, "relatedUsers.0.head").getAsString();
            }
        }
        return "no avatar";
    }

    public String getPlace() {
        if (type == 3) {
            return ppFromString(body, "detail.location.city", PPValueType.STRING).getAsString() + ppFromString(body, "detail.location.detail").getAsString();
        } else {
            return "";
        }
    }
}
