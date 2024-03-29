package com.roadstar.customerr.common.base.recycler_view;

/**
 * Click listener for recycler view
 */
public interface OnRecyclerViewItemClickListener {

    /**
     * @param holder view holder on clicked item
     */
    void onRecyclerViewItemClick(BaseRecyclerViewHolder holder);

    /**
     * @param holder     view holder on clicked item
     * @param resourceId resource id of clicked item
     */
    void onRecyclerViewChildItemClick(BaseRecyclerViewHolder holder, int resourceId);
}
