package com.maple.leaf.widget.calendar;

/**
 * Created by qiaopengli on 2017/12/5.
 */
public interface OnCalendarClickListener {
    void onClickDate(int year, int month, int day);
    void onPageChange(int year, int month, int day);
}
