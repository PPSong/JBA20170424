package com.penn.jba.model.realm;

import com.penn.jba.PPApplication;
import com.penn.jba.R;
import com.penn.jba.util.MessageType;
import com.penn.jba.util.PPHelper;
import com.penn.jba.util.PPValueType;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

import static com.penn.jba.util.PPHelper.ppFromString;

/**
 * Created by penn on 02/05/2017.
 */

public class Message extends RealmObject {
    @PrimaryKey
    private String id;
    private long createTime;
    private boolean read;
    private String messageType;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    private int type;
    private String body;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType.toString();
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getContent() {
        if (type == 1) {
          return ppFromString(body, "content").getAsString();
        }

        return getType() + "," + getId();
    }

    public String getAvatarNetFileName() {
        if (type == 1) {
            return ppFromString(body, "params.targetUser.head").getAsString();
        }
        return "no avatar";
    }

    public String getNickname() {
        if (type == 1) {
            return ppFromString(body, "params.targetUser.nickname").getAsString();
        }
        return "no avatar";
    }
}