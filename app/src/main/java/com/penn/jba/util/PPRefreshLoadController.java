package com.penn.jba.util;

import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

/**
 * Created by penn on 14/04/2017.
 */

public abstract class PPRefreshLoadController implements SwipeRefreshLayout.OnRefreshListener {
    private boolean loading = false;
    private boolean noMore = false;

    private int visibleThreshold = 1;
    private int lastVisibleItem, totalItemCount;

    public SwipeRefreshLayout swipeRefreshLayout;
    public RecyclerView recyclerView;

    public PPRefreshLoadController(SwipeRefreshLayout swipeRefreshLayout, RecyclerView recyclerView) {
        this.swipeRefreshLayout = swipeRefreshLayout;
        this.swipeRefreshLayout.setOnRefreshListener(this);
        this.recyclerView = recyclerView;

        if (recyclerView.getLayoutManager() instanceof LinearLayoutManager) {

            final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView
                    .getLayoutManager();

            //用setOnScrollListener, 而不用addOnScrollListener, 因为addOnScrollListener和MaterialViewPager插件冲突
            recyclerView
                    .setOnScrollListener(new RecyclerView.OnScrollListener() {
                        @Override
                        public void onScrolled(final RecyclerView recyclerView,
                                               int dx, int dy) {
                            super.onScrolled(recyclerView, dx, dy);

                            totalItemCount = linearLayoutManager.getItemCount();
                            lastVisibleItem = linearLayoutManager
                                    .findLastVisibleItemPosition();

                            if (!noMore && !loading && totalItemCount <= (lastVisibleItem + visibleThreshold)) {
                                // End has been reached
                                // Do something
                                loadMore(recyclerView);
                            }

                        }
                    });
        }
    }

    private void begin() {
        this.loading = true;
    }

    public void end() {
        this.loading = false;
    }

    public void reset() {
        noMore = false;
    }

    public void noMore() {
        noMore = true;
    }

    public abstract void doRefresh();

    @Override
    public void onRefresh() {
        if (!loading) {
            begin();
            doRefresh();
        } else {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    public abstract void doLoadMore();

    public  void loadMore(RecyclerView recyclerView) {
        if (!loading) {
            Log.v("pplog17", "loadMore");
            begin();
            final PPLoadAdapter tmp = ((PPLoadAdapter) (recyclerView.getAdapter()));
            tmp.needLoadMoreCell();
            doLoadMore();
        } else {
            //do nothing
        }
    }
}
