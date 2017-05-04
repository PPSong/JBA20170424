package com.penn.jba.message;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.penn.jba.CollectDetailActivity;
import com.penn.jba.FootprintBelong;
import com.penn.jba.PPApplication;
import com.penn.jba.R;
import com.penn.jba.dailyReport.DailyReportActivity;
import com.penn.jba.databinding.FootprintType0Binding;
import com.penn.jba.databinding.FootprintType10Binding;
import com.penn.jba.databinding.FootprintType11Binding;
import com.penn.jba.databinding.FootprintType1Binding;
import com.penn.jba.databinding.FootprintType3Binding;
import com.penn.jba.databinding.FootprintType4Binding;
import com.penn.jba.databinding.FootprintType8Binding;
import com.penn.jba.databinding.FootprintType9Binding;
import com.penn.jba.databinding.ListRowAllMessageBinding;
import com.penn.jba.databinding.ListRowAllMomentBinding;
import com.penn.jba.databinding.MessageType10Binding;
import com.penn.jba.databinding.MessageType11Binding;
import com.penn.jba.databinding.MessageType14Binding;
import com.penn.jba.databinding.MessageType15Binding;
import com.penn.jba.databinding.MessageType1Binding;
import com.penn.jba.databinding.MessageType16Binding;
import com.penn.jba.databinding.MessageType6Binding;
import com.penn.jba.databinding.MessageType8Binding;
import com.penn.jba.databinding.MessageType9Binding;
import com.penn.jba.model.CollectMoment;
import com.penn.jba.model.realm.Footprint;
import com.penn.jba.model.realm.Message;
import com.penn.jba.model.realm.Pic;
import com.penn.jba.util.CollectMomentImageAdapter;
import com.penn.jba.util.MessageType;
import com.penn.jba.util.MomentImageAdapter;
import com.penn.jba.util.PPHelper;
import com.penn.jba.util.PPLoadAdapter;
import com.penn.jba.util.PPValueType;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import io.realm.Realm;
import io.realm.RealmList;

import static com.penn.jba.R.string.footprint;
import static com.penn.jba.util.PPHelper.ppFromString;

/**
 * Created by penn on 14/04/2017.
 */

public class MessageAdapter extends RecyclerView.Adapter {
    private final int VIEW_PROG = -1;

    public List<Message> data;

    public MessageType messageType;

    public MessageAdapter(List<Message> data, MessageType messageType) {
        this.data = data;
        this.messageType = messageType;
    }

    public void needLoadMoreCell() {
        try (Realm realm = Realm.getDefaultInstance()) {
            Message message = new Message();
            message.setId("loadMore");
            message.setMessageType(messageType);
            message.setType(VIEW_PROG);

            realm.beginTransaction();
            realm.copyToRealmOrUpdate(message);
            realm.commitTransaction();
        }
    }

    public void cancelLoadMoreCell() {
        try (Realm realm = Realm.getDefaultInstance()) {
            final Message message = realm.where(Message.class)
                    .equalTo("id", "loadMore")
                    .equalTo("messageType", messageType.toString())
                    .findFirst();

            realm.beginTransaction();
            message.deleteFromRealm();
            realm.commitTransaction();
        }
    }

    @Override
    public int getItemViewType(int position) {
        return data.get(position).getType();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                      int viewType) {
        RecyclerView.ViewHolder vh;
        if (viewType == VIEW_PROG) {
            View v = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.progress_item, parent, false);

            vh = new ProgressViewHolder(v);
        } else {
            vh = onCreateRealViewHolder(parent, viewType);
        }
        return vh;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ProgressViewHolder) {
            //pptodo try to remove below two lines
            ((ProgressViewHolder) holder).progressBar.setIndeterminate(true);
            ((ProgressViewHolder) holder).bind();
        } else {
            onBindRealViewHolder(holder, position);
        }
    }

    @Override
    public int getItemCount() {
        int i = data == null ? 0 : data.size();

        return i;
    }

    public static class ProgressViewHolder extends RecyclerView.ViewHolder {
        public ProgressBar progressBar;
        public TextView tv;

        public ProgressViewHolder(View v) {
            super(v);
            progressBar = (ProgressBar) v.findViewById(R.id.pb);
        }

        public void bind() {
        }

    }

    private RecyclerView.ViewHolder onCreateRealViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case 1:
                MessageType1Binding binding1 = MessageType1Binding.inflate(layoutInflater, parent, false);
                return new MessageType1ViewHolder(binding1);
//            case 5:
//                MessageType5Binding binding5 = MessageType5Binding.inflate(layoutInflater, parent, false);
//                return new MessageType5ViewHolder(binding5);
            case 6:
                MessageType6Binding binding6 = MessageType6Binding.inflate(layoutInflater, parent, false);
                return new MessageType6ViewHolder(binding6);
            case 8:
                MessageType8Binding binding8 = MessageType8Binding.inflate(layoutInflater, parent, false);
                return new MessageType8ViewHolder(binding8);
            case 9:
                MessageType9Binding binding9 = MessageType9Binding.inflate(layoutInflater, parent, false);
                return new MessageType9ViewHolder(binding9);
            case 10:
                MessageType10Binding binding10 = MessageType10Binding.inflate(layoutInflater, parent, false);
                return new MessageType10ViewHolder(binding10);
            case 11:
                MessageType11Binding binding11 = MessageType11Binding.inflate(layoutInflater, parent, false);
                return new MessageType11ViewHolder(binding11);
            case 14:
                MessageType14Binding binding14 = MessageType14Binding.inflate(layoutInflater, parent, false);
                return new MessageType14ViewHolder(binding14);
            case 15:
                MessageType15Binding binding15 = MessageType15Binding.inflate(layoutInflater, parent, false);
                return new MessageType15ViewHolder(binding15);
            case 16:
                MessageType16Binding binding16 = MessageType16Binding.inflate(layoutInflater, parent, false);
                return new MessageType16ViewHolder(binding16);
            default:
                ListRowAllMessageBinding binding = ListRowAllMessageBinding.inflate(layoutInflater, parent, false);
                return new PPViewHolder(binding);
        }
    }

    public void onBindRealViewHolder(RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof MessageType1ViewHolder) {
            ((MessageType1ViewHolder) holder).bind(data.get(position));
        } else if (holder instanceof MessageType6ViewHolder) {
            ((MessageType6ViewHolder) holder).bind(data.get(position));
        } else if (holder instanceof MessageType8ViewHolder) {
            ((MessageType8ViewHolder) holder).bind(data.get(position));
        } else if (holder instanceof MessageType9ViewHolder) {
            ((MessageType9ViewHolder) holder).bind(data.get(position));
        } else if (holder instanceof MessageType10ViewHolder) {
            ((MessageType10ViewHolder) holder).bind(data.get(position));
        } else if (holder instanceof MessageType11ViewHolder) {
            ((MessageType11ViewHolder) holder).bind(data.get(position));
        } else if (holder instanceof MessageType14ViewHolder) {
            ((MessageType14ViewHolder) holder).bind(data.get(position));
        } else if (holder instanceof MessageType15ViewHolder) {
            ((MessageType15ViewHolder) holder).bind(data.get(position));
        } else if (holder instanceof MessageType16ViewHolder) {
            ((MessageType16ViewHolder) holder).bind(data.get(position));
        } else {
            ((PPViewHolder) holder).bind(data.get(position));
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Footprint ft = data.get(position);
//                if (ft.getType() == 10) {
//                    Intent intent = new Intent(context, DailyReportActivity.class);
//                    intent.putExtra("dailyReportId", ft.getId());
//                    context.startActivity(intent);
//                } else if (ft.getType() == 4) {
//                    Intent intent = new Intent(context, CollectDetailActivity.class);
//                    intent.putExtra("avatarStr", ft.getAvatarNetFileName());
//                    intent.putExtra("nickname", ft.getOtherUserNickname());
//                    Log.v("pplog139", ft.getBody());
//                    intent.putExtra("content", ft.getContent() + ":" + ppFromString(ft.getBody(), "detail.num").getAsInt());
//                    intent.putExtra("geoStr", ppFromString(ft.getBody(), "detail.location.geo").getAsJsonArray().toString());
//
//                    JsonArray moments = PPHelper.ppFromString(ft.getBody(), "detail.moments").getAsJsonArray();
//                    ArrayList<CollectMoment> collectMoments = new ArrayList<>();
//                    for (int i = 0; i < moments.size(); i++) {
//                        String id = PPHelper.ppFromString(ft.getBody(), "detail.moments." + i + ".id").getAsString();
//                        String picStr = PPHelper.ppFromString(ft.getBody(), "detail.moments." + i + ".pics." + 0).getAsString();
//                        collectMoments.add(new CollectMoment(id, picStr));
//                    }
//
//                    intent.putExtra("collectMomentsStr", new Gson().toJson(collectMoments));
//
//                    context.startActivity(intent);
//                }
            }
        });
    }

    public static class PPViewHolder extends RecyclerView.ViewHolder {
        private final ListRowAllMessageBinding binding;

        public PPViewHolder(ListRowAllMessageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Message message) {
            binding.setPresenter(message);
            binding.executePendingBindings();
        }
    }

    public class MessageType1ViewHolder extends RecyclerView.ViewHolder {
        private final MessageType1Binding binding;

        public MessageType1ViewHolder(MessageType1Binding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Message message) {
            binding.setPresenter(message);
            binding.executePendingBindings();

            binding.nameTv.setText(message.getNickname());
            binding.contentTv.setText(message.getContent());
            binding.timeTv.setReferenceTime(message.getCreateTime());
            Picasso.with(PPApplication.getContext())
                    .load(PPHelper.get80ImageUrl(message.getAvatarNetFileName()))
                    .placeholder(R.drawable.pictures_no).into(binding.avatarIv);
            Picasso.with(PPApplication.getContext())
                    .load(PPHelper.get80ImageUrl(message.getPic()))
                    .placeholder(R.drawable.pictures_no).into(binding.contentIv);
        }
    }

    public class MessageType6ViewHolder extends RecyclerView.ViewHolder {
        private final MessageType6Binding binding;

        public MessageType6ViewHolder(MessageType6Binding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Message message) {
            binding.setPresenter(message);
            binding.executePendingBindings();

            binding.nameTv.setText(message.getNickname());
            binding.contentTv.setText(message.getNickname());
            binding.timeTv.setReferenceTime(message.getCreateTime());
            Picasso.with(PPApplication.getContext())
                    .load(PPHelper.get80ImageUrl(message.getAvatarNetFileName()))
                    .placeholder(R.drawable.pictures_no).into(binding.avatarIv);
            Picasso.with(PPApplication.getContext())
                    .load(PPHelper.get80ImageUrl(message.getPic()))
                    .placeholder(R.drawable.pictures_no).into(binding.contentIv);
        }
    }

    public class MessageType8ViewHolder extends RecyclerView.ViewHolder {
        private final MessageType8Binding binding;

        public MessageType8ViewHolder(MessageType8Binding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Message message) {
            binding.setPresenter(message);
            binding.executePendingBindings();

            binding.nameTv.setText(message.getNickname());
            binding.contentTv.setText(message.getNickname());
            binding.timeTv.setReferenceTime(message.getCreateTime());
            Picasso.with(PPApplication.getContext())
                    .load(PPHelper.get80ImageUrl(message.getAvatarNetFileName()))
                    .placeholder(R.drawable.pictures_no).into(binding.avatarIv);
        }
    }

    public class MessageType9ViewHolder extends RecyclerView.ViewHolder {
        private final MessageType9Binding binding;

        public MessageType9ViewHolder(MessageType9Binding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Message message) {
            binding.setPresenter(message);
            binding.executePendingBindings();

            binding.nameTv.setText(message.getNickname());
            binding.contentTv.setText(message.getNickname());
            binding.timeTv.setReferenceTime(message.getCreateTime());
            Picasso.with(PPApplication.getContext())
                    .load(PPHelper.get80ImageUrl(message.getAvatarNetFileName()))
                    .placeholder(R.drawable.pictures_no).into(binding.avatarIv);
        }
    }

    public class MessageType10ViewHolder extends RecyclerView.ViewHolder {
        private final MessageType10Binding binding;

        public MessageType10ViewHolder(MessageType10Binding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Message message) {
            binding.setPresenter(message);
            binding.executePendingBindings();

            binding.nameTv.setText(message.getNickname());
            binding.contentTv.setText(message.getNickname());
            binding.timeTv.setReferenceTime(message.getCreateTime());
            Picasso.with(PPApplication.getContext())
                    .load(PPHelper.get80ImageUrl(message.getAvatarNetFileName()))
                    .placeholder(R.drawable.pictures_no).into(binding.avatarIv);
        }
    }

    public class MessageType11ViewHolder extends RecyclerView.ViewHolder {
        private final MessageType11Binding binding;

        public MessageType11ViewHolder(MessageType11Binding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Message message) {
            binding.setPresenter(message);
            binding.executePendingBindings();

            binding.nameTv.setText(message.getNickname());
            binding.contentTv.setText(message.getNickname());
            binding.timeTv.setReferenceTime(message.getCreateTime());
            Picasso.with(PPApplication.getContext())
                    .load(PPHelper.get80ImageUrl(message.getAvatarNetFileName()))
                    .placeholder(R.drawable.pictures_no).into(binding.avatarIv);
        }
    }

    public class MessageType14ViewHolder extends RecyclerView.ViewHolder {
        private final MessageType14Binding binding;

        public MessageType14ViewHolder(MessageType14Binding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Message message) {
            binding.setPresenter(message);
            binding.executePendingBindings();

            binding.timeTv.setReferenceTime(message.getCreateTime());
            Picasso.with(PPApplication.getContext())
                    .load(PPHelper.get80ImageUrl(message.getSystemIcon()))
                    .placeholder(R.drawable.pictures_no).into(binding.avatarIv);
        }
    }

    public class MessageType15ViewHolder extends RecyclerView.ViewHolder {
        private final MessageType15Binding binding;

        public MessageType15ViewHolder(MessageType15Binding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Message message) {
            binding.setPresenter(message);
            binding.executePendingBindings();

            binding.nameTv.setText(message.getNickname());
            binding.contentTv.setText(message.getNickname());
            binding.timeTv.setReferenceTime(message.getCreateTime());
            Picasso.with(PPApplication.getContext())
                    .load(PPHelper.get80ImageUrl(message.getAvatarNetFileName()))
                    .placeholder(R.drawable.pictures_no).into(binding.avatarIv);
        }
    }

    public class MessageType16ViewHolder extends RecyclerView.ViewHolder {
        private final MessageType16Binding binding;

        public MessageType16ViewHolder(MessageType16Binding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Message message) {
            binding.setPresenter(message);
            binding.executePendingBindings();

            binding.nameTv.setText(message.getNickname());
            binding.contentTv.setText(message.getNickname());
            binding.timeTv.setReferenceTime(message.getCreateTime());
            Picasso.with(PPApplication.getContext())
                    .load(PPHelper.get80ImageUrl(message.getAvatarNetFileName()))
                    .placeholder(R.drawable.pictures_no).into(binding.avatarIv);
        }
    }
}
