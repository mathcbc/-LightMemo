package com.simplememo.simplememo;

/**
 * Created by Benson on 2015/12/15.
 */

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class MemoItemAdapter extends ArrayAdapter<String> {
    private int resourceId;
    public MemoItemAdapter(Context context, int resource, ArrayList<String> objects) {
        super(context, resource, objects);
        resourceId = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String item = getItem(position);
        //最后一个的“增加”item设置为不一样的样式
        if (position == getCount() - 1) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.last_memo_item, null);
            TextView itemName = (TextView) view.findViewById(R.id.last_memo_item_name);
            itemName.setText(item);
            itemName.getPaint().setFakeBoldText(true);//加粗
            itemName.getPaint().setColor(Color.parseColor("#d7d7d7"));
            return view;
        }
        View view = LayoutInflater.from(getContext()).inflate(resourceId, null);
        TextView itemName = (TextView) view.findViewById(R.id.memo_item_name);
        itemName.setText(item);
        return view;
    }

}
