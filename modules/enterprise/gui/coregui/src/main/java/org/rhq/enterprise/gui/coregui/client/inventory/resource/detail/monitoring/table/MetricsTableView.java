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

package org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.table;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.ExpansionMode;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.DataArrivedEvent;
import com.smartgwt.client.widgets.grid.events.DataArrivedHandler;
import com.smartgwt.client.widgets.grid.events.RecordCollapseEvent;
import com.smartgwt.client.widgets.grid.events.RecordCollapseHandler;
import com.smartgwt.client.widgets.grid.events.RecordExpandEvent;
import com.smartgwt.client.widgets.grid.events.RecordExpandHandler;
import com.smartgwt.client.widgets.grid.events.SelectionChangedHandler;
import com.smartgwt.client.widgets.grid.events.SelectionEvent;
import com.smartgwt.client.widgets.grid.events.SortChangedHandler;
import com.smartgwt.client.widgets.grid.events.SortEvent;
import com.smartgwt.client.widgets.layout.VLayout;

import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.composite.MeasurementDataNumericHighLowComposite;
import org.rhq.core.domain.resource.Resource;
import org.rhq.enterprise.gui.coregui.client.UserSessionManager;
import org.rhq.enterprise.gui.coregui.client.components.table.Table;
import org.rhq.enterprise.gui.coregui.client.components.table.TableAction;
import org.rhq.enterprise.gui.coregui.client.gwt.GWTServiceLookup;
import org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractD3GraphListView;
import org.rhq.enterprise.gui.coregui.client.inventory.common.graph.MetricGraphData;
import org.rhq.enterprise.gui.coregui.client.inventory.common.graph.Refreshable;
import org.rhq.enterprise.gui.coregui.client.inventory.common.graph.graphtype.StackedBarMetricGraphImpl;
import org.rhq.enterprise.gui.coregui.client.inventory.resource.detail.monitoring.MetricD3Graph;
import org.rhq.enterprise.gui.coregui.client.util.BrowserUtility;
import org.rhq.enterprise.gui.coregui.client.util.Log;
import org.rhq.enterprise.gui.coregui.client.util.preferences.MeasurementUserPreferences;

/**
 * Views a resource's metrics in a tabular view with sparkline graph and optional detailed d3 graph.
 *
 * @author John Mazzitelli
 * @author Mike Thompson
 */
public class MetricsTableView extends Table<MetricsViewDataSource> implements Refreshable {

    private final Resource resource;
    private final AbstractD3GraphListView abstractD3GraphListView;
    private final MeasurementUserPreferences measurementUserPrefs;
    private final AddToDashboardComponent addToDashboardComponent;
    private Set<Integer> expandedRows;
    private boolean rendered = false;
    private MetricsTableListGrid metricsTableListGrid;

    public MetricsTableView(Resource resource, AbstractD3GraphListView abstractD3GraphListView, Set<Integer> expandedRows) {
        super();
        this.resource = resource;
        this.abstractD3GraphListView = abstractD3GraphListView;
        measurementUserPrefs = new MeasurementUserPreferences(UserSessionManager.getUserPreferences());
        setDataSource(new MetricsViewDataSource(resource));
        addToDashboardComponent = new AddToDashboardComponent(resource);
        if(null == expandedRows){
           this.expandedRows = new HashSet<Integer>();
        }else {
            this.expandedRows = expandedRows;
        }
    }

    /**
     * Creates this Table's list grid (called by onInit()). Subclasses can override this if they require a custom
     * subclass of ListGrid.
     *
     * @return this Table's list grid (must be an instance of ListGrid)
     */
    @Override
    protected ListGrid createListGrid() {
        Log.debug("CreateListGrid.expandedRows: "+expandedRows.size());
        if(null != metricsTableListGrid){
            removeMember(metricsTableListGrid);
            metricsTableListGrid.destroy();
        }
        metricsTableListGrid = new MetricsTableListGrid(this, resource);
        addToDashboardComponent.setMetricsListGrid(metricsTableListGrid);
        return metricsTableListGrid;
    }

    protected void configureTable() {
        ArrayList<ListGridField> fields = getDataSource().getListGridFields();
        setListGridFields(fields.toArray(new ListGridField[0]));

        if (!rendered) {
            addTableAction(MSG.view_measureTable_getLive(), new TableAction() {
                @Override
                public boolean isEnabled(ListGridRecord[] selection) {
                    return true;
                }

                @Override
                public void executeAction(ListGridRecord[] selection, Object actionValue) {
                    refresh();
                }
            });

            addExtraWidget(addToDashboardComponent, false);
            addToDashboardComponent.disableAddToDashboardButton();
            metricsTableListGrid.addSelectionChangedHandler(new SelectionChangedHandler() {
                @Override
                public void onSelectionChanged(SelectionEvent selectionEvent) {
                    if (metricsTableListGrid.getSelectedRecords().length > 0) {
                        addToDashboardComponent.enableAddToDashboardButton();
                    } else {
                        addToDashboardComponent.disableAddToDashboardButton();
                    }
                }
            });
            rendered = true;
        }
    }

    @Override
    /**
     * Redraw Graphs in this context means to refresh the table and redraw open graphs.
     */
    public void refreshData() {
        Log.debug("MetricsView.refreshData()");
        new Timer() {

            @Override
            public void run() {
                createListGrid();
                metricsTableListGrid.expandOpenedRows();
                BrowserUtility.graphSparkLines();
            }
        }.schedule(150);

    }


    @Override
    public void refresh() {
        Log.debug("metricsTableView.refresh");
        super.refresh(false);
        createListGrid();
        metricsTableListGrid.expandOpenedRows();
    }

    public class MetricsTableListGrid extends ListGrid {

        private static final int TREEVIEW_DETAIL_CHART_HEIGHT = 205;
        private static final int NUM_METRIC_POINTS = 60;
        final MetricsTableView metricsTableView;
        private Resource resource;

        public MetricsTableListGrid(final MetricsTableView metricsTableView, final Resource resource) {
            super();
            this.resource = resource;
            this.metricsTableView = metricsTableView;
            setCanExpandRecords(true);
            setCanExpandMultipleRecords(true);
            setExpansionMode(ExpansionMode.DETAIL_FIELD);

            addRecordExpandHandler(new RecordExpandHandler() {
                @Override
                public void onRecordExpand(RecordExpandEvent recordExpandEvent) {
                    metricsTableView.expandedRows.add(recordExpandEvent.getRecord().getAttributeAsInt(
                        MetricsViewDataSource.FIELD_METRIC_DEF_ID));
                    refreshData();
                }

            });
            addRecordCollapseHandler(new RecordCollapseHandler() {
                @Override
                public void onRecordCollapse(RecordCollapseEvent recordCollapseEvent) {
                    metricsTableView.expandedRows.remove(recordCollapseEvent.getRecord().getAttributeAsInt(
                        MetricsViewDataSource.FIELD_METRIC_DEF_ID));
                    refresh();
                    new Timer() {

                        @Override
                        public void run() {
                            BrowserUtility.graphSparkLines();
                        }
                    }.schedule(150);
                }
            });
            addSortChangedHandler(new SortChangedHandler() {
                @Override
                public void onSortChanged(SortEvent sortEvent) {
                    refreshData();
                }
            });

            addDataArrivedHandler(new DataArrivedHandler() {
                @Override
                public void onDataArrived(DataArrivedEvent dataArrivedEvent) {
                    expandOpenedRows();
                }
            });

        }

        public void expandOpenedRows() {

            int startRow = 0;
            int endRow = this.getRecords().length;
            for (int i = startRow; i < endRow; i++) {
                ListGridRecord listGridRecord = getRecord(i);
                if (null != listGridRecord) {
                    int metricDefinitionId = listGridRecord
                        .getAttributeAsInt(MetricsViewDataSource.FIELD_METRIC_DEF_ID);
                    if (null != metricsTableView && null != expandedRows && metricsTableView.expandedRows.contains(metricDefinitionId)) {
                        expandRecord(listGridRecord);
                    }
                }
            }
        }

        public void expandOpenedRows(Set<Integer> selectedRows) {
            expandedRows = selectedRows;
            expandOpenedRows();
        }

        @Override
        /**
         * If you expand a grid row then create a graph.
         */
        protected Canvas getExpansionComponent(final ListGridRecord record) {
            final Integer definitionId = record.getAttributeAsInt(MetricsViewDataSource.FIELD_METRIC_DEF_ID);
            final Integer resourceId = record.getAttributeAsInt(MetricsViewDataSource.FIELD_RESOURCE_ID);
            VLayout vLayout = new VLayout();
            vLayout.setPadding(5);

            final String chartId = "rChart-" + resourceId + "-" + definitionId;
            HTMLFlow htmlFlow = new HTMLFlow(MetricD3Graph.createGraphMarkerTemplate(chartId,
                TREEVIEW_DETAIL_CHART_HEIGHT));
            vLayout.addMember(htmlFlow);

            int[] definitionArrayIds = new int[1];
            definitionArrayIds[0] = definitionId;
            GWTServiceLookup.getMeasurementDataService().findDataForResource(resourceId, definitionArrayIds,
                measurementUserPrefs.getMetricRangePreferences().begin,
                measurementUserPrefs.getMetricRangePreferences().end, NUM_METRIC_POINTS,
                new AsyncCallback<List<List<MeasurementDataNumericHighLowComposite>>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        Log.warn("Error retrieving recent metrics charting data for resource [" + resourceId + "]:"
                            + caught.getMessage());
                    }

                    @Override
                    public void onSuccess(List<List<MeasurementDataNumericHighLowComposite>> results) {
                        if (!results.isEmpty()) {

                            //load the data results for the given metric definition
                            List<MeasurementDataNumericHighLowComposite> measurementList = results.get(0);

                            MeasurementDefinition measurementDefinition = null;
                            for (MeasurementDefinition definition : resource.getResourceType().getMetricDefinitions()) {
                                if (definition.getId() == definitionId) {
                                    measurementDefinition = definition;
                                    break;
                                }
                            }

                            MetricGraphData metricGraphData = MetricGraphData.createForResource(resourceId,
                                resource.getName(), measurementDefinition, measurementList, null);
                            metricGraphData.setHideLegend(true);

                            StackedBarMetricGraphImpl graph = GWT.create(StackedBarMetricGraphImpl.class);
                            graph.setMetricGraphData(metricGraphData);
                            final MetricD3Graph graphView = new MetricD3Graph(graph, abstractD3GraphListView);
                            new Timer() {
                                @Override
                                public void run() {
                                    graphView.drawJsniChart();
                                    BrowserUtility.graphSparkLines();
                                }
                            }.schedule(150);

                        } else {
                            Log.warn("No chart data retrieving for resource [" + resourceId + "-" + definitionId + "]");

                        }
                    }
                });

            return vLayout;
        }
    }

}
