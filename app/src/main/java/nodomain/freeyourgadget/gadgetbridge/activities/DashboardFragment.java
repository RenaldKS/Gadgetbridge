/*  Copyright (C) 2023-2024 Arjan Schrijver

    This file is part of Gadgetbridge.

    Gadgetbridge is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published
    by the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gadgetbridge is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>. */
package nodomain.freeyourgadget.gadgetbridge.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.gridlayout.widget.GridLayout;

import com.google.android.material.card.MaterialCardView;

import java.io.Serializable;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Set;

import nodomain.freeyourgadget.gadgetbridge.GBApplication;
import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.AbstractDashboardWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardActiveTimeWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardCalendarActivity;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardDistanceWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardGoalsWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardSleepWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardStepsWidget;
import nodomain.freeyourgadget.gadgetbridge.activities.dashboard.DashboardTodayWidget;
import nodomain.freeyourgadget.gadgetbridge.util.DateTimeUtils;
import nodomain.freeyourgadget.gadgetbridge.util.DashboardUtils;
import nodomain.freeyourgadget.gadgetbridge.util.Prefs;

public class DashboardFragment extends Fragment {
    private Calendar day = GregorianCalendar.getInstance();
    private TextView textViewDate;
    private TextView arrowLeft;
    private TextView arrowRight;
    private GridLayout gridLayout;
    private DashboardTodayWidget todayWidget;
    private DashboardGoalsWidget goalsWidget;
    private DashboardStepsWidget stepsWidget;
    private DashboardDistanceWidget distanceWidget;
    private DashboardActiveTimeWidget activeTimeWidget;
    private DashboardSleepWidget sleepWidget;
    private DashboardData dashboardData = new DashboardData();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View dashboardView = inflater.inflate(R.layout.fragment_dashboard, container, false);
        setHasOptionsMenu(true);
        textViewDate = dashboardView.findViewById(R.id.dashboard_date);
        gridLayout = dashboardView.findViewById(R.id.dashboard_gridlayout);

        // Increase column count on landscape, tablets and open foldables
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        if (displayMetrics.widthPixels / displayMetrics.density >= 600) {
            gridLayout.setColumnCount(4);
        }

        arrowLeft = dashboardView.findViewById(R.id.arrow_left);
        arrowLeft.setOnClickListener(v -> {
            day.add(Calendar.DAY_OF_MONTH, -1);
            refresh();
        });
        arrowRight = dashboardView.findViewById(R.id.arrow_right);
        arrowRight.setOnClickListener(v -> {
            Calendar today = GregorianCalendar.getInstance();
            if (!DateTimeUtils.isSameDay(today, day)) {
                day.add(Calendar.DAY_OF_MONTH, 1);
                refresh();
            }
        });

        todayWidget = null;
        goalsWidget = null;
        stepsWidget = null;
        distanceWidget = null;
        activeTimeWidget = null;
        sleepWidget = null;
        refresh();

        return dashboardView;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.dashboard_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.dashboard_show_calendar:
                Intent intent = new Intent(requireActivity(), DashboardCalendarActivity.class);
                intent.putExtra(DashboardCalendarActivity.EXTRA_TIMESTAMP, day.getTimeInMillis());
                startActivityForResult(intent, 0);
                return false;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == DashboardCalendarActivity.RESULT_OK && data != null) {
            long timeMillis = data.getLongExtra(DashboardCalendarActivity.EXTRA_TIMESTAMP, 0);
            if (timeMillis != 0) {
                day.setTimeInMillis(timeMillis);
            }
        }
        gridLayout.removeAllViews();
        todayWidget = null;
        goalsWidget = null;
        stepsWidget = null;
        distanceWidget = null;
        activeTimeWidget = null;
        sleepWidget = null;
        refresh();
    }

    private void refresh() {
        day.set(Calendar.HOUR_OF_DAY, 23);
        day.set(Calendar.MINUTE, 59);
        day.set(Calendar.SECOND, 59);
        dashboardData.clear();
        Prefs prefs = GBApplication.getPrefs();
        dashboardData.showAllDevices = prefs.getBoolean("dashboard_devices_all", true);
        dashboardData.showDeviceList = prefs.getStringSet("dashboard_devices_multiselect", new HashSet<>());
        dashboardData.timeTo = (int) (day.getTimeInMillis() / 1000);
        dashboardData.timeFrom = DateTimeUtils.shiftDays(dashboardData.timeTo, -1);

        Calendar today = GregorianCalendar.getInstance();
        if (DateTimeUtils.isSameDay(today, day)) {
            textViewDate.setText(getContext().getString(R.string.activity_summary_today));
            arrowRight.setAlpha(0.5f);
        } else {
            textViewDate.setText(DateTimeUtils.formatDate(day.getTime()));
            arrowRight.setAlpha(1);
        }

        boolean cardsEnabled = prefs.getBoolean("dashboard_cards_enabled", true);

        if (prefs.getBoolean("dashboard_widget_today_enabled", true)) {
            if (todayWidget == null) {
                todayWidget = DashboardTodayWidget.newInstance(dashboardData);
                createWidget(todayWidget, cardsEnabled, 2);
            } else {
                todayWidget.update();
            }
        }
        if (prefs.getBoolean("dashboard_widget_goals_enabled", true)) {
            if (goalsWidget == null) {
                goalsWidget = DashboardGoalsWidget.newInstance(dashboardData);
                createWidget(goalsWidget, cardsEnabled, 2);
            } else {
                goalsWidget.update();
            }
        }
        if (prefs.getBoolean("dashboard_widget_steps_enabled", true)) {
            if (stepsWidget == null) {
                stepsWidget = DashboardStepsWidget.newInstance(dashboardData);
                createWidget(stepsWidget, cardsEnabled, 1);
            } else {
                stepsWidget.update();
            }
        }
        if (prefs.getBoolean("dashboard_widget_distance_enabled", true)) {
            if (distanceWidget == null) {
                distanceWidget = DashboardDistanceWidget.newInstance(dashboardData);
                createWidget(distanceWidget, cardsEnabled, 1);
            } else {
                distanceWidget.update();
            }
        }
        if (prefs.getBoolean("dashboard_widget_active_time_enabled", true)) {
            if (activeTimeWidget == null) {
                activeTimeWidget = DashboardActiveTimeWidget.newInstance(dashboardData);
                createWidget(activeTimeWidget, cardsEnabled, 1);
            } else {
                activeTimeWidget.update();
            }
        }
        if (prefs.getBoolean("dashboard_widget_sleep_enabled", true)) {
            if (sleepWidget == null) {
                sleepWidget = DashboardSleepWidget.newInstance(dashboardData);
                createWidget(sleepWidget, cardsEnabled, 1);
            } else {
                sleepWidget.update();
            }
        }
    }

    private void createWidget(AbstractDashboardWidget widgetObj, boolean cardsEnabled, int columnSpan) {
        final float scale = requireContext().getResources().getDisplayMetrics().density;
        FragmentContainerView fragment = new FragmentContainerView(requireActivity());
        int fragmentId = View.generateViewId();
        fragment.setId(fragmentId);
        getChildFragmentManager()
                .beginTransaction()
                .replace(fragmentId, widgetObj)
                .commit();

        GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL,1f),
                GridLayout.spec(GridLayout.UNDEFINED, columnSpan, GridLayout.FILL,1f)
        );
        layoutParams.width = 0;
        int pixels_8dp = (int) (8 * scale + 0.5f);
        layoutParams.setMargins(pixels_8dp, pixels_8dp, pixels_8dp, pixels_8dp);

        if (cardsEnabled) {
            MaterialCardView card = new MaterialCardView(requireActivity());
            int pixels_4dp = (int) (4 * scale + 0.5f);
            card.setRadius(pixels_4dp);
            card.setCardElevation(pixels_4dp);
            card.setContentPadding(pixels_4dp, pixels_4dp, pixels_4dp, pixels_4dp);
            card.setLayoutParams(layoutParams);
            card.addView(fragment);
            gridLayout.addView(card);
        } else {
            fragment.setLayoutParams(layoutParams);
            gridLayout.addView(fragment);
        }
    }

    /**
     * This class serves as a data collection object for all data points used by the various
     * dashboard widgets. Since retrieving this data can be costly, this class makes sure it will
     * only be done once. It will be passed to every widget, making sure they have the necessary
     * data available.
     */
    public static class DashboardData implements Serializable {
        public boolean showAllDevices;
        public Set<String> showDeviceList;
        public int timeFrom;
        public int timeTo;
        private int stepsTotal;
        private float stepsGoalFactor;
        private long sleepTotalMinutes;
        private float sleepGoalFactor;
        private float distanceTotalMeters;
        private float distanceGoalFactor;
        private long activeMinutesTotal;
        private float activeMinutesGoalFactor;

        public void clear() {
            stepsTotal = 0;
            stepsGoalFactor = 0;
            sleepTotalMinutes = 0;
            sleepGoalFactor = 0;
            distanceTotalMeters = 0;
            distanceGoalFactor = 0;
            activeMinutesTotal = 0;
            activeMinutesGoalFactor = 0;
        }

        public synchronized int getStepsTotal() {
            if (stepsTotal == 0)
                stepsTotal = DashboardUtils.getStepsTotal(this);
            return stepsTotal;
        }

        public synchronized float getStepsGoalFactor() {
            if (stepsGoalFactor == 0)
                stepsGoalFactor = DashboardUtils.getStepsGoalFactor(this);
            return stepsGoalFactor;
        }

        public synchronized float getDistanceTotal() {
            if (distanceTotalMeters == 0)
                distanceTotalMeters = DashboardUtils.getDistanceTotal(this);
            return distanceTotalMeters;
        }

        public synchronized float getDistanceGoalFactor() {
            if (distanceGoalFactor == 0)
                distanceGoalFactor = DashboardUtils.getDistanceGoalFactor(this);
            return distanceGoalFactor;
        }

        public synchronized long getActiveMinutesTotal() {
            if (activeMinutesTotal == 0)
                activeMinutesTotal = DashboardUtils.getActiveMinutesTotal(this);
            return activeMinutesTotal;
        }

        public synchronized float getActiveMinutesGoalFactor() {
            if (activeMinutesGoalFactor == 0)
                activeMinutesGoalFactor = DashboardUtils.getActiveMinutesGoalFactor(this);
            return activeMinutesGoalFactor;
        }

        public synchronized long getSleepMinutesTotal() {
            if (sleepTotalMinutes == 0)
                sleepTotalMinutes = DashboardUtils.getSleepMinutesTotal(this);
            return sleepTotalMinutes;
        }

        public synchronized float getSleepMinutesGoalFactor() {
            if (sleepGoalFactor == 0)
                sleepGoalFactor = DashboardUtils.getSleepMinutesGoalFactor(this);
            return sleepGoalFactor;
        }
    }
}