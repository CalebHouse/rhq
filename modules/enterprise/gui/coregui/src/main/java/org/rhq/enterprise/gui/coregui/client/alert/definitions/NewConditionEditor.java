/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as
 * published by the Free Software Foundation, and/or the GNU Lesser
 * General Public License, version 2.1, also as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License and the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License
 * and the GNU Lesser General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.rhq.enterprise.gui.coregui.client.alert.definitions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.FormItemIfFunction;
import com.smartgwt.client.widgets.form.fields.ButtonItem;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;

import org.rhq.core.domain.alert.AlertCondition;
import org.rhq.core.domain.alert.AlertConditionCategory;
import org.rhq.core.domain.event.EventSeverity;
import org.rhq.core.domain.measurement.AvailabilityType;
import org.rhq.core.domain.measurement.DataType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.NumericType;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.operation.OperationRequestStatus;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.util.selenium.LocatableDynamicForm;

/**
 * @author John Mazzitelli
 */
public class NewConditionEditor extends LocatableDynamicForm {

    // these aren't "real" calltime condition categories (not real AlertConditionCategory enums)
    // but we need these values for the drop down menu selections
    private static final String ALERT_CONDITION_CATEGORY_CALLTIME_CHANGE = "calltime-change";
    private static final String ALERT_CONDITION_CATEGORY_CALLTIME_THRESHOLD = "calltime-threshold";

    private static final String AVAILABILITY_ITEMNAME = "availability";
    private static final String THRESHOLD_METRIC_ITEMNAME = "thresholdMetric";
    private static final String THRESHOLD_COMPARATOR_ITEMNAME = "thresholdComparator";
    private static final String THRESHOLD_ABSVALUE_ITEMNAME = "metricAbsoluteValue";
    private static final String BASELINE_METRIC_ITEMNAME = "baselineMetric";
    private static final String BASELINE_COMPARATOR_ITEMNAME = "baselineComparator";
    private static final String BASELINE_PERCENTAGE_ITEMNAME = "baselinePercentage";
    private static final String BASELINE_SELECTION_ITEMNAME = "baselineSelection";
    private static final String CHANGE_METRIC_ITEMNAME = "changeMetric";
    private static final String CALLTIME_THRESHOLD_METRIC_ITEMNAME = "calltimeThresholdMetric";
    private static final String CALLTIME_THRESHOLD_MINMAXAVG_ITEMNAME = "calltimeThresholdMinMaxAvgSelection";
    private static final String CALLTIME_THRESHOLD_COMPARATOR_ITEMNAME = "calltimeThresholdComparator";
    private static final String CALLTIME_THRESHOLD_ABSVALUE_ITEMNAME = "calltimeThresholdAbsoluteValue";
    private static final String CALLTIME_THRESHOLD_REGEX_ITEMNAME = "calltimeThresholdRegex";
    private static final String CALLTIME_CHANGE_METRIC_ITEMNAME = "calltimeChangeMetric";
    private static final String CALLTIME_CHANGE_MINMAXAVG_ITEMNAME = "calltimeChangeMinMaxAvgSelection";
    private static final String CALLTIME_CHANGE_COMPARATOR_ITEMNAME = "calltimeChangeComparator";
    private static final String CALLTIME_CHANGE_PERCENTAGE_ITEMNAME = "calltimeChangePercentageValue";
    private static final String CALLTIME_CHANGE_REGEX_ITEMNAME = "calltimeChangeRegex";
    private static final String TRAIT_METRIC_ITEMNAME = "trait";
    private static final String OPERATION_NAME_ITEMNAME = "operation";
    private static final String OPERATION_RESULTS_ITEMNAME = "operationResults";
    private static final String EVENT_SEVERITY_ITEMNAME = "eventSeverity";
    private static final String EVENT_REGEX_ITEMNAME = "eventRegex";

    private SelectItem conditionTypeSelectItem;
    private HashSet<AlertCondition> conditions; // the new condition we create goes into this set
    private boolean supportsMetrics = false;
    private boolean supportsCalltimeMetrics = false;
    private boolean supportsTraits = false;
    private boolean supportsOperations = false;
    private boolean supportsEvents = false;
    private boolean supportsResourceConfig = false;
    private Runnable closeFunction; // this is called after a button is pressed and the editor should close 
    private ResourceType resourceType;

    public NewConditionEditor(String locatorId, HashSet<AlertCondition> conditions, ResourceType rtype,
        Runnable closeFunc) {

        super(locatorId);
        this.conditions = conditions;
        this.closeFunction = closeFunc;
        this.resourceType = rtype;

        this.supportsEvents = (rtype.getEventDefinitions() != null && rtype.getEventDefinitions().size() > 0);
        this.supportsResourceConfig = (rtype.getResourceConfigurationDefinition() != null);

        Set<MeasurementDefinition> metricDefinitions = rtype.getMetricDefinitions();
        if (metricDefinitions != null && metricDefinitions.size() > 0) {
            for (MeasurementDefinition measurementDefinition : metricDefinitions) {
                switch (measurementDefinition.getDataType()) {
                case MEASUREMENT: {
                    this.supportsMetrics = true;
                    break;
                }
                case CALLTIME: {
                    this.supportsCalltimeMetrics = true;
                    break;
                }
                case TRAIT: {
                    this.supportsTraits = true;
                    break;
                }
                default: {
                    break;
                }
                }
            }
        }

        Set<OperationDefinition> operationDefinitions = rtype.getOperationDefinitions();
        if (operationDefinitions != null && operationDefinitions.size() > 0) {
            this.supportsOperations = true;
        }
    }

    @Override
    protected void onInit() {
        super.onInit();

        setMargin(20);

        conditionTypeSelectItem = new SelectItem("conditionType", "Condition Type");
        LinkedHashMap<String, String> condTypes = new LinkedHashMap<String, String>(7);
        condTypes.put(AlertConditionCategory.AVAILABILITY.name(), "Availability Change");
        if (supportsMetrics) {
            condTypes.put(AlertConditionCategory.THRESHOLD.name(), "Measurement Absolute Value Threshold");
            condTypes.put(AlertConditionCategory.BASELINE.name(), "Measurement Baseline Threshold");
            condTypes.put(AlertConditionCategory.CHANGE.name(), "Measurement Value Change");
        }
        if (supportsCalltimeMetrics) {
            condTypes.put(ALERT_CONDITION_CATEGORY_CALLTIME_THRESHOLD, "Calltime Value Threshold");
            condTypes.put(ALERT_CONDITION_CATEGORY_CALLTIME_CHANGE, "Calltime Value Change");
        }
        if (supportsTraits) {
            condTypes.put(AlertConditionCategory.TRAIT.name(), "Trait Value Change");
        }
        if (supportsOperations) {
            condTypes.put(AlertConditionCategory.CONTROL.name(), "Operation Execution");
        }
        if (supportsResourceConfig) {
            condTypes.put(AlertConditionCategory.RESOURCE_CONFIG.name(), "Resource Configuration Change");
        }
        if (supportsEvents) {
            condTypes.put(AlertConditionCategory.EVENT.name(), "Event Detection");
        }
        conditionTypeSelectItem.setValueMap(condTypes);
        conditionTypeSelectItem.setDefaultValue(AlertConditionCategory.AVAILABILITY.name());
        conditionTypeSelectItem.setWrapTitle(false);
        conditionTypeSelectItem.setRedrawOnChange(true);
        conditionTypeSelectItem.setWidth("*");

        SpacerItem spacer1 = new SpacerItem();
        spacer1.setColSpan(2);
        spacer1.setHeight(5);

        SpacerItem spacer2 = new SpacerItem();
        spacer2.setColSpan(2);
        spacer2.setHeight(5);

        ButtonItem ok = new ButtonItem("okButtonItem", "OK");
        ok.setEndRow(false);
        ok.setAlign(Alignment.RIGHT);
        ok.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (validate(false)) {
                    saveNewCondition();
                    closeFunction.run();
                }
            }
        });

        ButtonItem cancel = new ButtonItem("cancelButtonItem", "Cancel");
        cancel.setStartRow(false);
        cancel.setAlign(Alignment.LEFT);
        cancel.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                closeFunction.run();
            }
        });

        ArrayList<FormItem> formItems = new ArrayList<FormItem>();
        formItems.add(conditionTypeSelectItem);
        formItems.add(spacer1);
        formItems.addAll(buildAvailabilityChangeFormItems());
        if (supportsMetrics) {
            formItems.addAll(buildMetricThresholdFormItems());
            formItems.addAll(buildMetricBaselineFormItems());
            formItems.addAll(buildMetricChangeFormItems());
        }
        if (supportsCalltimeMetrics) {
            formItems.addAll(buildCalltimeThresholdFormItems());
            formItems.addAll(buildCalltimeChangeFormItems());
        }
        if (supportsTraits) {
            formItems.addAll(buildTraitChangeFormItems());
        }
        if (supportsOperations) {
            formItems.addAll(buildOperationFormItems());
        }
        if (supportsEvents) {
            formItems.addAll(buildEventFormItems());
        }
        if (supportsResourceConfig) {
            formItems.addAll(buildResourceConfigChangeFormItems());
        }
        formItems.add(spacer2);
        formItems.add(ok);
        formItems.add(cancel);

        setFields(formItems.toArray(new FormItem[formItems.size()]));
    };

    private void saveNewCondition() {
        final boolean calltimeCategory;
        final AlertConditionCategory category;

        String selectedCategory = conditionTypeSelectItem.getValue().toString();
        if (selectedCategory.equals(ALERT_CONDITION_CATEGORY_CALLTIME_THRESHOLD)) {
            calltimeCategory = true;
            category = AlertConditionCategory.THRESHOLD;
        } else if (selectedCategory.equals(ALERT_CONDITION_CATEGORY_CALLTIME_CHANGE)) {
            calltimeCategory = true;
            category = AlertConditionCategory.CHANGE;
        } else {
            calltimeCategory = false;
            category = AlertConditionCategory.valueOf(selectedCategory);
        }

        AlertCondition newCondition = new AlertCondition();
        newCondition.setCategory(category);

        switch (category) {
        case AVAILABILITY: {
            newCondition.setName(null);
            newCondition.setComparator(null);
            newCondition.setThreshold(null);
            newCondition.setOption(getValueAsString(AVAILABILITY_ITEMNAME));
            newCondition.setMeasurementDefinition(null);
            break;
        }

        case THRESHOLD: {
            if (!calltimeCategory) {
                MeasurementDefinition measDef = getMeasurementDefinition(getValueAsString(THRESHOLD_METRIC_ITEMNAME));
                newCondition.setName(measDef.getDisplayName()); // TODO should not use display name
                newCondition.setThreshold(Double.valueOf(getValueAsString(THRESHOLD_ABSVALUE_ITEMNAME)));
                newCondition.setComparator(getValueAsString(THRESHOLD_COMPARATOR_ITEMNAME));
                newCondition.setOption(null);
                newCondition.setMeasurementDefinition(measDef);
            } else {
                MeasurementDefinition measDef = getMeasurementDefinition(getValueAsString(CALLTIME_THRESHOLD_METRIC_ITEMNAME));
                newCondition.setName(getValueAsString(CALLTIME_THRESHOLD_REGEX_ITEMNAME));
                newCondition.setThreshold(Double.valueOf(getValueAsString(CALLTIME_THRESHOLD_ABSVALUE_ITEMNAME)));
                newCondition.setComparator(getValueAsString(CALLTIME_THRESHOLD_COMPARATOR_ITEMNAME));
                newCondition.setOption(getValueAsString(CALLTIME_THRESHOLD_MINMAXAVG_ITEMNAME));
                newCondition.setMeasurementDefinition(measDef);
            }
            break;
        }

        case BASELINE: {
            MeasurementDefinition measDef = getMeasurementDefinition(getValueAsString(BASELINE_METRIC_ITEMNAME));
            newCondition.setName(measDef.getDisplayName()); // TODO should not use display name
            newCondition.setThreshold(Double.valueOf(getValueAsString(BASELINE_PERCENTAGE_ITEMNAME)) / 100.0);
            newCondition.setComparator(getValueAsString(BASELINE_COMPARATOR_ITEMNAME));
            newCondition.setOption(null);
            newCondition.setMeasurementDefinition(measDef);
            break;
        }

        case CHANGE: {
            if (!calltimeCategory) {
                MeasurementDefinition measDef = getMeasurementDefinition(getValueAsString(CHANGE_METRIC_ITEMNAME));
                newCondition.setName(measDef.getDisplayName()); // TODO should not use display name
                newCondition.setComparator(null);
                newCondition.setThreshold(null);
                newCondition.setOption(null);
                newCondition.setMeasurementDefinition(measDef);
            } else {
                MeasurementDefinition measDef = getMeasurementDefinition(getValueAsString(CALLTIME_CHANGE_METRIC_ITEMNAME));
                newCondition.setName(getValueAsString(CALLTIME_CHANGE_REGEX_ITEMNAME));
                newCondition
                    .setThreshold(Double.valueOf(getValueAsString(CALLTIME_CHANGE_PERCENTAGE_ITEMNAME)) / 100.0);
                newCondition.setComparator(getValueAsString(CALLTIME_CHANGE_COMPARATOR_ITEMNAME));
                newCondition.setOption(getValueAsString(CALLTIME_CHANGE_MINMAXAVG_ITEMNAME));
                newCondition.setMeasurementDefinition(measDef);
            }
            break;
        }

        case TRAIT: {
            MeasurementDefinition measDef = getMeasurementDefinition(getValueAsString(TRAIT_METRIC_ITEMNAME));
            newCondition.setName(measDef.getDisplayName()); // TODO should not use display name
            newCondition.setComparator(null);
            newCondition.setThreshold(null);
            newCondition.setOption(null);
            newCondition.setMeasurementDefinition(measDef);
            break;
        }

        case CONTROL: {
            newCondition.setName(getValueAsString(OPERATION_NAME_ITEMNAME));
            newCondition.setComparator(null);
            newCondition.setThreshold(null);
            newCondition.setOption(getValueAsString(OPERATION_RESULTS_ITEMNAME));
            newCondition.setMeasurementDefinition(null);
            break;
        }

        case EVENT: {
            newCondition.setName(getValueAsString(EVENT_SEVERITY_ITEMNAME));
            newCondition.setComparator(null);
            newCondition.setThreshold(null);
            newCondition.setOption(getValueAsString(EVENT_REGEX_ITEMNAME));
            newCondition.setMeasurementDefinition(null);
            break;
        }

        case RESOURCE_CONFIG: {
            newCondition.setName(null);
            newCondition.setComparator(null);
            newCondition.setThreshold(null);
            newCondition.setOption(null);
            newCondition.setMeasurementDefinition(null);
            break;
        }

        default: {
            CoreGUI.getErrorHandler().handleError("Invalid alert category selected: " + category); // should never happen
            break;
        }
        }

        this.conditions.add(newCondition);
    }

    private ArrayList<FormItem> buildMetricThresholdFormItems() {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(AlertConditionCategory.THRESHOLD);

        String helpStr = "Specify the threshold value that, when violated, triggers the condition. The value you specify is an absolute value with an optional units specifier.";
        StaticTextItem helpItem = buildHelpTextItem("thresholdHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        formItems.add(buildMetricDropDownMenu(THRESHOLD_METRIC_ITEMNAME, false, ifFunc));
        formItems.add(buildComparatorDropDownMenu(THRESHOLD_COMPARATOR_ITEMNAME, ifFunc));
        TextItem absoluteValue = new TextItem(THRESHOLD_ABSVALUE_ITEMNAME, "Metric Value");
        absoluteValue.setWrapTitle(false);
        absoluteValue.setRequired(true);
        absoluteValue
            .setTooltip("The threshold value of the metric that will trigger the condition when compared using the selected comparator.");

        absoluteValue.setShowIfCondition(ifFunc);
        formItems.add(absoluteValue);

        return formItems;
    }

    private ArrayList<FormItem> buildMetricBaselineFormItems() {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(AlertConditionCategory.BASELINE);

        String helpStr = "Specify the baseline value that must be violated to trigger the condition. The value you specify is a percentage of the given baseline value.";
        StaticTextItem helpItem = buildHelpTextItem("baselineHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        // if a metric is trending (up or down), it will never have baselines calculated for it so only show dynamic metrics
        formItems.add(buildMetricDropDownMenu(BASELINE_METRIC_ITEMNAME, true, ifFunc));
        formItems.add(buildComparatorDropDownMenu(BASELINE_COMPARATOR_ITEMNAME, ifFunc));

        TextItem baselinePercentage = new TextItem(BASELINE_PERCENTAGE_ITEMNAME, "Baseline Percentage");
        baselinePercentage.setWrapTitle(false);
        baselinePercentage.setRequired(true);
        baselinePercentage
            .setTooltip("A collected metric value will trigger this condition when compared to this percentage of the selected baseline value using the selected comparator");
        baselinePercentage.setShowIfCondition(ifFunc);
        formItems.add(baselinePercentage);

        SelectItem baselineSelection = new SelectItem(BASELINE_SELECTION_ITEMNAME, "Baseline");
        LinkedHashMap<String, String> baselines = new LinkedHashMap<String, String>(3);
        baselines.put("min", "Minimum"); // TODO can we have the current value of the min baseline
        baselines.put("mean", "Average"); // TODO can we have the current value of the avg baseline
        baselines.put("max", "Maximum"); // TODO can we have the current value of the max baseline
        baselineSelection.setValueMap(baselines);
        baselineSelection.setDefaultValue("mean");
        baselineSelection.setWrapTitle(false);
        baselineSelection.setWidth("*");
        baselineSelection.setRedrawOnChange(true);
        baselineSelection.setShowIfCondition(ifFunc);
        formItems.add(baselineSelection);

        return formItems;
    }

    private ArrayList<FormItem> buildMetricChangeFormItems() {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(AlertConditionCategory.CHANGE);

        String helpStr = "Specify the metric whose value must change to trigger the condition.";
        StaticTextItem helpItem = buildHelpTextItem("changeMetricHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        formItems.add(buildMetricDropDownMenu(CHANGE_METRIC_ITEMNAME, false, ifFunc));

        return formItems;
    }

    private ArrayList<FormItem> buildCalltimeThresholdFormItems() {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(ALERT_CONDITION_CATEGORY_CALLTIME_THRESHOLD);

        String helpStr = "Specify the calltime threshold value that, when violated, triggers the condition. The value you specify is an absolute value with an optional units specifier. You also must specify which calltime limit to compare the value with (minimum, maximum or average calltime value).";
        StaticTextItem helpItem = buildHelpTextItem("calltimeThresholdHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        formItems.add(buildCalltimeMetricDropDownMenu(CALLTIME_THRESHOLD_METRIC_ITEMNAME, ifFunc));

        SelectItem minMaxAvgSelection = new SelectItem(CALLTIME_THRESHOLD_MINMAXAVG_ITEMNAME, "Calltime Limit");
        LinkedHashMap<String, String> limits = new LinkedHashMap<String, String>(3);
        limits.put("MIN", "Minimum");
        limits.put("AVG", "Average");
        limits.put("MAX", "Maximum");
        minMaxAvgSelection.setTooltip("The calltime limit value that is to be compared with the given value");
        minMaxAvgSelection.setValueMap(limits);
        minMaxAvgSelection.setDefaultValue("AVG");
        minMaxAvgSelection.setWrapTitle(false);
        minMaxAvgSelection.setWidth("*");
        minMaxAvgSelection.setRedrawOnChange(true);
        minMaxAvgSelection.setShowIfCondition(ifFunc);
        formItems.add(minMaxAvgSelection);

        formItems.add(buildComparatorDropDownMenu(CALLTIME_THRESHOLD_COMPARATOR_ITEMNAME, ifFunc));
        TextItem absoluteValue = new TextItem(CALLTIME_THRESHOLD_ABSVALUE_ITEMNAME, "Calltime Value");
        absoluteValue.setWrapTitle(false);
        absoluteValue.setRequired(true);
        absoluteValue
            .setTooltip("The threshold value of the metric that will trigger the condition when compared using the selected comparator.");
        absoluteValue.setShowIfCondition(ifFunc);
        formItems.add(absoluteValue);

        TextItem regex = new TextItem(CALLTIME_THRESHOLD_REGEX_ITEMNAME, "Regular Expression");
        regex.setRequired(false);
        regex
            .setTooltip("If specified, this is a regular expression that must match a call destination in order to trigger the condition.");
        regex.setWrapTitle(false);
        regex.setShowIfCondition(ifFunc);
        formItems.add(regex);

        return formItems;
    }

    private ArrayList<FormItem> buildCalltimeChangeFormItems() {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(ALERT_CONDITION_CATEGORY_CALLTIME_CHANGE);

        String helpStr = "Specify the calltime value that, when changed at least a specified amount, triggers the condition. You must specify which calltime limit to check (minimum, maximum or average calltime value) and the percentage of change that must occur.";
        StaticTextItem helpItem = buildHelpTextItem("calltimeChangeHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        formItems.add(buildCalltimeMetricDropDownMenu(CALLTIME_CHANGE_METRIC_ITEMNAME, ifFunc));

        SelectItem minMaxAvgSelection = new SelectItem(CALLTIME_CHANGE_MINMAXAVG_ITEMNAME, "Calltime Limit");
        LinkedHashMap<String, String> limits = new LinkedHashMap<String, String>(3);
        limits.put("MIN", "Minimum");
        limits.put("AVG", "Average");
        limits.put("MAX", "Maximum");
        minMaxAvgSelection.setTooltip("The calltime limit value that is to be checked for change");
        minMaxAvgSelection.setValueMap(limits);
        minMaxAvgSelection.setDefaultValue("AVG");
        minMaxAvgSelection.setWrapTitle(false);
        minMaxAvgSelection.setWidth("*");
        minMaxAvgSelection.setRedrawOnChange(true);
        minMaxAvgSelection.setShowIfCondition(ifFunc);
        formItems.add(minMaxAvgSelection);

        formItems.add(buildCalltimeComparatorDropDownMenu(CALLTIME_CHANGE_COMPARATOR_ITEMNAME, ifFunc));

        TextItem percentage = new TextItem(CALLTIME_CHANGE_PERCENTAGE_ITEMNAME, "Percentage Change");
        percentage.setWrapTitle(false);
        percentage.setRequired(true);
        percentage
            .setTooltip("A collected calltime value will trigger this condition when it differs by at least this percentage of the selected calltime limit value");
        percentage.setShowIfCondition(ifFunc);
        formItems.add(percentage);

        TextItem regex = new TextItem(CALLTIME_CHANGE_REGEX_ITEMNAME, "Regular Expression");
        regex.setRequired(false);
        regex
            .setTooltip("If specified, this is a regular expression that must match a call destination in order to trigger the condition.");
        regex.setWrapTitle(false);
        regex.setShowIfCondition(ifFunc);
        formItems.add(regex);

        return formItems;
    }

    private ArrayList<FormItem> buildTraitChangeFormItems() {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(AlertConditionCategory.TRAIT);

        String helpStr = "Specify the trait whose value must change to trigger the condition.";
        StaticTextItem helpItem = buildHelpTextItem("traitHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        LinkedHashMap<String, String> traitsMap = new LinkedHashMap<String, String>();
        for (MeasurementDefinition def : this.resourceType.getMetricDefinitions()) {
            if (def.getDataType() == DataType.TRAIT) {
                traitsMap.put(def.getName(), def.getDisplayName());
            }
        }

        SelectItem traitSelection = new SelectItem(TRAIT_METRIC_ITEMNAME, "Trait");
        traitSelection.setValueMap(traitsMap);
        traitSelection.setDefaultValue(traitsMap.keySet().iterator().next()); // just use the first one
        traitSelection.setWidth("*");
        traitSelection.setRedrawOnChange(true);
        traitSelection.setShowIfCondition(ifFunc);
        formItems.add(traitSelection);

        return formItems;
    }

    private ArrayList<FormItem> buildAvailabilityChangeFormItems() {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(AlertConditionCategory.AVAILABILITY);

        String helpStr = "Specify the availability state change that will trigger the condition.";
        StaticTextItem helpItem = buildHelpTextItem("availabilityHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        SelectItem selection = new SelectItem(AVAILABILITY_ITEMNAME, "Availability");
        LinkedHashMap<String, String> avails = new LinkedHashMap<String, String>(2);
        avails.put(AvailabilityType.UP.name(), "Goes UP");
        avails.put(AvailabilityType.DOWN.name(), "Goes DOWN");
        selection.setValueMap(avails);
        selection.setDefaultValue(AvailabilityType.DOWN.name());
        selection.setShowIfCondition(ifFunc);

        formItems.add(selection);
        return formItems;
    }

    private ArrayList<FormItem> buildOperationFormItems() {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(AlertConditionCategory.CONTROL);

        String helpStr = "Specify the result that must occur when the selected operation is executed in order to trigger the condition.";
        StaticTextItem helpItem = buildHelpTextItem("operationHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        LinkedHashMap<String, String> ops = new LinkedHashMap<String, String>();
        for (OperationDefinition opDef : this.resourceType.getOperationDefinitions()) {
            ops.put(opDef.getName(), opDef.getDisplayName());
        }

        SelectItem opSelection = new SelectItem(OPERATION_NAME_ITEMNAME, "Operation");
        opSelection.setValueMap(ops);
        opSelection.setDefaultValue(ops.keySet().iterator().next()); // just use the first one
        opSelection.setWidth("*");
        opSelection.setRedrawOnChange(true);
        opSelection.setShowIfCondition(ifFunc);
        formItems.add(opSelection);

        SelectItem opResultsSelection = new SelectItem(OPERATION_RESULTS_ITEMNAME, "Operation Status");
        LinkedHashMap<String, String> operationStatuses = new LinkedHashMap<String, String>(4);
        operationStatuses.put(OperationRequestStatus.INPROGRESS.name(), OperationRequestStatus.INPROGRESS.name());
        operationStatuses.put(OperationRequestStatus.SUCCESS.name(), OperationRequestStatus.SUCCESS.name());
        operationStatuses.put(OperationRequestStatus.FAILURE.name(), OperationRequestStatus.FAILURE.name());
        operationStatuses.put(OperationRequestStatus.CANCELED.name(), OperationRequestStatus.CANCELED.name());
        opResultsSelection.setValueMap(operationStatuses);
        opResultsSelection.setDefaultValue(OperationRequestStatus.FAILURE.name());
        opResultsSelection.setWrapTitle(false);
        opResultsSelection.setShowIfCondition(ifFunc);
        formItems.add(opResultsSelection);

        return formItems;
    }

    private ArrayList<FormItem> buildEventFormItems() {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(AlertConditionCategory.EVENT);

        String helpStr = "Specify the event severity that an event message must be reported with in order to trigger this condition. If you specify an optional regular expression, the event message must also match that regular expression in order for the condition to trigger.";
        StaticTextItem helpItem = buildHelpTextItem("eventHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        SelectItem eventSeveritySelection = new SelectItem(EVENT_SEVERITY_ITEMNAME, "Event Severity");
        LinkedHashMap<String, String> severities = new LinkedHashMap<String, String>(5);
        severities.put(EventSeverity.DEBUG.name(), EventSeverity.DEBUG.name());
        severities.put(EventSeverity.INFO.name(), EventSeverity.INFO.name());
        severities.put(EventSeverity.WARN.name(), EventSeverity.WARN.name());
        severities.put(EventSeverity.ERROR.name(), EventSeverity.ERROR.name());
        severities.put(EventSeverity.FATAL.name(), EventSeverity.FATAL.name());
        eventSeveritySelection.setValueMap(severities);
        eventSeveritySelection.setDefaultValue(EventSeverity.ERROR.name());
        eventSeveritySelection.setWrapTitle(false);
        eventSeveritySelection.setShowIfCondition(ifFunc);
        formItems.add(eventSeveritySelection);

        TextItem eventRegex = new TextItem(EVENT_REGEX_ITEMNAME, "Regular Expression");
        eventRegex.setRequired(false);
        eventRegex
            .setTooltip("If specified, this is a regular expression that must match a collected event message in order to trigger the condition.");
        eventRegex.setWrapTitle(false);
        eventRegex.setShowIfCondition(ifFunc);
        formItems.add(eventRegex);

        return formItems;
    }

    private ArrayList<FormItem> buildResourceConfigChangeFormItems() {
        ArrayList<FormItem> formItems = new ArrayList<FormItem>();

        ShowIfCategoryFunction ifFunc = new ShowIfCategoryFunction(AlertConditionCategory.RESOURCE_CONFIG);

        String helpStr = "This condition is triggered when the resource configuration changes.";
        StaticTextItem helpItem = buildHelpTextItem("changeConfigHelp", helpStr, ifFunc);
        formItems.add(helpItem);

        return formItems;
    }

    private SelectItem buildMetricDropDownMenu(String itemName, boolean dynamicOnly, FormItemIfFunction ifFunc) {

        LinkedHashMap<String, String> metricsMap = new LinkedHashMap<String, String>();
        for (MeasurementDefinition def : this.resourceType.getMetricDefinitions()) {
            if (def.getDataType() == DataType.MEASUREMENT) {
                if (!dynamicOnly || def.getNumericType() == NumericType.DYNAMIC) {
                    metricsMap.put(def.getName(), def.getDisplayName());
                }
            }
        }

        SelectItem metricSelection = new SelectItem(itemName, "Metric");
        metricSelection.setValueMap(metricsMap);
        metricSelection.setDefaultValue(metricsMap.keySet().iterator().next()); // just use the first one
        metricSelection.setWidth("*");
        metricSelection.setRedrawOnChange(true);
        metricSelection.setShowIfCondition(ifFunc);
        return metricSelection;
    }

    private SelectItem buildCalltimeMetricDropDownMenu(String itemName, FormItemIfFunction ifFunc) {

        LinkedHashMap<String, String> metricsMap = new LinkedHashMap<String, String>();
        for (MeasurementDefinition def : this.resourceType.getMetricDefinitions()) {
            if (def.getDataType() == DataType.CALLTIME) {
                metricsMap.put(def.getName(), def.getDisplayName());
            }
        }

        SelectItem metricSelection = new SelectItem(itemName, "Metric");
        metricSelection.setValueMap(metricsMap);
        metricSelection.setDefaultValue(metricsMap.keySet().iterator().next()); // just use the first one
        metricSelection.setWidth("*");
        metricSelection.setRedrawOnChange(true);
        metricSelection.setShowIfCondition(ifFunc);
        return metricSelection;
    }

    private SelectItem buildComparatorDropDownMenu(String itemName, FormItemIfFunction ifFunc) {

        LinkedHashMap<String, String> comparators = new LinkedHashMap<String, String>(3);
        comparators.put("<", "< (Less than)");
        comparators.put("=", "= (Equal to)");
        comparators.put(">", "> (Greater than)");

        SelectItem comparatorSelection = new SelectItem(itemName, "Comparator");
        comparatorSelection.setValueMap(comparators);
        comparatorSelection.setDefaultValue("<");
        comparatorSelection.setTooltip("How a collected metric value should be compared to the given threshold value");
        comparatorSelection.setShowIfCondition(ifFunc);
        return comparatorSelection;
    }

    private SelectItem buildCalltimeComparatorDropDownMenu(String itemName, FormItemIfFunction ifFunc) {

        LinkedHashMap<String, String> comparators = new LinkedHashMap<String, String>(3);
        comparators.put("LO", "Shrinks");
        comparators.put("CH", "Changes");
        comparators.put("HI", "Grows");

        SelectItem comparatorSelection = new SelectItem(itemName, "Comparator");
        comparatorSelection.setValueMap(comparators);
        comparatorSelection.setDefaultValue("CH");
        comparatorSelection.setTooltip("How a collected calltime value should be compared to the given calltime limit");
        comparatorSelection.setShowIfCondition(ifFunc);
        return comparatorSelection;
    }

    private StaticTextItem buildHelpTextItem(String itemName, String helpText, FormItemIfFunction ifFunc) {
        StaticTextItem help = new StaticTextItem(itemName);
        help.setShowTitle(false);
        help.setColSpan(2);
        help.setRowSpan(2);
        help.setWrap(true);
        help.setValue(helpText);
        help.setShowIfCondition(ifFunc);
        return help;
    }

    private MeasurementDefinition getMeasurementDefinition(String metricName) {
        for (MeasurementDefinition def : this.resourceType.getMetricDefinitions()) {
            if (metricName.equals(def.getName())) {
                return def;
            }
        }
        CoreGUI.getErrorHandler().handleError("Should have found metric definition - something is wrong");
        return null;
    }

    private class ShowIfCategoryFunction implements FormItemIfFunction {
        private final AlertConditionCategory category;
        private final String calltimeCategory;

        public ShowIfCategoryFunction(AlertConditionCategory category) {
            this.category = category;
            this.calltimeCategory = null;
        }

        public ShowIfCategoryFunction(String calltimeCategory) {
            this.category = null;
            this.calltimeCategory = calltimeCategory;
        }

        public boolean execute(FormItem item, Object value, DynamicForm form) {
            String conditionTypeString = form.getValue("conditionType").toString();
            if (category != null) {
                return category.name().equals(conditionTypeString);
            } else {
                return calltimeCategory.equals(conditionTypeString);
            }
        }
    }
}
