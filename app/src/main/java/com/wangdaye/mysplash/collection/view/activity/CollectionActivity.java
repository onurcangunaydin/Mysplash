package com.wangdaye.mysplash.collection.view.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.wangdaye.mysplash.Mysplash;
import com.wangdaye.mysplash.R;
import com.wangdaye.mysplash._common.data.entity.unsplash.Photo;
import com.wangdaye.mysplash._common.i.model.DownloadModel;
import com.wangdaye.mysplash._common.i.presenter.DownloadPresenter;
import com.wangdaye.mysplash._common.ui.adapter.PhotoAdapter;
import com.wangdaye.mysplash._common.ui.widget.nestedScrollView.NestedScrollAppBarLayout;
import com.wangdaye.mysplash._common.ui.widget.SwipeBackCoordinatorLayout;
import com.wangdaye.mysplash._common.utils.DisplayUtils;
import com.wangdaye.mysplash._common.utils.NotificationUtils;
import com.wangdaye.mysplash._common.utils.helper.DownloadHelper;
import com.wangdaye.mysplash._common.utils.helper.IntentHelper;
import com.wangdaye.mysplash._common.utils.manager.AuthManager;
import com.wangdaye.mysplash._common.i.model.BrowsableModel;
import com.wangdaye.mysplash._common.i.model.EditResultModel;
import com.wangdaye.mysplash._common.i.presenter.BrowsablePresenter;
import com.wangdaye.mysplash._common.i.presenter.EditResultPresenter;
import com.wangdaye.mysplash._common.i.presenter.SwipeBackManagePresenter;
import com.wangdaye.mysplash._common.i.view.BrowsableView;
import com.wangdaye.mysplash._common.i.view.EditResultView;
import com.wangdaye.mysplash._common.i.view.SwipeBackManageView;
import com.wangdaye.mysplash._common.ui.dialog.RequestBrowsableDataDialog;
import com.wangdaye.mysplash._common.ui.dialog.UpdateCollectionDialog;
import com.wangdaye.mysplash._common.utils.AnimUtils;
import com.wangdaye.mysplash._common.utils.BackToTopUtils;
import com.wangdaye.mysplash.collection.model.activity.BorwsableObject;
import com.wangdaye.mysplash.collection.model.activity.DownloadObject;
import com.wangdaye.mysplash.collection.model.activity.EditResultObject;
import com.wangdaye.mysplash.collection.presenter.activity.BrowsableImplementor;
import com.wangdaye.mysplash.collection.presenter.activity.DownloadImplementor;
import com.wangdaye.mysplash.collection.presenter.activity.EditResultImplementor;
import com.wangdaye.mysplash.collection.presenter.activity.SwipeBackManageImplementor;
import com.wangdaye.mysplash.collection.presenter.activity.ToolbarImplementor;
import com.wangdaye.mysplash.collection.view.widget.CollectionPhotosView;
import com.wangdaye.mysplash._common.data.entity.unsplash.Collection;
import com.wangdaye.mysplash._common.i.presenter.ToolbarPresenter;
import com.wangdaye.mysplash._common.ui._basic.MysplashActivity;
import com.wangdaye.mysplash._common.ui.widget.CircleImageView;
import com.wangdaye.mysplash._common.ui.widget.coordinatorView.StatusBarView;
import com.wangdaye.mysplash.main.view.activity.MainActivity;
import com.wangdaye.mysplash.me.view.activity.MeActivity;
import com.wangdaye.mysplash.user.view.activity.UserActivity;

/**
 * Collection activity.
 * */

public class CollectionActivity extends MysplashActivity
        implements SwipeBackManageView, EditResultView, BrowsableView,
        View.OnClickListener, Toolbar.OnMenuItemClickListener, PhotoAdapter.OnDownloadPhotoListener,
        SwipeBackCoordinatorLayout.OnSwipeListener, UpdateCollectionDialog.OnCollectionChangedListener {
    // model.
    private EditResultModel editResultModel;
    private BrowsableModel browsableModel;
    private DownloadModel downloadModel;

    // view.
    private RequestBrowsableDataDialog requestDialog;

    private CoordinatorLayout container;
    private NestedScrollAppBarLayout appBar;
    private RelativeLayout creatorBar;
    private CircleImageView avatarImage;
    private StatusBarView statusBar;
    private CollectionPhotosView photosView;
    
    // presenter.
    private ToolbarPresenter toolbarPresenter;
    private SwipeBackManagePresenter swipeBackManagePresenter;
    private EditResultPresenter editResultPresenter;
    private BrowsablePresenter browsablePresenter;
    private DownloadPresenter downloadPresenter;

    // data
    public static final String KEY_COLLECTION_ACTIVITY_COLLECTION = "collection_activity_collection";
    public static final String KEY_COLLECTION_ACTIVITY_ID = "collection_activity_id";

    /** <br> life cycle. */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection);
        initModel();
        initPresenter();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!isStarted()) {
            setStarted();
            initView(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        browsablePresenter.cancelRequest();
        if (photosView != null) {
            photosView.cancelRequest();
        }
    }

    @Override
    protected void setTheme() {
        if (Mysplash.getInstance().isLightTheme()) {
            setTheme(R.style.MysplashTheme_light_Translucent_Collection);
        } else {
            setTheme(R.style.MysplashTheme_dark_Translucent_Collection);
        }
    }

    @Override
    public void handleBackPressed() {
        if (photosView.needPagerBackToTop()
                && BackToTopUtils.isSetBackToTop(false)) {
            backToTop();
        } else {
            finishActivity(SwipeBackCoordinatorLayout.DOWN_DIR);
        }
    }

    @Override
    public void backToTop() {
        BackToTopUtils.showTopBar(appBar, photosView);
        photosView.pagerBackToTop();
    }

    @Override
    protected boolean needSetStatusBarTextDark() {
        return true;
    }

    @Override
    public void finishActivity(int dir) {
        Intent result = new Intent();
        result.putExtra(MeActivity.KEY_ME_ACTIVITY_DELETE_COLLECTION, editResultPresenter.getEditKey() == null);
        result.putExtra(MeActivity.KEY_ME_ACTIVITY_COLLECTION, getIntent().getParcelableExtra(KEY_COLLECTION_ACTIVITY_COLLECTION));
        setResult(RESULT_OK, result);
        SwipeBackCoordinatorLayout.hideBackgroundShadow(container);
        finish();
        switch (dir) {
            case SwipeBackCoordinatorLayout.UP_DIR:
                overridePendingTransition(0, R.anim.activity_slide_out_top);
                break;

            case SwipeBackCoordinatorLayout.DOWN_DIR:
                overridePendingTransition(0, R.anim.activity_slide_out_bottom);
                break;
        }
    }

    @Override
    public View getSnackbarContainer() {
        return container;
    }

    /** <br> presenter. */

    private void initPresenter() {
        this.toolbarPresenter = new ToolbarImplementor();
        this.swipeBackManagePresenter = new SwipeBackManageImplementor(this);
        this.editResultPresenter = new EditResultImplementor(editResultModel, this);
        this.browsablePresenter = new BrowsableImplementor(browsableModel, this);
        this.downloadPresenter = new DownloadImplementor(downloadModel);
    }

    /** <br> view. */

    // init.

    @SuppressLint("SetTextI18n")
    private void initView(boolean init) {
        if (init && browsablePresenter.isBrowsable()) {
            browsablePresenter.requestBrowsableData();
        } else {
            Collection c = (Collection) editResultPresenter.getEditKey();

            this.container = (CoordinatorLayout) findViewById(R.id.activity_collection_container);

            SwipeBackCoordinatorLayout swipeBackView
                    = (SwipeBackCoordinatorLayout) findViewById(R.id.activity_collection_swipeBackView);
            swipeBackView.setOnSwipeListener(this);

            this.statusBar = (StatusBarView) findViewById(R.id.activity_collection_statusBar);
            if (DisplayUtils.isNeedSetStatusBarMask()) {
                statusBar.setBackgroundResource(R.color.colorPrimary_light);
                statusBar.setMask(true);
            }

            this.appBar = (NestedScrollAppBarLayout) findViewById(R.id.activity_collection_appBar);

            TextView title = (TextView) findViewById(R.id.activity_collection_title);
            title.setText(c.title);

            TextView description = (TextView) findViewById(R.id.activity_collection_description);
            if (TextUtils.isEmpty(c.description)) {
                description.setVisibility(View.GONE);
            } else {
                DisplayUtils.setTypeface(this, description);
                description.setText(c.description);
            }

            Toolbar toolbar = (Toolbar) findViewById(R.id.activity_collection_toolbar);
            if (Mysplash.getInstance().isLightTheme()) {
                if (browsablePresenter.isBrowsable()) {
                    toolbar.setNavigationIcon(R.drawable.ic_toolbar_home_light);
                } else {
                    toolbar.setNavigationIcon(R.drawable.ic_toolbar_back_light);
                }
                toolbar.inflateMenu(R.menu.activity_collection_toolbar_light);
                toolbar.setOnMenuItemClickListener(this);
            } else {
                if (browsablePresenter.isBrowsable()) {
                    toolbar.setNavigationIcon(R.drawable.ic_toolbar_home_dark);
                } else {
                    toolbar.setNavigationIcon(R.drawable.ic_toolbar_back_dark);
                }
                toolbar.inflateMenu(R.menu.activity_collection_toolbar_dark);
                toolbar.setOnMenuItemClickListener(this);
            }
            toolbar.setNavigationOnClickListener(this);
            if (AuthManager.getInstance().getUsername() != null
                    && AuthManager.getInstance().getUsername().equals(c.user.username)) {
                toolbar.getMenu().getItem(0).setVisible(true);
            } else {
                toolbar.getMenu().getItem(0).setVisible(false);
            }
            if (c.curated) {
                toolbar.getMenu().getItem(2).setVisible(true);
            } else {
                toolbar.getMenu().getItem(2).setVisible(false);
            }

            this.creatorBar = (RelativeLayout) findViewById(R.id.activity_collection_creatorBar);
            creatorBar.setOnClickListener(this);

            findViewById(R.id.activity_collection_touchBar).setOnClickListener(this);

            this.avatarImage = (CircleImageView) findViewById(R.id.activity_collection_avatar);
            Glide.with(this)
                    .load(c.user.profile_image.large)
                    .priority(Priority.HIGH)
                    .override(128, 128)
                    .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                    .into(avatarImage);

            TextView subtitle = (TextView) findViewById(R.id.activity_collection_subtitle);
            DisplayUtils.setTypeface(this, subtitle);
            subtitle.setText(getString(R.string.by) + " " + c.user.name);

            this.photosView = (CollectionPhotosView) findViewById(R.id.activity_collection_photosView);
            photosView.initMP(this, (Collection) editResultPresenter.getEditKey());
            photosView.initRefresh();

            AnimUtils.animInitShow(photosView, 400);
        }
    }

    /** <br> model. */

    // init.

    private void initModel() {
        this.editResultModel = new EditResultObject(
                (Collection) getIntent().getParcelableExtra(KEY_COLLECTION_ACTIVITY_COLLECTION));
        this.browsableModel = new BorwsableObject(getIntent());
        this.downloadModel = new DownloadObject();
    }

    // interface.

    public Collection getCollection() {
        return (Collection) editResultPresenter.getEditKey();
    }

    public void downloadCollection() {
        downloadPresenter.setDownloadKey(getCollection());
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            downloadPresenter.download();
        } else {
            requestPermission(Mysplash.WRITE_EXTERNAL_STORAGE, 2);
        }
    }

    /** <br> permission. */

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void requestPermission(int permissionCode, int requestCode) {
        switch (permissionCode) {
            case Mysplash.WRITE_EXTERNAL_STORAGE:
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    this.requestPermissions(
                            new String[] {
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            requestCode);
                } else {
                    downloadPresenter.download();
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permission, @NonNull int[] grantResult) {
        super.onRequestPermissionsResult(requestCode, permission, grantResult);
        for (int i = 0; i < permission.length; i ++) {
            switch (permission[i]) {
                case Manifest.permission.WRITE_EXTERNAL_STORAGE:
                    if (grantResult[i] == PackageManager.PERMISSION_GRANTED) {
                        downloadPresenter.download();
                    } else {
                        NotificationUtils.showSnackbar(
                                getString(R.string.feedback_need_permission),
                                Snackbar.LENGTH_SHORT);
                    }
                    break;
            }
        }
    }

    /** <br> interface. */

    // on click listener.

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case -1:
                if (browsablePresenter.isBrowsable()) {
                    browsablePresenter.visitParentView();
                }
                toolbarPresenter.touchNavigatorIcon(this);
                break;

            case R.id.activity_collection_creatorBar:
                toolbarPresenter.touchToolbar(this);
                break;

            case R.id.activity_collection_touchBar:
                IntentHelper.startUserActivity(
                        this,
                        avatarImage,
                        ((Collection) editResultPresenter.getEditKey()).user,
                        UserActivity.PAGE_PHOTO);
                break;
        }
    }

    // on menu item click listener.

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        return toolbarPresenter.touchMenuItem(this, item.getItemId());
    }

    // on download photo listener. (photo adapter)

    @Override
    public void onDownload(Photo photo) {
        downloadPresenter.setDownloadKey(photo);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            downloadPresenter.download();
        } else {
            requestPermission(Mysplash.WRITE_EXTERNAL_STORAGE, DownloadHelper.DOWNLOAD_TYPE);
        }
    }

    // on swipe listener.

    @Override
    public boolean canSwipeBack(int dir) {
        return swipeBackManagePresenter.checkCanSwipeBack(dir);
    }

    @Override
    public void onSwipeProcess(float percent) {
        statusBar.setAlpha(1 - percent);
        container.setBackgroundColor(SwipeBackCoordinatorLayout.getBackgroundColor(percent));
    }

    @Override
    public void onSwipeFinish(int dir) {
        swipeBackManagePresenter.swipeBackFinish(this, dir);
    }

    // on collection changed listener.

    @Override
    public void onEditCollection(Collection c) {
        AuthManager.getInstance().getCollectionsManager().updateCollection(c);
        editResultPresenter.updateSomething(c);
    }

    @Override
    public void onDeleteCollection(Collection c) {
        AuthManager.getInstance().getCollectionsManager().deleteCollection(c);
        editResultPresenter.deleteSomething(c);
    }

    // view.

    // swipe back manage view.

    @Override
    public boolean checkCanSwipeBack(int dir) {
        if (dir == SwipeBackCoordinatorLayout.UP_DIR) {
            return photosView.canSwipeBack(dir)
                    && appBar.getY() <= -appBar.getMeasuredHeight() + creatorBar.getMeasuredHeight();
        } else {
            return photosView.canSwipeBack(dir)
                    && appBar.getY() >= 0;
        }
    }

    // edit result view.

    @Override
    public void drawCreateResult(Object newKey) {
        // do nothing.
    }

    @Override
    public void drawUpdateResult(Object newKey) {
        Collection c = (Collection) newKey;

        TextView title = (TextView) findViewById(R.id.activity_collection_title);
        title.setText(c.title);

        TextView description = (TextView) findViewById(R.id.activity_collection_description);
        if (TextUtils.isEmpty(c.description)) {
            description.setVisibility(View.GONE);
        } else {
            DisplayUtils.setTypeface(this, description);
            description.setText(c.description);
        }
    }

    @Override
    public void drawDeleteResult(Object oldKey) {
        editResultPresenter.setEditKey(null);
        finishActivity(SwipeBackCoordinatorLayout.NULL_DIR);
    }

    // browsable view.

    @Override
    public void showRequestDialog() {
        requestDialog = new RequestBrowsableDataDialog();
        requestDialog.show(getFragmentManager(), null);
    }

    @Override
    public void dismissRequestDialog() {
        requestDialog.dismiss();
        requestDialog = null;
    }

    @Override
    public void drawBrowsableView() {
        initModel();
        initPresenter();
        initView(false);
    }

    @Override
    public void visitParentView() {
        startActivity(new Intent(this, MainActivity.class));
    }
}
