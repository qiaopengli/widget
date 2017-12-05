package com.lqp.box;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.maple.leaf.widget.calendar.schedule.ScheduleLayout;

/**
 * Created by liqiaopeng on 2017/11/15.
 */

public class CalendarActivity extends FragmentActivity {
    private ScheduleLayout scheduleLayout = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        scheduleLayout = findViewById(R.id.activity_calendar_schedule_layout);
        scheduleLayout.initCalendar(0L);


        findViewById(R.id.goto_today).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scheduleLayout.setTodayToView();
            }
        });
    }
}
