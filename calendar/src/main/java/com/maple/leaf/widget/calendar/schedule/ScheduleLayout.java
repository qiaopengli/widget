package com.maple.leaf.widget.calendar.schedule;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.maple.leaf.widget.calendar.CalendarUtils;
import com.maple.leaf.widget.calendar.OnCalendarClickListener;
import com.maple.leaf.widget.calendar.R;
import com.maple.leaf.widget.calendar.month.MonthCalendarView;
import com.maple.leaf.widget.calendar.month.MonthView;
import com.maple.leaf.widget.calendar.week.WeekCalendarView;
import com.maple.leaf.widget.calendar.week.WeekView;

import org.joda.time.DateTime;

import java.util.Calendar;
import java.util.List;

/**
 * Created by qiaopengli on 2017/12/5.
 */
public class ScheduleLayout extends FrameLayout {

    private final int DEFAULT_MONTH = 0;
    private final int DEFAULT_WEEK = 1;

    private MonthCalendarView monthCalendarView;
    private WeekCalendarView weekCalendarView;
    private RelativeLayout monthCalendarParent;
    private RelativeLayout scheduleContentParent;
    private ScheduleRecyclerView scheduleRecyclerView;

    private int mCurrentSelectYear;
    private int mCurrentSelectMonth;
    private int mCurrentSelectDay;
    private int mRowSize;
    private int mMinDistance;
    private int mAutoScrollDistance;
    private int mDefaultView;
    private float mDownPosition[] = new float[2];
    private boolean mIsScrolling = false;
    private boolean mIsAutoChangeMonthRow;
    private boolean mCurrentRowsIsSix = true;

    private ScheduleState mState;
    private OnCalendarClickListener mOnCalendarClickListener;
    private GestureDetector mGestureDetector;

    private int mIdMonthCalendarView = 0;
    private int mIdWeekCalendarView = 0;
    private int mIdMonthCalendarParent = 0;
    private int mIdScheduleContentParent = 0;
    private int mIdScheduleRecyclerView = 0;

    public ScheduleLayout(Context context) {
        this(context, null);
    }

    public ScheduleLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScheduleLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context.obtainStyledAttributes(attrs, R.styleable.ScheduleLayout));
        initDate();
        initGestureDetector();
    }

    private void initAttrs(TypedArray array) {
        mDefaultView = array.getInt(R.styleable.ScheduleLayout_default_view, DEFAULT_MONTH);
        mIsAutoChangeMonthRow = array.getBoolean(R.styleable.ScheduleLayout_auto_change_month_row, false);

        //child view ids
        mIdMonthCalendarParent = array.getResourceId(R.styleable.ScheduleLayout_month_layout, -1);
        mIdMonthCalendarView = array.getResourceId(R.styleable.ScheduleLayout_month_calendar, -1);
        mIdWeekCalendarView = array.getResourceId(R.styleable.ScheduleLayout_week_calendar, -1);
        mIdScheduleContentParent = array.getResourceId(R.styleable.ScheduleLayout_content_layout, -1);
        mIdScheduleRecyclerView = array.getResourceId(R.styleable.ScheduleLayout_content_recycler_view, -1);

        array.recycle();
        mState = ScheduleState.OPEN;
        mRowSize = getResources().getDimensionPixelSize(R.dimen.week_calendar_height);
        mMinDistance = getResources().getDimensionPixelSize(R.dimen.calendar_min_distance);
        mAutoScrollDistance = getResources().getDimensionPixelSize(R.dimen.auto_scroll_distance);
    }

    private void initGestureDetector() {
        mGestureDetector = new GestureDetector(getContext(), new OnScheduleScrollListener(this));
    }

    private void initDate() {
        Calendar calendar = Calendar.getInstance();
        resetCurrentSelectDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        monthCalendarParent = findViewById(mIdMonthCalendarParent);
        monthCalendarView = findViewById(mIdMonthCalendarView);
        weekCalendarView = findViewById(mIdWeekCalendarView);
        scheduleContentParent = findViewById(mIdScheduleContentParent);
        scheduleRecyclerView = findViewById(mIdScheduleRecyclerView);
        bindingMonthAndWeekCalendar();
    }

    private void bindingMonthAndWeekCalendar() {
        monthCalendarView.setOnCalendarClickListener(mMonthCalendarClickListener);
        weekCalendarView.setOnCalendarClickListener(mWeekCalendarClickListener);
        // 初始化视图
        Calendar calendar = Calendar.getInstance();
        if (mIsAutoChangeMonthRow) {
            mCurrentRowsIsSix = CalendarUtils.getMonthRows(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH)) == 6;
        }
        if (mDefaultView == DEFAULT_MONTH) {
            weekCalendarView.setVisibility(INVISIBLE);
            mState = ScheduleState.OPEN;
            if (!mCurrentRowsIsSix) {
                scheduleContentParent.setY(scheduleContentParent.getY() - mRowSize);
            }
        } else if (mDefaultView == DEFAULT_WEEK) {
            weekCalendarView.setVisibility(VISIBLE);
            mState = ScheduleState.CLOSE;
            int row = CalendarUtils.getWeekRow(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            monthCalendarParent.setY(-row * mRowSize);
            scheduleContentParent.setY(scheduleContentParent.getY() - 5 * mRowSize);
        }
    }

    private void resetCurrentSelectDate(int year, int month, int day) {
        mCurrentSelectYear = year;
        mCurrentSelectMonth = month;
        mCurrentSelectDay = day;
    }

    private OnCalendarClickListener mMonthCalendarClickListener = new OnCalendarClickListener() {
        @Override
        public void onClickDate(int year, int month, int day) {
            weekCalendarView.setOnCalendarClickListener(null);
            int weeks = CalendarUtils.getWeeksAgo(mCurrentSelectYear, mCurrentSelectMonth, mCurrentSelectDay, year, month, day);
            resetCurrentSelectDate(year, month, day);
            int position = weekCalendarView.getCurrentItem() + weeks;
            if (weeks != 0) {
                weekCalendarView.setCurrentItem(position, false);
            }
            resetWeekView(position);
            weekCalendarView.setOnCalendarClickListener(mWeekCalendarClickListener);
        }

        @Override
        public void onPageChange(int year, int month, int day) {
            computeCurrentRowsIsSix(year, month);
        }
    };

    private void computeCurrentRowsIsSix(int year, int month) {
        if (mIsAutoChangeMonthRow) {
            boolean isSixRow = CalendarUtils.getMonthRows(year, month) == 6;
            if (mCurrentRowsIsSix != isSixRow) {
                mCurrentRowsIsSix = isSixRow;
                if (mState == ScheduleState.OPEN) {
                    if (mCurrentRowsIsSix) {
                        AutoMoveAnimation animation = new AutoMoveAnimation(scheduleContentParent, mRowSize);
                        scheduleContentParent.startAnimation(animation);
                    } else {
                        AutoMoveAnimation animation = new AutoMoveAnimation(scheduleContentParent, -mRowSize);
                        scheduleContentParent.startAnimation(animation);
                    }
                }
            }
        }
    }

    private void resetWeekView(int position) {
        WeekView weekView = weekCalendarView.getCurrentWeekView();
        if (weekView != null) {
            weekView.setSelectYearMonth(mCurrentSelectYear, mCurrentSelectMonth, mCurrentSelectDay);
            weekView.invalidate();
        } else {
            WeekView newWeekView = weekCalendarView.getWeekAdapter().instanceWeekView(position);
            newWeekView.setSelectYearMonth(mCurrentSelectYear, mCurrentSelectMonth, mCurrentSelectDay);
            newWeekView.invalidate();
            weekCalendarView.setCurrentItem(position);
        }
        if (mOnCalendarClickListener != null) {
            mOnCalendarClickListener.onClickDate(mCurrentSelectYear, mCurrentSelectMonth, mCurrentSelectDay);
        }
    }

    private OnCalendarClickListener mWeekCalendarClickListener = new OnCalendarClickListener() {
        @Override
        public void onClickDate(int year, int month, int day) {
            monthCalendarView.setOnCalendarClickListener(null);
            int months = CalendarUtils.getMonthsAgo(mCurrentSelectYear, mCurrentSelectMonth, year, month);
            resetCurrentSelectDate(year, month, day);
            if (months != 0) {
                int position = monthCalendarView.getCurrentItem() + months;
                monthCalendarView.setCurrentItem(position, false);
            }
            resetMonthView();
            monthCalendarView.setOnCalendarClickListener(mMonthCalendarClickListener);
            if (mIsAutoChangeMonthRow) {
                mCurrentRowsIsSix = CalendarUtils.getMonthRows(year, month) == 6;
            }
        }

        @Override
        public void onPageChange(int year, int month, int day) {
            if (mIsAutoChangeMonthRow) {
                if (mCurrentSelectMonth != month) {
                    mCurrentRowsIsSix = CalendarUtils.getMonthRows(year, month) == 6;
                }
            }
        }
    };

    private void resetMonthView() {
        MonthView monthView = monthCalendarView.getCurrentMonthView();
        if (monthView != null) {
            monthView.setSelectYearMonth(mCurrentSelectYear, mCurrentSelectMonth, mCurrentSelectDay);
            monthView.invalidate();
        }
        if (mOnCalendarClickListener != null) {
            mOnCalendarClickListener.onClickDate(mCurrentSelectYear, mCurrentSelectMonth, mCurrentSelectDay);
        }
        resetCalendarPosition();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = MeasureSpec.getSize(heightMeasureSpec);
        resetViewHeight(scheduleContentParent, height - mRowSize);
        resetViewHeight(this, height);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void resetViewHeight(View view, int height) {
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        if (layoutParams.height != height) {
            layoutParams.height = height;
            view.setLayoutParams(layoutParams);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mDownPosition[0] = ev.getRawX();
                mDownPosition[1] = ev.getRawY();
                mGestureDetector.onTouchEvent(ev);
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (mIsScrolling) {
            return true;
        }
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
                float x = ev.getRawX();
                float y = ev.getRawY();
                float distanceX = Math.abs(x - mDownPosition[0]);
                float distanceY = Math.abs(y - mDownPosition[1]);
                if (distanceY > mMinDistance && distanceY > distanceX * 2.0f) {
                    return (y > mDownPosition[1] && isRecyclerViewTouch()) || (y < mDownPosition[1] && mState == ScheduleState.OPEN);
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    private boolean isRecyclerViewTouch() {
        return mState == ScheduleState.CLOSE && (scheduleRecyclerView.getChildCount() == 0 || scheduleRecyclerView.isScrollTop());
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mDownPosition[0] = event.getRawX();
                mDownPosition[1] = event.getRawY();
                resetCalendarPosition();
                return true;
            case MotionEvent.ACTION_MOVE:
                transferEvent(event);
                mIsScrolling = true;
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                transferEvent(event);
                changeCalendarState();
                resetScrollingState();
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void transferEvent(MotionEvent event) {
        if (mState == ScheduleState.CLOSE) {
            monthCalendarView.setVisibility(VISIBLE);
            weekCalendarView.setVisibility(INVISIBLE);
            mGestureDetector.onTouchEvent(event);
        } else {
            mGestureDetector.onTouchEvent(event);
        }
    }

    private void changeCalendarState() {
        if (scheduleContentParent.getY() > mRowSize * 2 &&
                scheduleContentParent.getY() < monthCalendarView.getHeight() - mRowSize) { // 位于中间
            ScheduleAnimation animation = new ScheduleAnimation(this, mState, mAutoScrollDistance);
            animation.setDuration(300);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    changeState();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            scheduleContentParent.startAnimation(animation);
        } else if (scheduleContentParent.getY() <= mRowSize * 2) { // 位于顶部
            ScheduleAnimation animation = new ScheduleAnimation(this, ScheduleState.OPEN, mAutoScrollDistance);
            animation.setDuration(50);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (mState == ScheduleState.OPEN) {
                        changeState();
                    } else {
                        resetCalendar();
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            scheduleContentParent.startAnimation(animation);
        } else {
            ScheduleAnimation animation = new ScheduleAnimation(this, ScheduleState.CLOSE, mAutoScrollDistance);
            animation.setDuration(50);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (mState == ScheduleState.CLOSE) {
                        mState = ScheduleState.OPEN;
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            scheduleContentParent.startAnimation(animation);
        }
    }

    private void resetCalendarPosition() {
        if (mState == ScheduleState.OPEN) {
            monthCalendarParent.setY(0);
            if (mCurrentRowsIsSix) {
                scheduleContentParent.setY(monthCalendarView.getHeight());
            } else {
                scheduleContentParent.setY(monthCalendarView.getHeight() - mRowSize);
            }
        } else {
            monthCalendarParent.setY(-CalendarUtils.getWeekRow(mCurrentSelectYear, mCurrentSelectMonth, mCurrentSelectDay) * mRowSize);
            scheduleContentParent.setY(mRowSize);
        }
    }

    private void resetCalendar() {
        if (mState == ScheduleState.OPEN) {
            monthCalendarView.setVisibility(VISIBLE);
            weekCalendarView.setVisibility(INVISIBLE);
        } else {
            monthCalendarView.setVisibility(INVISIBLE);
            weekCalendarView.setVisibility(VISIBLE);
        }
    }

    private void changeState() {
        if (mState == ScheduleState.OPEN) {
            mState = ScheduleState.CLOSE;
            monthCalendarView.setVisibility(INVISIBLE);
            weekCalendarView.setVisibility(VISIBLE);
            monthCalendarParent.setY((1 - monthCalendarView.getCurrentMonthView().getWeekRow()) * mRowSize);
            checkWeekCalendar();
        } else {
            mState = ScheduleState.OPEN;
            monthCalendarView.setVisibility(VISIBLE);
            weekCalendarView.setVisibility(INVISIBLE);
            monthCalendarParent.setY(0);
        }
    }

    private void checkWeekCalendar() {
        WeekView weekView = weekCalendarView.getCurrentWeekView();
        DateTime start = weekView.getStartDate();
        DateTime end = weekView.getEndDate();
        DateTime current = new DateTime(mCurrentSelectYear, mCurrentSelectMonth + 1, mCurrentSelectDay, 23, 59, 59);
        int week = 0;
        while (current.getMillis() < start.getMillis()) {
            week--;
            start = start.plusDays(-7);
        }
        current = new DateTime(mCurrentSelectYear, mCurrentSelectMonth + 1, mCurrentSelectDay, 0, 0, 0);
        if (week == 0) {
            while (current.getMillis() > end.getMillis()) {
                week++;
                end = end.plusDays(7);
            }
        }
        if (week != 0) {
            int position = weekCalendarView.getCurrentItem() + week;
            if (weekCalendarView.getWeekViews().get(position) != null) {
                weekCalendarView.getWeekViews().get(position).setSelectYearMonth(mCurrentSelectYear, mCurrentSelectMonth, mCurrentSelectDay);
                weekCalendarView.getWeekViews().get(position).invalidate();
            } else {
                WeekView newWeekView = weekCalendarView.getWeekAdapter().instanceWeekView(position);
                newWeekView.setSelectYearMonth(mCurrentSelectYear, mCurrentSelectMonth, mCurrentSelectDay);
                newWeekView.invalidate();
            }
            weekCalendarView.setCurrentItem(position, false);
        }
    }

    private void resetScrollingState() {
        mDownPosition[0] = 0;
        mDownPosition[1] = 0;
        mIsScrolling = false;
    }

    protected void onCalendarScroll(float distanceY) {
        MonthView monthView = monthCalendarView.getCurrentMonthView();
        distanceY = Math.min(distanceY, mAutoScrollDistance);
        float calendarDistanceY = distanceY / (mCurrentRowsIsSix ? 5.0f : 4.0f);
        int row = monthView.getWeekRow() - 1;
        int calendarTop = -row * mRowSize;
        int scheduleTop = mRowSize;
        float calendarY = monthCalendarParent.getY() - calendarDistanceY * row;
        calendarY = Math.min(calendarY, 0);
        calendarY = Math.max(calendarY, calendarTop);
        monthCalendarParent.setY(calendarY);
        float scheduleY = scheduleContentParent.getY() - distanceY;
        if (mCurrentRowsIsSix) {
            scheduleY = Math.min(scheduleY, monthCalendarView.getHeight());
        } else {
            scheduleY = Math.min(scheduleY, monthCalendarView.getHeight() - mRowSize);
        }
        scheduleY = Math.max(scheduleY, scheduleTop);
        scheduleContentParent.setY(scheduleY);
    }

    public void setOnCalendarClickListener(OnCalendarClickListener onCalendarClickListener) {
        mOnCalendarClickListener = onCalendarClickListener;
    }

    private void resetMonthViewDate(final int year, final int month, final int day, final int position) {
        if (monthCalendarView.getMonthViews().get(position) == null) {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    resetMonthViewDate(year, month, day, position);
                }
            }, 50);
        } else {
            monthCalendarView.getMonthViews().get(position).clickThisMonth(year, month, day);
        }
    }

    /**
     * 初始化年月日
     *
     * @param year
     * @param month (0-11)
     * @param day   (1-31)
     */
    public void initData(int year, int month, int day) {
        int monthDis = CalendarUtils.getMonthsAgo(mCurrentSelectYear, mCurrentSelectMonth, year, month);
        int position = monthCalendarView.getCurrentItem() + monthDis;
        monthCalendarView.setCurrentItem(position);
        resetMonthViewDate(year, month, day, position);
    }

    /**
     * 添加多个圆点提示
     *
     * @param hints
     */
    public void addTaskHints(List<Integer> hints) {
        CalendarUtils.getInstance(getContext()).addTaskHints(mCurrentSelectYear, mCurrentSelectMonth, hints);
        if (monthCalendarView.getCurrentMonthView() != null) {
            monthCalendarView.getCurrentMonthView().invalidate();
        }
        if (weekCalendarView.getCurrentWeekView() != null) {
            weekCalendarView.getCurrentWeekView().invalidate();
        }
    }

    /**
     * 删除多个圆点提示
     *
     * @param hints
     */
    public void removeTaskHints(List<Integer> hints) {
        CalendarUtils.getInstance(getContext()).removeTaskHints(mCurrentSelectYear, mCurrentSelectMonth, hints);
        if (monthCalendarView.getCurrentMonthView() != null) {
            monthCalendarView.getCurrentMonthView().invalidate();
        }
        if (weekCalendarView.getCurrentWeekView() != null) {
            weekCalendarView.getCurrentWeekView().invalidate();
        }
    }

    /**
     * 添加一个圆点提示
     *
     * @param day
     */
    public void addTaskHint(Integer day) {
        if (monthCalendarView.getCurrentMonthView() != null) {
            if (monthCalendarView.getCurrentMonthView().addTaskHint(day)) {
                if (weekCalendarView.getCurrentWeekView() != null) {
                    weekCalendarView.getCurrentWeekView().invalidate();
                }
            }
        }
    }

    /**
     * 删除一个圆点提示
     *
     * @param day
     */
    public void removeTaskHint(Integer day) {
        if (monthCalendarView.getCurrentMonthView() != null) {
            if (monthCalendarView.getCurrentMonthView().removeTaskHint(day)) {
                if (weekCalendarView.getCurrentWeekView() != null) {
                    weekCalendarView.getCurrentWeekView().invalidate();
                }
            }
        }
    }

    public ScheduleRecyclerView getSchedulerRecyclerView() {
        return scheduleRecyclerView;
    }

    public MonthCalendarView getMonthCalendar() {
        return monthCalendarView;
    }

    public WeekCalendarView getWeekCalendar() {
        return weekCalendarView;
    }

    public int getCurrentSelectYear() {
        return mCurrentSelectYear;
    }

    public int getCurrentSelectMonth() {
        return mCurrentSelectMonth;
    }

    public int getCurrentSelectDay() {
        return mCurrentSelectDay;
    }

    public void initCalendar(Long currentTime) {
        if (currentTime == null) {
            currentTime = System.currentTimeMillis();
        }
        if (monthCalendarView != null) {
            monthCalendarView.init(currentTime);
        }
        if (weekCalendarView != null) {
            weekCalendarView.init(currentTime);
        }
    }

    public void setTodayToView() {
        long currentTime = monthCalendarView.getCurrentTime();
        DateTime dateTime = new DateTime(currentTime);
        initData(dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth());
    }

}
