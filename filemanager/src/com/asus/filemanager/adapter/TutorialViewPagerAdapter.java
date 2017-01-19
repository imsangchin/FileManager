package com.asus.filemanager.adapter;

import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.SpannableString;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.asus.filemanager.R;

import java.util.ArrayList;
import java.util.List;

public class TutorialViewPagerAdapter extends PagerAdapter {

    /**
     * {@link List} contains all views showed on each page of {@link ViewPager}
     */
    List<View> mViewLists;

    /**
     * {@link List} contains all title string value showed on each page.
     */
    private List<SpannableString> mTitleLists;

    /**
     * {@link List} contains all content string value showed on each page.
     */
    private List<SpannableString> mContentLists;

    /**
     * Constructor with needed parameters.
     *
     * @param viewLists The layout view list of each page.
     * @param titleLists The title list of each page.
     * @param contentLists The content list of each page.
     */
    public TutorialViewPagerAdapter(@NonNull List<View> viewLists,
                                    @NonNull List<SpannableString> titleLists,
                                    @NonNull List<SpannableString> contentLists)
    {
        mViewLists = viewLists;
        mTitleLists = new ArrayList<>(titleLists);
        mContentLists = new ArrayList<>(contentLists);
    }

    @Override
    public int getCount() {
        return mViewLists.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView(mViewLists.get(position));
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {

        View rootView = mViewLists.get(position);
        TextView titleView = (TextView) rootView.findViewById(R.id.textView_title);
        TextView contentView = (TextView) rootView.findViewById(R.id.textView_content);

        titleView.setText(mTitleLists.get(position));
        contentView.setText(mContentLists.get(position));

        // Special case, only the first page shows the icon.
        View iconView = rootView.findViewById(R.id.imageView);
        if(position == 0)
        {
            iconView.setVisibility(View.VISIBLE);
        }
        else
        {
            iconView.setVisibility(View.GONE);
        }

        container.addView(rootView, 0);
        return rootView;
    }
}
