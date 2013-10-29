/*
 * RHQ Management Platform
 * Copyright (C) 2005-2013 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.gui.coregui.client.inventory.common;

import java.util.List;

import com.google.gwt.user.client.Timer;
import com.smartgwt.client.widgets.Label;

import org.rhq.core.domain.common.EntityContext;
import org.rhq.core.domain.measurement.Availability;
import org.rhq.core.domain.resource.group.composite.ResourceGroupAvailability;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.dashboard.AutoRefreshUtil;
import org.rhq.enterprise.gui.coregui.client.inventory.AutoRefresh;
import org.rhq.enterprise.gui.coregui.client.inventory.common.graph.ButtonBarDateTimeRangeEditor;
import org.rhq.enterprise.gui.coregui.client.inventory.common.graph.Refreshable;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.avail.AvailabilityD3GraphView;
import org.rhq.enterprise.gui.coregui.client.util.async.CountDownLatch;
import org.rhq.enterprise.gui.coregui.client.util.enhanced.EnhancedVLayout;
import org.rhq.enterprise.gui.coregui.client.util.preferences.MeasurementUserPreferences;

/**
 * Provide the shared stuff for create GraphListViews like Availability graphs
 * and User Preferences pickers for the date range.
 */
public abstract class AbstractD3GraphListView extends EnhancedVLayout implements AutoRefresh, Refreshable {
    protected final static int SINGLE_CHART_HEIGHT = 225;
    protected final static int MULTI_CHART_HEIGHT = 210;
    protected static final Label loadingLabel = new Label(MSG.common_msg_loading());
    protected List<Availability> availabilityList;
    protected List<ResourceGroupAvailability> groupAvailabilityList;
    protected static AvailabilityD3GraphView availabilityGraph;
    protected final MeasurementUserPreferences measurementUserPrefs;
    protected boolean showAvailabilityGraph = false;
    protected final ButtonBarDateTimeRangeEditor buttonBarDateTimeRangeEditor;
    protected static Timer refreshTimer;
    protected boolean isRefreshing;

    public AbstractD3GraphListView() {
        super();
        measurementUserPrefs = new MeasurementUserPreferences(UserSessionManager.getUserPreferences());
        buttonBarDateTimeRangeEditor = new ButtonBarDateTimeRangeEditor(measurementUserPrefs, this);
        startRefreshCycle();
    }

    public abstract void refreshData();

    protected abstract void queryAvailability(final EntityContext context, Long startTime, Long endTime,
        final CountDownLatch countDownLatch);

    @Override
    public void startRefreshCycle() {
        refreshTimer = AutoRefreshUtil.startRefreshCycleWithPageRefreshInterval(this, this, refreshTimer);
    }

    @Override
    protected void onDestroy() {
        AutoRefreshUtil.onDestroy(refreshTimer);

        super.onDestroy();
    }

    @Override
    public boolean isRefreshing() {
        return isRefreshing;
    }

    //Custom refresh operation as we are not directly extending Table
    @Override
    public void refresh() {
        if (isVisible() && !isRefreshing()) {
            isRefreshing = true;
            try {
                if(!buttonBarDateTimeRangeEditor.isCustomTimeRangeActive()){
                    buttonBarDateTimeRangeEditor.updateTimeRangeToNow();
                }
                refreshData();
            } finally {
                isRefreshing = false;
            }
        }
    }

}
