package com.asus.filemanager.activity;

import com.asus.filemanager.R;

import android.app.DialogFragment;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class AboutDialog extends DialogFragment {

    private NewFeatureListAdapter mNewFeatureListAdapter;

    private boolean mShowAbout;

    public static AboutDialog newInstance(boolean showAbout) {
        AboutDialog frag = new AboutDialog();

        Bundle args = new Bundle();
        args.putBoolean("show-about", showAbout);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mShowAbout = getArguments().getBoolean("show-about");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Context context = getActivity();

        View view = inflater.inflate(R.layout.whatsnew, container,
                false);

        if (mShowAbout) {
            getDialog().setTitle(R.string.about);

            PackageInfo pkgInfo = null;
            try {
                pkgInfo = context.getPackageManager().getPackageInfo(
                        getActivity().getPackageName(), 0);
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }

            TextView appVersion = (TextView) view.findViewById(R.id.app_version);
            if (pkgInfo != null && pkgInfo.versionName != null) {
                appVersion.setVisibility(View.VISIBLE);
                String VersionText = getActivity().getString(R.string.version_text) + String.valueOf(pkgInfo.versionName);
                appVersion.setText(VersionText);
            } else {
                appVersion.setVisibility(View.INVISIBLE);
            }
        } else {
            getDialog().setTitle(R.string.whats_new_label);
            View appSnippet = view.findViewById(R.id.app_snippet);
            appSnippet.setVisibility(View.GONE);
        }

        /*
        String[] features = context.getResources().getStringArray(R.array.new_features);
        String[] descriptions = context.getResources().getStringArray(R.array.new_feature_descriptions);
        if (features != null && features.length > 0) {
            View whatsNewLabel = view.findViewById(R.id.whats_new_label);
            if (mShowAbout) whatsNewLabel.setVisibility(View.VISIBLE);

            ListView listView = (ListView) view.findViewById(R.id.new_feature_list);
            mNewFeatureListAdapter = new NewFeatureListAdapter(features, descriptions);
            listView.setAdapter(mNewFeatureListAdapter);
            listView.setEnabled(false);
            listView.setClickable(false);
        }
        */

        return view;
    }

    class ViewHolder {
        ImageView indicator;
        TextView feature;
        TextView description;
    }

    private class NewFeatureListAdapter extends BaseAdapter {

        private String[] mFeatures;
        private String[] mDescriptions;

        public NewFeatureListAdapter(String[] features, String[] descriptions) {
            mFeatures = features;
            mDescriptions = descriptions;
        }

        @Override
        public int getCount() {
            if (mFeatures != null) {
                return mFeatures.length;
            }
            return 0;
        }

        @Override
        public Object getItem(int arg0) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.new_feature_list_item, null);
                holder = new ViewHolder();
                holder.indicator = (ImageView) convertView.findViewById(R.id.item_indicator);
                holder.feature = (TextView) convertView.findViewById(R.id.feature);
                holder.description = (TextView) convertView.findViewById(R.id.description);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            if (mFeatures != null) holder.feature.setText(mFeatures[position]);
            if (mDescriptions != null) holder.description.setText(mDescriptions[position]);

            return convertView;
        }
    }
}
