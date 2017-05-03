package com.penn.jba.message;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.florent37.materialviewpager.header.MaterialViewPagerHeaderDecorator;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.penn.jba.FootprintBelong;
import com.penn.jba.R;
import com.penn.jba.dailyReport.ReportListAdapter;
import com.penn.jba.databinding.FragmentFootprintMineBinding;
import com.penn.jba.databinding.FragmentMessageListBinding;
import com.penn.jba.databinding.FragmentReportListBinding;
import com.penn.jba.footprint.FootprintAdapter;
import com.penn.jba.model.MessageEvent;
import com.penn.jba.model.realm.Footprint;
import com.penn.jba.model.realm.Message;
import com.penn.jba.model.realm.Pic;
import com.penn.jba.util.FootprintStatus;
import com.penn.jba.util.MessageType;
import com.penn.jba.util.PPHelper;
import com.penn.jba.util.PPJSONObject;
import com.penn.jba.util.PPLoadAdapter;
import com.penn.jba.util.PPRefreshLoadController;
import com.penn.jba.util.PPRetrofit;
import com.penn.jba.util.PPWarn;
import com.penn.jba.util.PicStatus;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

import static android.R.attr.key;
import static com.penn.jba.util.PPHelper.ppWarning;

public class MessageListFragment extends Fragment {

    private final static int pageSize = 15;

    private Context activityContext;

    private ArrayList<Disposable> disposableList = new ArrayList<Disposable>();

    //custom
    private Realm realm;

    private RealmResults<Message> messages;

    private MessageAdapter messageAdapter;

    private FragmentMessageListBinding binding;

    private InnerPPRefreshLoadController ppRefreshLoadController;

    private MessageType messageType;

    private String data = "";

    private String groupName;

    public MessageListFragment() {
        // Required empty public constructor
    }

    public static MessageListFragment newInstance(MessageType type) {
        MessageListFragment fragment = new MessageListFragment();
        Bundle args = new Bundle();
        args.putString("messageType", type.toString());
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityContext = getActivity();
        if (getArguments() != null) {
            String tmpMessageType = getArguments().getString("messageType");
            messageType = MessageType.valueOf(tmpMessageType);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //common
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_message_list, container, false);
        View view = binding.getRoot();
        //end common

        setup();

        return view;
    }

    private void setup() {
        if (messageType == MessageType.MOMENT) {
            groupName = "moment";
        } else if (messageType == MessageType.FRIEND) {
            groupName = "friend";
        } else if (messageType == MessageType.SYSTEM) {
            groupName = "system";
        }

        realm = Realm.getDefaultInstance();
        messages = realm.where(Message.class).equalTo("messageType", messageType.toString()).findAllSorted("createTime", Sort.DESCENDING);
        messages.addChangeListener(changeListener);

        binding.mainRv.setLayoutManager(new LinearLayoutManager(activityContext));
        messageAdapter = new MessageAdapter(messages, messageType);
        binding.mainRv.setAdapter(messageAdapter);

        binding.mainRv.setHasFixedSize(true);

        ppRefreshLoadController = new InnerPPRefreshLoadController(binding.mainSwipeRefreshLayout, binding.mainRv);

        ppRefreshLoadController.onRefresh();
    }

    private final OrderedRealmCollectionChangeListener<RealmResults<Message>> changeListener = new OrderedRealmCollectionChangeListener<RealmResults<Message>>() {
        @Override
        public void onChange(RealmResults<Message> collection, OrderedCollectionChangeSet changeSet) {
            // `null`  means the async query returns the first time.
            if (changeSet == null) {
                messageAdapter.notifyDataSetChanged();
                return;
            }
            // For deletions, the adapter has to be notified in reverse order.
            OrderedCollectionChangeSet.Range[] deletions = changeSet.getDeletionRanges();
            for (int i = deletions.length - 1; i >= 0; i--) {
                OrderedCollectionChangeSet.Range range = deletions[i];
                messageAdapter.notifyItemRangeRemoved(range.startIndex, range.length);
            }

            OrderedCollectionChangeSet.Range[] insertions = changeSet.getInsertionRanges();
            for (OrderedCollectionChangeSet.Range range : insertions) {
                messageAdapter.notifyItemRangeInserted(range.startIndex, range.length);
            }

            OrderedCollectionChangeSet.Range[] modifications = changeSet.getChangeRanges();
            for (OrderedCollectionChangeSet.Range range : modifications) {
                messageAdapter.notifyItemRangeChanged(range.startIndex, range.length);
            }
        }
    };

    private int processMessage(String s, boolean refresh) {
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.beginTransaction();

            if (refresh) {
                RealmResults<Message> r = realm.where(Message.class).equalTo("messageType", messageType.toString()).findAll();
                r.deleteAllFromRealm();
            }

            JsonArray ja = PPHelper.ppFromString(s, "data.list").getAsJsonArray();

            int realNum = 0;
            for (int i = 0; i < ja.size(); i++) {

                //防止loadmore是查询到已有的记录
                long createTime = 0;
                int type = -100;

                String id = PPHelper.ppFromString(s, "data.list." + i + ".id").getAsString();
                createTime = PPHelper.ppFromString(s, "data.list." + i + ".createTime").getAsLong();
                boolean read = PPHelper.ppFromString(s, "data.list." + i + ".read").getAsInt() == 1 ? true : false;
                type = PPHelper.ppFromString(s, "data.list." + i + ".type").getAsInt();

                Message message = realm.where(Message.class)
                        .equalTo("id", id)
                        .findFirst();

                if (message == null) {
                    message = realm.createObject(Message.class, id);
                    realNum++;
                }

                message.setCreateTime(createTime);
                message.setRead(read);
                message.setType(type);
                message.setMessageType(messageType);
                message.setBody(PPHelper.ppFromString(s, "data.list." + i + "").getAsJsonObject().toString());
            }

            realm.commitTransaction();

            return realNum;
        }
    }

    private void setUnreadNum(int totalNum, int currentTypeNum) {
        EventBus.getDefault().post(new MessageEvent("updateMessageBadge", "" + totalNum));
        if (messageType == MessageType.MOMENT) {
            EventBus.getDefault().post(new MessageEvent("updateMomentMessageBadge", "" + currentTypeNum));
        } else if (messageType == MessageType.FRIEND) {
            EventBus.getDefault().post(new MessageEvent("updateFriendMessageBadge", "" + currentTypeNum));
        } else if (messageType == MessageType.SYSTEM) {
            EventBus.getDefault().post(new MessageEvent("updateSystemMessageBadge", "" + currentTypeNum));
        }
    }

    private class InnerPPRefreshLoadController extends PPRefreshLoadController {

        public InnerPPRefreshLoadController(SwipeRefreshLayout swipeRefreshLayout, RecyclerView recyclerView) {
            super(swipeRefreshLayout, recyclerView);
        }

        @Override
        public void doRefresh() {
            PPJSONObject jBody = new PPJSONObject();

            jBody
                    .put("group", groupName);

            final Observable<String> apiResult = PPRetrofit.getInstance().api("message.list", jBody.getJSONObject());
            disposableList.add(
                    apiResult
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    new Consumer<String>() {
                                        public void accept(String s) throws Exception {

                                            PPWarn ppWarn = ppWarning(s);
                                            if (ppWarn != null) {
                                                throw new Exception(ppWarn.msg);
                                            }

                                            processMessage(s, true);

                                            swipeRefreshLayout.setRefreshing(false);
                                            end();
                                            reset();

                                            int totalUnread = PPHelper.ppFromString(s, "data.totalUnread").getAsInt();
                                            int curTypeUnread = PPHelper.ppFromString(s, "data.unRead." + groupName).getAsInt();

                                            setUnreadNum(totalUnread, curTypeUnread);
                                        }
                                    },
                                    new Consumer<Throwable>() {
                                        public void accept(Throwable t1) {
                                            PPHelper.ppShowError(t1.toString());

                                            swipeRefreshLayout.setRefreshing(false);
                                            end();

                                            t1.printStackTrace();
                                        }
                                    }
                            )
            );
        }

        @Override
        public void doLoadMore() {
            PPJSONObject jBody = new PPJSONObject();
            jBody
                    //因为最后一条记录为"loadmore"的fake记录
                    .put("before", "" + messages.get(messages.size() - 2).getCreateTime())
                    .put("group", groupName);

            final Observable<String> apiResult = PPRetrofit.getInstance().api("message.list", jBody.getJSONObject());

            disposableList.add(
                    apiResult
                            .subscribeOn(Schedulers.io())
                            .map(new Function<String, String>() {
                                @Override
                                public String apply(String s) throws Exception {
                                    Log.v("pplog5", s);

                                    PPWarn ppWarn = ppWarning(s);

                                    if (ppWarn != null) {
                                        return ppWarn.msg;
                                    } else {
                                        if (processMessage(s, false) < pageSize) {
                                            noMore();
                                        }

                                        return "OK";
                                    }
                                }
                            })
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    new Consumer<String>() {
                                        public void accept(String s) {
                                            if (s != "OK") {
                                                PPHelper.ppShowError(s);

                                                return;
                                            }

                                            if (recyclerView.getAdapter() instanceof PPLoadAdapter) {
                                                ((PPLoadAdapter) recyclerView.getAdapter()).cancelLoadMoreCell();
                                            } else if (recyclerView.getAdapter() instanceof MessageAdapter) {
                                                ((MessageAdapter) recyclerView.getAdapter()).cancelLoadMoreCell();
                                            }

                                            end();
                                        }
                                    },
                                    new Consumer<Throwable>() {
                                        public void accept(Throwable t1) {
                                            PPHelper.ppShowError(t1.getMessage());
                                            t1.printStackTrace();

                                            if (recyclerView.getAdapter() instanceof PPLoadAdapter) {
                                                ((PPLoadAdapter) recyclerView.getAdapter()).cancelLoadMoreCell();
                                            } else if (recyclerView.getAdapter() instanceof MessageAdapter) {
                                                ((MessageAdapter) recyclerView.getAdapter()).cancelLoadMoreCell();
                                            }
                                            end();
                                        }
                                    }
                            )
            );
        }
    }
}
