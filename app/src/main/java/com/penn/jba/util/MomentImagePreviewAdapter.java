package com.penn.jba.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.penn.jba.model.realm.Pic;

import io.realm.RealmList;

/**
 * Created by penn on 21/04/2017.
 */

public class MomentImagePreviewAdapter extends BaseAdapter {
    private Context mContext;
    private RealmList<Pic> data;
    private int width;

    public MomentImagePreviewAdapter(Context c, RealmList<Pic> data, int width) {
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
            Bitmap bmp = BitmapFactory.decodeByteArray(data.get(position).getLocalData(), 0, data.get(position).getLocalData().length);
            imageView.setImageBitmap(bmp);
        } else {
            imageView = (ImageView) convertView;
        }

        return imageView;
    }
}