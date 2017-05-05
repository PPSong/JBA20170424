package com.penn.jba.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.penn.jba.R;
import com.penn.jba.model.realm.Pic;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import io.realm.RealmList;

import static android.R.attr.bitmap;
import static android.view.KeyCharacterMap.load;

/**
 * Created by penn on 21/04/2017.
 */

public class MomentImageAdapter extends BaseAdapter {
    private Context mContext;
    private RealmList<Pic> data;
    private int width;

    public MomentImageAdapter(Context c, RealmList<Pic> data, int width) {
        mContext = c;
        this.data = data;
        this.width = width;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;
        if (convertView == null) {
            // if it's not recycled, initialize some attributes
            imageView = new ImageView(mContext);
            imageView.setLayoutParams(new GridView.LayoutParams(width, width));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            imageView = (ImageView) convertView;
        }

        Pic pic = data.get(position);
        if (pic.getStatus().equals(PicStatus.LOCAL.toString())) {
            //local
            //pptodo compress and resize bitmap
            Bitmap bmp = BitmapFactory.decodeByteArray(pic.getLocalData(), 0, pic.getLocalData().length);
            imageView.setImageBitmap(bmp);
        } else {
            //net
            Picasso.with(mContext)
                    .load(PPHelper.get80ImageUrl(pic.getNetFileName()))
                    .placeholder(R.drawable.pictures_no)
                    .into(imageView);
        }

        return imageView;
    }
}