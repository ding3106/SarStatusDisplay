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

public class SarStateAdapter extends RecyclerView.Adapter<SarStateAdapter.ViewHolder> {

    private static final String TAG = "SarStateAdapter";

    private List<String> mSarStateItemList;
    static class ViewHolder extends RecyclerView.ViewHolder{
        TextView sarStateItem;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            sarStateItem = (TextView)itemView.findViewById(R.id.tv_SarStateItem);
        }
    }

    public SarStateAdapter(List<String> sarStateItemList){
        mSarStateItemList = sarStateItemList;
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.sar_state_item, viewGroup, false);

        ViewHolder viewHolder = new ViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        String sarStateItem = mSarStateItemList.get(i);
        viewHolder.sarStateItem.setText(sarStateItem);
        if (i % 2 == 1) {
           // viewHolder.sarStateItem.setBackgroundResource(R.color.lightgray);
        }
        if(i < 2 ){//控制recyclerView前两项字体加粗
            Log.w(TAG, "onBindViewHolder: i = "+ i );
            viewHolder.sarStateItem.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
        }

    }

    @Override
    public int getItemCount() {
        return mSarStateItemList.size();
    }
}
