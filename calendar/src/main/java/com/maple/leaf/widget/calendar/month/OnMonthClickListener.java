package com.maple.leaf.widget.calendar.month;

/**
 * Created by qiaopengli on 2017/12/5.
 */
public interface OnMonthClickListener {
    void onClickThisMonth(int year, int month, int day);
    void onClickLastMonth(int year, int month, int day);
    void onClickNextMonth(int year, int month, int day);
}
