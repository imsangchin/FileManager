package com.asus.filemanager.adapter;

import com.asus.filemanager.activity.ShoppingCart;
import com.asus.filemanager.utility.VFile;

public interface BasicFileListAdapter {
    public static class CheckResult {
        public int count;
        public boolean hasDir;
        public int dircount;
    }

    public interface AdapterUpdateObserver {
        public void updateAdapterDone();
    }
    public void updateAdapter(VFile[] files, boolean forceUpdate, int sortType, AdapterUpdateObserver observer);
    public boolean isItemsSelected();
    public VFile[] getFiles();
    public void clearCacheTag();
    public void updateAdapterResult();
    public void notifyDataSetChanged();
    public void isHiddenDate(boolean hidden); // needs rename to setHiddenDate
    public int getCount();
    public void onDrop(int position);
    public void setOrientation(int orientation);
    public CheckResult getSelectedCount();
    public void setSelectAll();
    public Object getItem(int position);
    public CheckResult getFilesCount(); // get num of files which is not directory
    public void clearItemsSelected();
    public void setShoppingCart(ShoppingCart shoppingCart);
    public void syncWithShoppingCart();
}