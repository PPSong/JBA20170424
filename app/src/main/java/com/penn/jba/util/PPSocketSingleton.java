package com.penn.jba.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.gson.JsonObject;
import com.penn.jba.R;
import com.penn.jba.model.MessageEvent;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;
import org.msgpack.core.MessagePack;
import org.msgpack.core.MessageUnpacker;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import static android.R.attr.value;
import static com.penn.jba.util.PPHelper.ppWarning;
import static com.penn.jba.util.PPHelper.socketUrl;

/**
 * Created by penn on 03/05/2017.
 */

public class PPSocketSingleton {
    private static PPSocketSingleton instance;

    private static Socket socket;

    private PPSocketSingleton(String url) {
        Log.v("ppLog", "PPSocketSingleton");
        Log.v("pplog160", "socketUrl:" + url);
        try {
            socket = IO.socket(url);

            socket
                    .on(
                            Socket.EVENT_CONNECT,
                            new Emitter.Listener() {

                                @Override
                                public void call(Object... args) {
                                    Log.v("ppLog", "Socket.EVENT_CONNECT");
                                    try {
                                        JSONObject tmpBody = new JSONObject();
                                        JSONObject _sess = new JSONObject();
                                        _sess.put("userid", PPHelper.currentUserId);

                                        _sess.put("token", PPHelper.token);
                                        _sess.put("tokentimestamp", PPHelper.tokenTimestamp);
                                        tmpBody.put("_sess", _sess);


                                        socket.emit("$init", tmpBody);
                                        Log.v("pplog165", tmpBody.toString());
                                        //socket.disconnect();
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }

                            })
                    .on(
                            "$init",
                            new Emitter.Listener() {

                                @Override
                                public void call(Object... args) {
                                    String msg = args[0].toString();
                                    Log.v("ppLog", "$init from server:" + msg);
                                }
                            })
                    .on(
                            "$kick",
                            new Emitter.Listener() {

                                @Override
                                public void call(Object... args) {
                                    String msg = args[0].toString();
                                    Log.v("ppLog", "$kick from server:" + msg);
                                }
                            })
                    .on(
                            "sync",
                            new Emitter.Listener() {

                                @Override
                                public void call(Object... args) {
//                                    MessageUnpacker unPacker= MessagePack.newDefaultUnpacker((byte[])args[0]);
//
//                                    try {
//                                        String msg = unPacker.unpackValue().toString();
//                                        Log.v("ppLog", "sync from server:" + msg);
//                                    } catch (IOException e) {
//                                        e.printStackTrace();
//                                        Log.v("ppLog", "sync from server err:");
//                                    }
                                    //pptodo 暂时不用管过来啥, 调用message.list更新未读数
                                    sync();
                                }
                            })
                    .on(
                            Socket.EVENT_DISCONNECT, new Emitter.Listener() {

                                @Override
                                public void call(Object... args) {
                                    Log.v("ppLog", "Socket.EVENT_DISCONNECT");
                                }

                            });

            socket.connect();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void close() {
        socket.close();
        instance = null;
    }

    public static PPSocketSingleton getInstance(String url) {
        if (instance == null) {
            instance = new PPSocketSingleton(url);
        }
        Log.v("pplog162", "testUrl3:");
        Log.v("pplog162", "testUrl4:");

        return instance;
    }

    private void sync() {
        PPJSONObject jBody = new PPJSONObject();

        final Observable<String> apiResult = PPRetrofit.getInstance()
                .api("message.getNum", jBody.getJSONObject());

        apiResult
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String s) throws Exception {
                        PPWarn ppWarn = ppWarning(s);
                        if (ppWarn != null) {
                            throw new Exception(ppWarn.msg);
                        }

                        int totalUnread = PPHelper.ppFromString(s, "data.totalUnread").getAsInt();
                        EventBus.getDefault().post(new MessageEvent("updateMessageBadge", "" + totalUnread));
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable t) throws Exception {
                        PPHelper.ppShowError(t.toString());
                    }
                });
    }
}
