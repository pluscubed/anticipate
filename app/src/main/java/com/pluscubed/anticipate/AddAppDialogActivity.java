package com.pluscubed.anticipate;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.inquiry.Inquiry;
import com.pluscubed.anticipate.transitions.FabDialogMorphSetup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import rx.Single;
import rx.SingleSubscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class AddAppDialogActivity extends AppCompatActivity {

    private static final int PAYLOAD_ICON = 32;

    List<AppPackage> mAppList;
    private AppAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog_add_perapp);

        View viewById = findViewById(R.id.root);
        FabDialogMorphSetup.setupSharedElementTransitions(this, viewById, getResources().getDimensionPixelSize(R.dimen.dialog_corners));

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

        Single.create(new Single.OnSubscribe<List<AppPackage>>() {
            @Override
            public void call(SingleSubscriber<? super List<AppPackage>> singleSubscriber) {
                List<ApplicationInfo> infos = getPackageManager().getInstalledApplications(0);
                Collections.sort(infos, new Comparator<ApplicationInfo>() {
                    @Override
                    public int compare(ApplicationInfo lhs, ApplicationInfo rhs) {
                        return lhs.loadLabel(getPackageManager()).toString()
                                .compareTo(rhs.loadLabel(getPackageManager()).toString());
                    }
                });
                for (int i = 0, infosSize = infos.size(); i < infosSize; i++) {
                    ApplicationInfo info = infos.get(i);
                    AppPackage appPackage = new AppPackage();
                    appPackage.id = i;
                    appPackage.package_name = info.packageName;
                    mAppList.add(appPackage);
                }
            }
        }).subscribeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleSubscriber<List<AppPackage>>() {
                    @Override
                    public void onSuccess(List<AppPackage> value) {
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
        public void onBindViewHolder(ViewHolder holder, int position, List<Object> payloads) {
            if (payloads.contains(PAYLOAD_ICON)) {
                holder.icon.setImageDrawable(mAppList.get(position).icon);
            } else {
                onBindViewHolder(holder, position);
            }
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            final AppPackage app = mAppList.get(position);

            try {
                final ApplicationInfo info = getPackageManager().getApplicationInfo(app.package_name, 0);

                if (app.icon == null) {
                    holder.icon.setImageDrawable(null);
                    Single.create(new Single.OnSubscribe<Drawable>() {
                        @Override
                        public void call(SingleSubscriber<? super Drawable> singleSubscriber) {
                            singleSubscriber.onSuccess(info.loadIcon(getPackageManager()));
                        }
                    }).subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new SingleSubscriber<Drawable>() {
                                @Override
                                public void onSuccess(Drawable value) {
                                    app.icon = value;
                                    notifyItemChanged(mAppList.indexOf(app), PAYLOAD_ICON);
                                }

                                @Override
                                public void onError(Throwable error) {

                                }
                            });
                } else {
                    holder.icon.setImageDrawable(app.icon);
                }
                holder.title.setText(info.loadLabel(getPackageManager()));
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            holder.desc.setText(app.package_name);

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
                            Inquiry.get().insertInto(table, AppPackage.class)
                                    .values(mAppList.get(getAdapterPosition()))
                                    .run();
                        }
                    }
                });
            }
        }
    }

}
