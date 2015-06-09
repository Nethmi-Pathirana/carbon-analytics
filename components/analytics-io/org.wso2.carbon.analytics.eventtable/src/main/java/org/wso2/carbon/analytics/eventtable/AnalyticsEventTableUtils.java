/*
*  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.analytics.eventtable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.wso2.carbon.analytics.datasource.commons.Record;
import org.wso2.carbon.analytics.datasource.commons.RecordGroup;
import org.wso2.carbon.analytics.datasource.commons.exception.AnalyticsException;
import org.wso2.carbon.analytics.datasource.core.util.GenericUtils;
import org.wso2.carbon.analytics.eventtable.internal.ServiceHolder;
import org.wso2.siddhi.core.event.ComplexEvent;
import org.wso2.siddhi.core.event.ComplexEventChunk;
import org.wso2.siddhi.core.event.stream.StreamEvent;
import org.wso2.siddhi.query.api.definition.Attribute;

/**
 * Utility operations for analytics event table.
 */
public class AnalyticsEventTableUtils {

    public static void putEvents(int tenantId, String tableName, List<Attribute> attrs, 
            ComplexEventChunk<StreamEvent> addingEventChunk) {
        List<Record> records = new ArrayList<Record>();
        StreamEvent event;
        while (addingEventChunk.hasNext()) {
            event = addingEventChunk.next();
            records.add(streamEventToRecord(tenantId, tableName, attrs, event));
        }
        try {
            ServiceHolder.getAnalyticsDataService().put(records);
        } catch (AnalyticsException e) {
            throw new IllegalStateException("Error in adding records to analytics event table: " + 
                    e.getMessage(), e);
        }
    }
    
    private static Record streamEventToRecord(int tenantId, String tableName, List<Attribute> attrs,
            StreamEvent event) {
        Map<String, Object> values = streamEventToRecordValues(tenantId, tableName, attrs, event);
        return new Record(tenantId, tableName, values, event.getTimestamp());
    }
    
    private static Map<String, Object> streamEventToRecordValues(int tenantId, String tableName, List<Attribute> attrs,
            ComplexEvent event) {
        Object[] data = event.getOutputData();
        Map<String, Object> values = new HashMap<String, Object>();
        for (int i = 0; i < attrs.size(); i++) {
            if (data.length > i) {
                values.put(attrs.get(i).getName(), data[i]);
            } else {
                break;
            }
        }
        return values;
    }
    
    public static Record getRecordWithEventValues(int tenantId, String tableName, List<Attribute> attrs,
            ComplexEvent event) {
        try {
            Map<String, Object> values = streamEventToRecordValues(tenantId, tableName, attrs, event);
            List<Map<String, Object>> valuesBatch = new ArrayList<Map<String,Object>>();
            valuesBatch.add(values);
            RecordGroup rgs[] = ServiceHolder.getAnalyticsDataService().getWithKeyValues(tenantId, tableName, 1, null, valuesBatch);
            List<Record> records = GenericUtils.listRecords(ServiceHolder.getAnalyticsDataService(), rgs);
            if (records.size() > 0) {
                return records.get(0);
            } else {
                return null;
            }
        } catch (AnalyticsException e) {
            throw new IllegalStateException("Error in getting event records with values: " + e.getMessage(), e);
        }
    }
    
    public static StreamEvent recordToStreamEvent(List<Attribute> attrs, Record record) {
        StreamEvent event = new StreamEvent(0, 0, attrs.size());
        Object[] data = new Object[attrs.size()];
        for (int i = 0; i < attrs.size(); i++) {
            data[i] = record.getValue(attrs.get(i).getName());
        }
        return event;
    }
    
}