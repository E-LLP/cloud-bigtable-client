/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.bigtable.hbase.adapters.filters;

import com.google.bigtable.v2.RowFilter;

import org.apache.hadoop.hbase.filter.RandomRowFilter;

import java.io.IOException;

/**
 * Adapter for {@link org.apache.hadoop.hbase.filter.RandomRowFilter}
 */
public class RandomRowFilterAdapter implements TypedFilterAdapter<RandomRowFilter> {
  @Override
  public RowFilter adapt(FilterAdapterContext context, RandomRowFilter filter)
      throws IOException {
    return RowFilter.newBuilder()
        .setRowSampleFilter(filter.getChance())
        .build();
  }

  @Override
  public FilterSupportStatus isFilterSupported(
      FilterAdapterContext context, RandomRowFilter filter) {
    return FilterSupportStatus.SUPPORTED;
  }
}
