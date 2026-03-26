/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.sql.opensearch.storage;

import java.util.function.Function;
import org.opensearch.common.unit.TimeValue;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.WrapperQueryBuilder;
import org.opensearch.sql.common.setting.Settings;
import org.opensearch.sql.opensearch.client.OpenSearchClient;
import org.opensearch.sql.opensearch.request.OpenSearchRequestBuilder;
import org.opensearch.sql.opensearch.storage.scan.OpenSearchIndexScan;
import org.opensearch.sql.opensearch.storage.scan.OpenSearchIndexScanBuilder;
import org.opensearch.sql.storage.read.TableScanBuilder;

/**
 * Vector-search-aware OpenSearch index. Seeds the scan with a knn query and enables score tracking.
 */
public class VectorSearchIndex extends OpenSearchIndex {

  private final String field;
  private final float[] vector;
  private final int k;

  public VectorSearchIndex(
      OpenSearchClient client,
      Settings settings,
      String indexName,
      String field,
      float[] vector,
      int k) {
    super(client, settings, indexName);
    this.field = field;
    this.vector = vector;
    this.k = k;
  }

  @Override
  public TableScanBuilder createScanBuilder() {
    final TimeValue cursorKeepAlive =
        getSettings().getSettingValue(Settings.Key.SQL_CURSOR_KEEP_ALIVE);
    var builder = createRequestBuilder();

    // Inject knn query
    builder.pushDownFilter(buildKnnQuery());
    builder.pushDownTrackedScore(true);

    Function<OpenSearchRequestBuilder, OpenSearchIndexScan> createScanOperator =
        requestBuilder ->
            new OpenSearchIndexScan(
                getClient(),
                requestBuilder.getMaxResponseSize(),
                requestBuilder.build(
                    getIndexName(), cursorKeepAlive, getClient(), getFieldTypes().isEmpty()));
    return new OpenSearchIndexScanBuilder(builder, createScanOperator);
  }

  private QueryBuilder buildKnnQuery() {
    StringBuilder vectorJson = new StringBuilder("[");
    for (int i = 0; i < vector.length; i++) {
      if (i > 0) vectorJson.append(",");
      vectorJson.append(vector[i]);
    }
    vectorJson.append("]");

    String knnQueryJson =
        String.format(
            "{\"knn\":{\"%s\":{\"vector\":%s,\"k\":%d}}}", field, vectorJson.toString(), k);
    return new WrapperQueryBuilder(knnQueryJson);
  }
}
