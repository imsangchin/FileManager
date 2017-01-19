package com.asus.filemanager.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.asus.filemanager.R;
import com.asus.filemanager.utility.LocalVFile;
import com.asus.filemanager.utility.VFile;

import java.util.ArrayList;

public class ShoppingCartDetailActivity extends Activity {
    private Context mContext;
    private ArrayList<VFile> mData;
    private ListView mListView;
    private ShoppingCartListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping_cart_detail);
        mContext = ShoppingCartDetailActivity.this;

        Intent intent = getIntent();
        if (intent != null) {
             mData = intent.getParcelableArrayListExtra("a");
        } else {
            Log.e("test", "intent = null");
        }

        mListView = (ListView) findViewById(android.R.id.list);
        mListView.setEmptyView(findViewById(android.R.id.empty));
        mAdapter = new ShoppingCartListAdapter(mContext, mData);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                mData.remove(i);
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void finish() {
        Intent intent1 = new Intent();
        intent1.putParcelableArrayListExtra("b", mData);
        Log.e("test", "mData.size = " + mData.size());
        setResult(123, intent1);
        super.finish();
    }
}
