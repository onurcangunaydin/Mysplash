package com.wangdaye.mysplash.main.presenter.widget;

import android.content.Context;
import android.support.design.widget.Snackbar;

import com.wangdaye.mysplash.Mysplash;
import com.wangdaye.mysplash.R;
import com.wangdaye.mysplash._common.data.entity.unsplash.Collection;
import com.wangdaye.mysplash._common.data.service.CollectionService;
import com.wangdaye.mysplash._common.i.model.CollectionsModel;
import com.wangdaye.mysplash._common.i.presenter.CollectionsPresenter;
import com.wangdaye.mysplash._common.i.view.CollectionsView;
import com.wangdaye.mysplash._common.ui._basic.MysplashActivity;
import com.wangdaye.mysplash._common.ui.adapter.CollectionAdapter;
import com.wangdaye.mysplash._common.utils.NotificationUtils;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

/**
 * Collections implementor.
 * */

public class CollectionsImplementor
        implements CollectionsPresenter {
    // model & view.
    private CollectionsModel model;
    private CollectionsView view;

    // data
    private OnRequestCollectionsListener listener;

    /** <br> life cycle. */

    public CollectionsImplementor(CollectionsModel model, CollectionsView view) {
        this.model = model;
        this.view = view;
    }

    /** <br> presenter. */

    @Override
    public void requestCollections(Context c, int page, boolean refresh) {
        if (!model.isRefreshing() && !model.isLoading()) {
            if (refresh) {
                model.setRefreshing(true);
            } else {
                model.setLoading(true);
            }
            final String[] types = c
                    .getResources()
                    .getStringArray(R.array.collection_type_values);
            if (model.getCollectionsType().equals(types[0])) {
                requestAllCollections(c, page, refresh);
            } else if (model.getCollectionsType().equals(types[1])) {
                requestCuratedCollections(c, page, refresh);
            } else {
                requestFeaturedCollections(c, page, refresh);
            }
        }
    }

    @Override
    public void cancelRequest() {
        if (listener != null) {
            listener.cancel();
        }
        model.getService().cancel();
        model.setRefreshing(false);
        model.setLoading(false);
    }

    @Override
    public void refreshNew(Context c, boolean notify) {
        if (notify) {
            view.setRefreshing(true);
        }
        requestCollections(c, model.getCollectionsPage(), true);
    }

    @Override
    public void loadMore(Context c, boolean notify) {
        if (notify) {
            view.setLoading(true);
        }
        requestCollections(c, model.getCollectionsPage(), false);
    }

    @Override
    public void initRefresh(Context c) {
        cancelRequest();
        refreshNew(c, false);
        view.initRefreshStart();
    }

    @Override
    public boolean canLoadMore() {
        return !model.isRefreshing() && !model.isLoading() && !model.isOver();
    }

    @Override
    public boolean isRefreshing() {
        return model.isRefreshing();
    }

    @Override
    public boolean isLoading() {
        return model.isLoading();
    }

    @Override
    public Object getRequestKey() {
        return null;
    }

    @Override
    public void setRequestKey(Object k) {
        // do nothing.
    }

    @Override
    public void setType(String key) {
        model.setCollectionsType(key);
    }

    @Override
    public String getType() {
        return model.getCollectionsType();
    }

    @Override
    public void setActivityForAdapter(MysplashActivity a) {
        model.getAdapter().setActivity(a);
    }

    @Override
    public CollectionAdapter getAdapter() {
        return model.getAdapter();
    }

    /** <br> utils. */

    private void requestAllCollections(Context c, int page, boolean refresh) {
        page = refresh ? 1 : page + 1;
        listener = new OnRequestCollectionsListener(c, page, refresh);
        model.getService()
                .requestAllCollections(
                        page,
                        Mysplash.DEFAULT_PER_PAGE,
                        listener);
    }

    private void requestCuratedCollections(Context c, int page, boolean refresh) {
        page = refresh ? 1 : page + 1;
        listener = new OnRequestCollectionsListener(c, page, refresh);
        model.getService()
                .requestCuratedCollections(
                        page,
                        Mysplash.DEFAULT_PER_PAGE,
                        listener);
    }

    private void requestFeaturedCollections(Context c, int page, boolean refresh) {
        page = refresh ? 1 : page + 1;
        model.getService()
                .requestFeaturedCollections(
                        page,
                        Mysplash.DEFAULT_PER_PAGE,
                        new OnRequestCollectionsListener(c, page, refresh));
    }

    /** <br> interface. */

    private class OnRequestCollectionsListener implements CollectionService.OnRequestCollectionsListener {
        // data
        private Context c;
        private int page;
        private boolean refresh;
        private boolean canceled;

        OnRequestCollectionsListener(Context c, int page, boolean refresh) {
            this.c = c;
            this.page = page;
            this.refresh = refresh;
            this.canceled = false;
        }

        public void cancel() {
            canceled = true;
        }

        @Override
        public void onRequestCollectionsSuccess(Call<List<Collection>> call, Response<List<Collection>> response) {
            if (canceled) {
                return;
            }

            model.setRefreshing(false);
            model.setLoading(false);
            if (refresh) {
                model.getAdapter().clearItem();
                model.setOver(false);
                view.setRefreshing(false);
                view.setPermitLoading(true);
            } else {
                view.setLoading(false);
            }
            if (response.isSuccessful()
                    && model.getAdapter().getRealItemCount() + response.body().size() > 0) {
                model.setCollectionsPage(page);
                for (int i = 0; i < response.body().size(); i ++) {
                    model.getAdapter().insertItem(response.body().get(i), model.getAdapter().getRealItemCount());
                }
                if (response.body().size() < Mysplash.DEFAULT_PER_PAGE) {
                    model.setOver(true);
                    view.setPermitLoading(false);
                }
                view.requestCollectionsSuccess();
            } else {
                view.requestCollectionsFailed(c.getString(R.string.feedback_load_nothing_tv));
            }
        }

        @Override
        public void onRequestCollectionsFailed(Call<List<Collection>> call, Throwable t) {
            if (canceled) {
                return;
            }
            model.setRefreshing(false);
            model.setLoading(false);
            if (refresh) {
                view.setRefreshing(false);
            } else {
                view.setLoading(false);
            }
            NotificationUtils.showSnackbar(
                    c.getString(R.string.feedback_load_failed_toast) + " (" + t.getMessage() + ")",
                    Snackbar.LENGTH_SHORT);
            view.requestCollectionsFailed(c.getString(R.string.feedback_load_failed_tv));
        }
    }
}
