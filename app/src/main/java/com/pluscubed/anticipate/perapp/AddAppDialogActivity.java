package com.pluscubed.anticipate.perapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.inquiry.Inquiry;
import com.bumptech.glide.Glide;
import com.pluscubed.anticipate.R;
import com.pluscubed.anticipate.transition.FabDialogMorphSetup;
import com.pluscubed.anticipate.util.PrefUtils;

import java.util.ArrayList;
import java.util.List;

import rx.Single;
import rx.SingleSubscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func2;

public class AddAppDialogActivity extends AppCompatActivity {

    // static final int PAYLOAD_ICON = 32;

    List<AppInfo> mAppList;
    AppAdapter mAdapter;

    ProgressBar mProgressBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog_add_perapp);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ViewGroup root = (ViewGroup) findViewById(R.id.root);
            root.setTransitionGroup(true);
            FabDialogMorphSetup.setupSharedElementTransitions(this, root, getResources().getDimensionPixelSize(R.dimen.dialog_corners));
        }

        RecyclerView view = (RecyclerView) findViewById(R.id.recyclerview);
        DefaultItemAnimator animator = new DefaultItemAnimator() {
            @Override
            public boolean canReuseUpdatedViewHolder(RecyclerView.ViewHolder viewHolder) {
                return true;
            }
        };
        view.setItemAnimator(animator);
        mAdapter = new AppAdapter();
        view.setAdapter(mAdapter);
        view.setLayoutManager(new LinearLayoutManager(this));

        mAppList = new ArrayList<>();

        mProgressBar = (ProgressBar) findViewById(R.id.progressbar);

        mProgressBar.setVisibility(View.VISIBLE);
        Single.zip(DbUtil.getPerAppListApps(this), DbUtil.getInstalledApps(this), new Func2<List<AppInfo>, List<AppInfo>, List<AppInfo>>() {
            @Override
            public List<AppInfo> call(List<AppInfo> perAppList, List<AppInfo> installedApps) {
                installedApps.removeAll(perAppList);
                for (int i = 0; i < installedApps.size(); i++) {
                    AppInfo info = installedApps.get(i);
                    info.id = i;
                }
                return installedApps;
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleSubscriber<List<AppInfo>>() {
                    @Override
                    public void onSuccess(List<AppInfo> value) {
                        mAppList = value;

                        mProgressBar.setVisibility(View.GONE);

                        mAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(Throwable error) {
                        error.printStackTrace();
                    }
                });


    }


    private class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

        public AppAdapter() {
            super();
            setHasStableIds(true);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_app, parent, false);
            return new ViewHolder(view);
        }


        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            final AppInfo app = mAppList.get(position);

            Glide.with(AddAppDialogActivity.this)
                    .load(app)
                    .crossFade()
                    .into(holder.icon);


            holder.title.setText(app.name);

            holder.desc.setText(app.packageName);

        }

        @Override
        public int getItemCount() {
            return mAppList.size();
        }

        @Override
        public long getItemId(int position) {
            return mAppList.get(position).id;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView icon;
            TextView title;
            TextView desc;

            public ViewHolder(View itemView) {
                super(itemView);

                icon = (ImageView) itemView.findViewById(R.id.image_app);
                title = (TextView) itemView.findViewById(R.id.text_name);
                desc = (TextView) itemView.findViewById(R.id.text_desc);

                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (getAdapterPosition() != RecyclerView.NO_POSITION) {
                            String table = PrefUtils.isBlacklistMode(AddAppDialogActivity.this) ?
                                    PerAppListActivity.TABLE_BLACKLISTED_APPS : PerAppListActivity.TABLE_WHITELISTED_APPS;
                            AppInfo appInfo = mAppList.get(getAdapterPosition());
                            Inquiry.get().insertInto(table, AppInfo.class)
                                    .values(appInfo)
                                    .run();

                            //appInfo.icon = null;

                            Intent intent = new Intent();
                            intent.putExtra(PerAppListActivity.EXTRA_ADDED, appInfo);
                            setResult(Activity.RESULT_OK, intent);
                            supportFinishAfterTransition();
                        }
                    }
                });
            }
        }
    }

}
