package com.chrisfeb.app.udpsendfiletwo;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class LogAdapter extends RecyclerView.Adapter<LogAdapter.LogViewHolder> {

    private ArrayList<String> mLogArray = new ArrayList<>();
    private ArrayList<String> mDateArray = new ArrayList<>();

    private RecyclerView mRecyclerView;

    private static final String TAG = "LogAdapter";

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

        Context context = viewGroup.getContext();
        int layoutForLogItem = R.layout.list_item;
        LayoutInflater inflater = LayoutInflater.from(context);
        boolean shouldAttachToParentImmediately = false;

        View view = inflater.inflate(layoutForLogItem, viewGroup, shouldAttachToParentImmediately);

        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder logViewHolder, int i) {

        logViewHolder.mLogItem.setText(mLogArray.get(i));
        logViewHolder.mTvDate.setText(mDateArray.get(i));
    }

    @Override
    public int getItemCount() {
        if (mLogArray.isEmpty()){
            return 0;
        } else {
            return mLogArray.size();
        }
    }

    public class LogViewHolder extends RecyclerView.ViewHolder {
        private final TextView mLogItem;
        private final TextView mTvDate;
        public LogViewHolder(@NonNull View itemView) {
            super(itemView);

            mLogItem = itemView.findViewById(R.id.tv_log);
            mTvDate = itemView.findViewById(R.id.tv_date);
        }
    }


    public void addItem(String newLog, String date){

        mLogArray.add(newLog);
        mDateArray.add(date);
        mRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
                mRecyclerView.scrollToPosition(mLogArray.size() - 1);
            }
        });
    }
}
