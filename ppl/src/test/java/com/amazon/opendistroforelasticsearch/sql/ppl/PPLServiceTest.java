/*
 *   Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file. This file is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *   express or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 */

package com.amazon.opendistroforelasticsearch.sql.ppl;

import com.amazon.opendistroforelasticsearch.sql.common.response.ResponseListener;
import com.amazon.opendistroforelasticsearch.sql.data.model.ExprType;
import com.amazon.opendistroforelasticsearch.sql.executor.ExecutionEngine;
import com.amazon.opendistroforelasticsearch.sql.executor.ExecutionEngine.QueryResponse;
import com.amazon.opendistroforelasticsearch.sql.planner.physical.PhysicalPlan;
import com.amazon.opendistroforelasticsearch.sql.ppl.config.PPLServiceConfig;
import com.amazon.opendistroforelasticsearch.sql.ppl.domain.PPLQueryRequest;
import com.amazon.opendistroforelasticsearch.sql.storage.StorageEngine;
import com.amazon.opendistroforelasticsearch.sql.storage.Table;
import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PPLServiceTest {
    private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    private PPLService pplService;

    @Mock
    private StorageEngine storageEngine;

    @Mock
    private ExecutionEngine executionEngine;

    @Mock
    private Table table;

    @Mock
    private PhysicalPlan plan;

    @Before
    public void setUp() {
        when(table.getFieldTypes()).thenReturn(ImmutableMap.of("a", ExprType.INTEGER));
        when(table.implement(any())).thenReturn(plan);
        when(storageEngine.getTable(any())).thenReturn(table);

        context.registerBean(StorageEngine.class, () -> storageEngine);
        context.registerBean(ExecutionEngine.class, () -> executionEngine);
        context.register(PPLServiceConfig.class);
        context.refresh();
        pplService = context.getBean(PPLService.class);
    }

    @Test
    public void testExecuteShouldPass() {
        doAnswer(invocation -> {
            ResponseListener<QueryResponse> listener = invocation.getArgument(1);
            listener.onResponse(new QueryResponse(Collections.emptyList()));
            return null;
        }).when(executionEngine).execute(any(), any());

        pplService.execute(new PPLQueryRequest("search source=t a=1", null), new ResponseListener<QueryResponse>() {
            @Override
            public void onResponse(QueryResponse pplQueryResponse) {

            }

            @Override
            public void onFailure(Exception e) {
                Assert.fail();
            }
        });
    }

    @Test
    public void testExecuteWithIllegalQueryShouldBeCaughtByHandler() {
        pplService.execute(new PPLQueryRequest("search", null), new ResponseListener<QueryResponse>() {
            @Override
            public void onResponse(QueryResponse pplQueryResponse) {
                Assert.fail();
            }

            @Override
            public void onFailure(Exception e) {

            }
        });
    }
}