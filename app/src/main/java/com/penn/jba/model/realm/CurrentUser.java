package com.penn.jba.model.realm;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by penn on 09/04/2017.
 */

public class CurrentUser extends RealmObject {
    @PrimaryKey
    private String userId;
    private String token;
    private long tokenTimestamp;
    private String phone;
    private String nickname;
    private int gender;
    private long birthday;
    private String head;
    private String baiduApiUrl;
    private String baiduAkBrowser;
    private String socketHost;
    private int socketPort;
    private int unreadMessageMoment;
    private int unreadMessageIndex;
    private int unreadMessageFriend;
    private int unreadMessageSystem;
    private int follows;
    private int newFriend;
    private int fans;
    private int newFans;
    private String imToken;
    private String imAppKey;
    private int imUnreadCount;
    private RealmList<Pic> pics;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public long getTokenTimestamp() {
        return tokenTimestamp;
    }

    public void setTokenTimestamp(long tokenTimestamp) {
        this.tokenTimestamp = tokenTimestamp;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getGender() {
        return gender;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    public long getBirthday() {
        return birthday;
    }

    public void setBirthday(long birthday) {
        this.birthday = birthday;
    }

    public String getHead() {
        return head;
    }

    public void setHead(String head) {
        this.head = head;
    }

    public String getBaiduApiUrl() {
        return baiduApiUrl;
    }

    public void setBaiduApiUrl(String baiduApiUrl) {
        this.baiduApiUrl = baiduApiUrl;
    }

    public String getBaiduAkBrowser() {
        return baiduAkBrowser;
    }

    public void setBaiduAkBrowser(String baiduAkBrowser) {
        this.baiduAkBrowser = baiduAkBrowser;
    }

    public String getSocketHost() {
        return socketHost;
    }

    public void setSocketHost(String socketHost) {
        this.socketHost = socketHost;
    }

    public int getSocketPort() {
        return socketPort;
    }

    public void setSocketPort(int socketPort) {
        this.socketPort = socketPort;
    }

    public int getUnreadMessageMoment() {
        return unreadMessageMoment;
    }

    public void setUnreadMessageMoment(int unreadMessageMoment) {
        this.unreadMessageMoment = unreadMessageMoment;
    }

    public int getUnreadMessageIndex() {
        return unreadMessageIndex;
    }

    public void setUnreadMessageIndex(int unreadMessageIndex) {
        this.unreadMessageIndex = unreadMessageIndex;
    }

    public int getUnreadMessageFriend() {
        return unreadMessageFriend;
    }

    public void setUnreadMessageFriend(int unreadMessageFriend) {
        this.unreadMessageFriend = unreadMessageFriend;
    }

    public int getUnreadMessageSystem() {
        return unreadMessageSystem;
    }

    public void setUnreadMessageSystem(int unreadMessageSystem) {
        this.unreadMessageSystem = unreadMessageSystem;
    }

    public int getFollows() {
        return follows;
    }

    public void setFollows(int follows) {
        this.follows = follows;
    }

    public int getNewFriend() {
        return newFriend;
    }

    public void setNewFriend(int newFriend) {
        this.newFriend = newFriend;
    }

    public int getFans() {
        return fans;
    }

    public void setFans(int fans) {
        this.fans = fans;
    }

    public int getNewFans() {
        return newFans;
    }

    public void setNewFans(int newFans) {
        this.newFans = newFans;
    }

    public String getImToken() {
        return imToken;
    }

    public void setImToken(String imToken) {
        this.imToken = imToken;
    }

    public String getImAppKey() {
        return imAppKey;
    }

    public void setImAppKey(String imAppKey) {
        this.imAppKey = imAppKey;
    }

    public int getImUnreadCount() {
        return imUnreadCount;
    }

    public void setImUnreadCount(int imUnreadCount) {
        this.imUnreadCount = imUnreadCount;
    }

    public RealmList<Pic> getPics() {
        return pics;
    }

    public void setPics(RealmList<Pic> pics) {
        this.pics = pics;
    }
}
