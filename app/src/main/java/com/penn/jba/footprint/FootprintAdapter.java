package com.penn.jba.footprint;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.penn.jba.CollectDetailActivity;
import com.penn.jba.FootprintBelong;
import com.penn.jba.MomentDetailActivity;
import com.penn.jba.PPApplication;
import com.penn.jba.R;

import com.penn.jba.dailyReport.DailyReportActivity;
import com.penn.jba.databinding.FootprintType11Binding;
import com.penn.jba.databinding.FootprintType4Binding;
import com.penn.jba.databinding.FootprintType8Binding;
import com.penn.jba.databinding.FootprintType9Binding;
import com.penn.jba.databinding.FootprintType1Binding;
import com.penn.jba.databinding.FootprintType3Binding;
import com.penn.jba.databinding.FootprintType10Binding;
import com.penn.jba.databinding.FootprintType0Binding;
import com.penn.jba.databinding.ListRowAllMomentBinding;
import com.penn.jba.model.CollectMoment;
import com.penn.jba.model.realm.Footprint;

import com.penn.jba.util.CollectMomentImageAdapter;
import com.penn.jba.util.PPHelper;
import com.penn.jba.util.PPLoadAdapter;
import com.penn.jba.util.PPValueType;
import com.penn.jba.view.RoundedTransformation;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import static com.penn.jba.util.PPHelper.ppFromString;

/**
 * Created by penn on 14/04/2017.
 */

public class FootprintAdapter extends PPLoadAdapter<Footprint> {
    private Context context;

    public FootprintAdapter(Context context, List<Footprint> data, FootprintBelong footprintBelong) {
        super(context, data, footprintBelong);
        this.context = context;
    }

    public int getRealItemViewType(Footprint ft) {
        return ft.getType();
    }

    @Override
    public RecyclerView.ViewHolder onCreateRealViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

        switch (viewType) {
            case 8:
                FootprintType8Binding binding8 = FootprintType8Binding.inflate(layoutInflater, parent, false);
                return new FootprintType8ViewHolder(binding8);
            case 9:
                FootprintType9Binding binding9 = FootprintType9Binding.inflate(layoutInflater, parent, false);
                return new FootprintType9ViewHolder(binding9);
            case 1:
                FootprintType1Binding binding1 = FootprintType1Binding.inflate(layoutInflater, parent, false);
                return new FootprintType1ViewHolder(binding1);
            case 3:
                FootprintType3Binding binding3 = FootprintType3Binding.inflate(layoutInflater, parent, false);
                return new FootprintType3ViewHolder(binding3);
            case 10:
                FootprintType10Binding binding10 = FootprintType10Binding.inflate(layoutInflater, parent, false);
                return new FootprintType10ViewHolder(binding10);
            case 0:
                FootprintType0Binding binding0 = FootprintType0Binding.inflate(layoutInflater, parent, false);
                return new FootprintType0ViewHolder(binding0);
            case 11:
                FootprintType11Binding binding11 = FootprintType11Binding.inflate(layoutInflater, parent, false);
                return new FootprintType11ViewHolder(binding11);
            case 4:
                FootprintType4Binding binding4 = FootprintType4Binding.inflate(layoutInflater, parent, false);
                return new FootprintType4ViewHolder(binding4);
            default:
                ListRowAllMomentBinding binding = ListRowAllMomentBinding.inflate(layoutInflater, parent, false);
                return new PPViewHolder(binding);
        }
    }

    @Override
    public void onBindRealViewHolder(RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof FootprintType8ViewHolder) {
            ((FootprintType8ViewHolder) holder).bind(data.get(position));
        } else if (holder instanceof FootprintType9ViewHolder) {
            ((FootprintType9ViewHolder) holder).bind(data.get(position));
        } else if (holder instanceof FootprintType1ViewHolder) {
            ((FootprintType1ViewHolder) holder).bind(data.get(position));
        } else if (holder instanceof FootprintType3ViewHolder) {
            ((FootprintType3ViewHolder) holder).bind(data.get(position));
        } else if (holder instanceof FootprintType10ViewHolder) {
            ((FootprintType10ViewHolder) holder).bind(data.get(position));
        } else if (holder instanceof FootprintType0ViewHolder) {
            ((FootprintType0ViewHolder) holder).bind(data.get(position));
        } else if (holder instanceof FootprintType11ViewHolder) {
            ((FootprintType11ViewHolder) holder).bind(data.get(position));
        } else if (holder instanceof FootprintType4ViewHolder) {
            ((FootprintType4ViewHolder) holder).bind(data.get(position));
        } else {
            ((PPViewHolder) holder).bind(data.get(position));
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Footprint ft = data.get(position);
                if (ft.getType() == 10) {
                    Intent intent = new Intent(context, DailyReportActivity.class);
                    intent.putExtra("dailyReportId", ft.getId());
                    context.startActivity(intent);
                } else if (ft.getType() == 4) {
                    Intent intent = new Intent(context, CollectDetailActivity.class);
                    intent.putExtra("avatarStr", ft.getAvatarNetFileName());
                    intent.putExtra("nickname", ft.getOtherUserNickname());

                    intent.putExtra("content", ft.getContent() + ":" + ppFromString(ft.getBody(), "detail.num").getAsInt());
                    intent.putExtra("geoStr", ppFromString(ft.getBody(), "detail.location.geo").getAsJsonArray().toString());

                    JsonArray moments = PPHelper.ppFromString(ft.getBody(), "detail.moments").getAsJsonArray();
                    ArrayList<CollectMoment> collectMoments = new ArrayList<>();
                    for (int i = 0; i < moments.size(); i++) {
                        String id = PPHelper.ppFromString(ft.getBody(), "detail.moments." + i + ".id").getAsString();
                        String picStr = PPHelper.ppFromString(ft.getBody(), "detail.moments." + i + ".pics." + 0).getAsString();
                        collectMoments.add(new CollectMoment(id, picStr));
                    }

                    intent.putExtra("collectMomentsStr", new Gson().toJson(collectMoments));

                    context.startActivity(intent);
                } else if (ft.getType() == 3) {
                    Intent intent = new Intent(context, MomentDetailActivity.class);
                    Log.d("weng123",""+ppFromString(ft.getBody(),"detail.id").getAsString());
                    intent.putExtra("momentId", ppFromString(ft.getBody(),"detail.id").getAsString());
                    context.startActivity(intent);
                }
            }
        });
    }

    public static class PPViewHolder extends RecyclerView.ViewHolder {
        private final ListRowAllMomentBinding binding;

        public PPViewHolder(ListRowAllMomentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Footprint ft) {
            binding.setPresenter(ft);
            binding.executePendingBindings();
        }
    }

    // x寄了一封信给我
    public class FootprintType8ViewHolder extends RecyclerView.ViewHolder {
        private final FootprintType8Binding binding;

        public FootprintType8ViewHolder(FootprintType8Binding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Footprint ft) {
            binding.setPresenter(ft);
            binding.executePendingBindings();
            binding.timeLineInclude.dateTv.setText(ft.getDate());
            binding.timeLineInclude.timedetailTv.setText(ft.getTime());
            Picasso.with(PPApplication.getContext())
                    .load(PPHelper.get80ImageUrl(ft.getAvatarNetFileName()))
                    .placeholder(R.drawable.pictures_no).into(binding.avatarIv);
        }
    }

    // x给我回信
    public class FootprintType9ViewHolder extends RecyclerView.ViewHolder {
        private final FootprintType9Binding binding;

        public FootprintType9ViewHolder(FootprintType9Binding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Footprint ft) {
            binding.setPresenter(ft);
            binding.executePendingBindings();
            binding.timeLineInclude.dateTv.setText(ft.getDate());
            binding.timeLineInclude.timedetailTv.setText(ft.getTime());
            Picasso.with(PPApplication.getContext())
                    .load(PPHelper.get80ImageUrl(ft.getAvatarNetFileName()))
                    .placeholder(R.drawable.pictures_no).into(binding.avatarIv);
        }
    }

    // 我关注了x
    public class FootprintType1ViewHolder extends RecyclerView.ViewHolder {
        private final FootprintType1Binding binding;

        public FootprintType1ViewHolder(FootprintType1Binding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Footprint ft) {
            binding.setPresenter(ft);
            binding.executePendingBindings();
            binding.timeLineInclude.dateTv.setText(ft.getDate());
            binding.timeLineInclude.timedetailTv.setText(ft.getTime());
            Picasso.with(PPApplication.getContext())
                    .load(PPHelper.get80ImageUrl(ft.getAvatarNetFileName()))
                    .placeholder(R.drawable.pictures_no).into(binding.avatarIv);
        }
    }

    // 我的moment
    public class FootprintType3ViewHolder extends RecyclerView.ViewHolder {
        private final FootprintType3Binding binding;

        public FootprintType3ViewHolder(FootprintType3Binding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Footprint ft) {
            binding.setPresenter(ft);
            binding.executePendingBindings();
            binding.timeLineInclude.dateTv.setText(ft.getDate());
            binding.timeLineInclude.timedetailTv.setText(ft.getTime());
            binding.contentTv.setText(ft.getContent());
            binding.placeTv.setText(ft.getPlace());

//            //设置图片
            String pics = ft.getPics().get(0).getKey().toString();

            Log.d("weng233", "" + pics);
            Picasso.with(PPApplication.getContext())
                    .load(PPHelper.getSlimImageUrl(pics))
                    .transform(new RoundedTransformation(30, 0))
                    .placeholder(R.drawable.pictures_no).into(binding.contentSiv);
//
//            int picNum = pics.size();
//            int width = 0;
//            if (picNum == 0) {
//                //do nothing
//            } else if (picNum == 1) {
//                binding.mainGv.setNumColumns(1);
//                width = PPHelper.MomentGridViewWidth;
//            } else if (picNum == 2) {
//                binding.mainGv.setNumColumns(2);
//                width = PPHelper.MomentGridViewWidth / 2;
//            } else if (picNum == 3) {
//                binding.mainGv.setNumColumns(2);
//                width = PPHelper.MomentGridViewWidth / 2;
//            } else if (picNum == 4) {
//                binding.mainGv.setNumColumns(2);
//                width = PPHelper.MomentGridViewWidth / 2;
//            } else {
//                binding.mainGv.setNumColumns(3);
//                width = PPHelper.MomentGridViewWidth / 3;
//            }
//
//            final float scale = context.getResources().getDisplayMetrics().density;
//            int pixels = (int) (width * scale + 0.5f);
//            MomentImageAdapter momentImageAdapter = new MomentImageAdapter(context, pics, pixels);
//            binding.mainGv.setAdapter(momentImageAdapter);
        }
    }

    // 我被x人记录了片刻
    public class FootprintType10ViewHolder extends RecyclerView.ViewHolder {
        private final FootprintType10Binding binding;

        public FootprintType10ViewHolder(FootprintType10Binding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Footprint ft) {
            binding.setPresenter(ft);
            binding.executePendingBindings();
            //binding.timeLineInclude.timeTv.setReferenceTime(ft.getCreateTime());
            binding.timeLineInclude.dateTv.setText(ft.getDate());
            binding.timeLineInclude.timedetailTv.setText(ft.getTime());
        }
    }

    // 第一次足迹
    public class FootprintType0ViewHolder extends RecyclerView.ViewHolder {
        private final FootprintType0Binding binding;

        public FootprintType0ViewHolder(FootprintType0Binding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Footprint ft) {
            binding.setPresenter(ft);
            binding.executePendingBindings();
            binding.timeLineInclude.dateTv.setText(ft.getDate());
            binding.timeLineInclude.timedetailTv.setText(ft.getTime());
        }
    }

    public class FootprintType11ViewHolder extends RecyclerView.ViewHolder {
        private final FootprintType11Binding binding;

        public FootprintType11ViewHolder(FootprintType11Binding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Footprint ft) {
            binding.setPresenter(ft);
            binding.executePendingBindings();
            binding.timeLineInclude.dateTv.setText(ft.getDate());
            binding.timeLineInclude.timedetailTv.setText(ft.getTime());

            Picasso.with(PPApplication.getContext())
                    .load(PPHelper.get80ImageUrl(ft.getAvatarNetFileName()))
                    .placeholder(R.drawable.pictures_no).into(binding.avatarIv);

            JsonArray geo = ppFromString(ft.getBody(), "detail.geo").getAsJsonArray();
            Picasso.with(PPApplication.getContext())
                    .load(PPHelper.getBaiduMap(geo))
                    .placeholder(R.drawable.header).into(binding.mapIv);
        }
    }

    public class FootprintType4ViewHolder extends RecyclerView.ViewHolder {
        private final FootprintType4Binding binding;

        public FootprintType4ViewHolder(FootprintType4Binding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Footprint ft) {
            binding.setPresenter(ft);
            binding.executePendingBindings();
            binding.timeLineInclude.dateTv.setText(ft.getDate());
            binding.timeLineInclude.timedetailTv.setText(ft.getTime());

            binding.contentTv.setText(ft.getContent());
            binding.placeTv.setText(ft.getPlace());

            //设置图片
            ArrayList<String> pics = new ArrayList<>();
            String body = ft.getBody();
            JsonArray moments = ppFromString(body, "detail.moments").getAsJsonArray();
            int size = moments.size();

            for (int i = 0; i < size; i++) {
                pics.add(ppFromString(body, "detail.moments." + i + ".pics.0", PPValueType.STRING).getAsString());
            }

            int picNum = pics.size();
            int width = 0;
            if (picNum == 0) {
                //do nothing
            } else if (picNum == 1) {
                binding.mainGv.setNumColumns(1);
                width = PPHelper.MomentGridViewWidth;
            } else if (picNum == 2) {
                binding.mainGv.setNumColumns(2);
                width = PPHelper.MomentGridViewWidth / 2;
            } else if (picNum == 3) {
                binding.mainGv.setNumColumns(2);
                width = PPHelper.MomentGridViewWidth / 2;
            } else if (picNum == 4) {
                binding.mainGv.setNumColumns(2);
                width = PPHelper.MomentGridViewWidth / 2;
            } else {
                binding.mainGv.setNumColumns(3);
                width = PPHelper.MomentGridViewWidth / 3;
            }

            final float scale = context.getResources().getDisplayMetrics().density;
            int pixels = (int) (width * scale + 0.5f);
            CollectMomentImageAdapter collectMomentImageAdapter = new CollectMomentImageAdapter(context, pics, pixels);
            binding.mainGv.setAdapter(collectMomentImageAdapter);
            binding.timeLineInclude.timedetailTv.setText(ft.getTime());
            Picasso.with(PPApplication.getContext())
                    .load(PPHelper.get80ImageUrl(ft.getAvatarNetFileName()))
                    .placeholder(R.drawable.pictures_no).into(binding.avatarIv);
        }
    }
}
