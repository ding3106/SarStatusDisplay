package com.motorola.sarstatusdisplay;

import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class WifiStateAdapter  extends RecyclerView.Adapter<WifiStateAdapter.ViewHolder>{

    private static final String TAG = "WifiStateAdapter";

    private List<String> mWifiStateItemList;
    static class ViewHolder extends RecyclerView.ViewHolder{
        TextView wifiStateItem;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            wifiStateItem = (TextView)itemView.findViewById(R.id.tv_SarStateItem);
        }
    }

    public WifiStateAdapter(List<String> mWifiStateItemList){
        this.mWifiStateItemList = mWifiStateItemList;
    }
    @NonNull
    @Override
    public WifiStateAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.sar_state_item, viewGroup, false);

        WifiStateAdapter.ViewHolder viewHolder = new WifiStateAdapter.ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull WifiStateAdapter.ViewHolder viewHolder, int i) {
        String wifiStateItem = mWifiStateItemList.get(i);
        viewHolder.wifiStateItem.setText(wifiStateItem);
        if (i % 2 == 1) {
            // viewHolder.sarStateItem.setBackgroundResource(R.color.lightgray);
        }
        if(i < 3 ){//控制recyclerView前三项字体加粗
            Log.w(TAG, "onBindViewHolder: i = "+ i );
            viewHolder.wifiStateItem.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        }

    }

    @Override
    public int getItemCount() {
        return mWifiStateItemList.size();
    }
}
