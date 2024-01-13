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
package nodomain.freeyourgadget.gadgetbridge.activities.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nodomain.freeyourgadget.gadgetbridge.R;
import nodomain.freeyourgadget.gadgetbridge.util.FormatUtils;

/**
 * A simple {@link AbstractDashboardWidget} subclass.
 * Use the {@link DashboardDistanceWidget#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DashboardDistanceWidget extends AbstractDashboardWidget {
    private static final Logger LOG = LoggerFactory.getLogger(DashboardDistanceWidget.class);
    private TextView distanceText;
    private ImageView distanceGauge;

    public DashboardDistanceWidget() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param timeFrom Start time in seconds since Unix epoch.
     * @param timeTo End time in seconds since Unix epoch.
     * @return A new instance of fragment DashboardDistanceWidget.
     */
    public static DashboardDistanceWidget newInstance(int timeFrom, int timeTo) {
        DashboardDistanceWidget fragment = new DashboardDistanceWidget();
        Bundle args = new Bundle();
        args.putInt(ARG_TIME_FROM, timeFrom);
        args.putInt(ARG_TIME_TO, timeTo);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.dashboard_widget_distance, container, false);
        distanceText = fragmentView.findViewById(R.id.distance_text);
        distanceGauge = fragmentView.findViewById(R.id.distance_gauge);

        fillData();

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (distanceText != null && distanceGauge != null) fillData();
    }

    @Override
    protected void fillData() {
        // Update text representation
        String distanceFormatted = FormatUtils.getFormattedDistanceLabel(getDistanceTotal());
        distanceText.setText(distanceFormatted);

        // Draw gauge
        distanceGauge.setImageBitmap(drawGauge(200, 15, color_distance, getDistanceGoalFactor()));
    }
}