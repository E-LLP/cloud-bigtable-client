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
package com.google.cloud.bigtable.dataflow.coders;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.hadoop.hbase.client.Result;

import com.google.bigtable.repackaged.com.google.cloud.hbase.adapters.Adapters;
import com.google.bigtable.repackaged.com.google.com.google.bigtable.v2.Row;
import com.google.cloud.dataflow.sdk.coders.AtomicCoder;
import com.google.cloud.dataflow.sdk.coders.Coder;

/**
 * An {@link AtomicCoder} that serializes and deserializes the {@link Result}.
 */
public class HBaseResultCoder extends AtomicCoder<Result> {

  private static final long serialVersionUID = -4975428837770254686L;

  private static final HBaseResultCoder INSTANCE = new HBaseResultCoder();

  public static HBaseResultCoder getInstance() {
    return INSTANCE;
  }

  @Override
  public Result decode(InputStream inputStream, Coder.Context context)
      throws IOException {
    return Adapters.ROW_ADAPTER.adaptResponse(Row.parseDelimitedFrom(inputStream));
  }

  @Override
  public void encode(Result value, OutputStream outputStream, Coder.Context context)
      throws IOException {
    Adapters.ROW_ADAPTER.adaptToRow(value).writeDelimitedTo(outputStream);
  }
}
