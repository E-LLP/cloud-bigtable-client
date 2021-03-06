/*
 * Copyright 2015 Google Inc. All Rights Reserved. Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the License. You may obtain
 * a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 * for the specific language governing permissions and limitations under the License.
 */
package org.apache.hadoop.hbase.client;

import com.google.bigtable.v2.MutateRowRequest;
import com.google.cloud.bigtable.config.BigtableOptions;
import com.google.cloud.bigtable.grpc.BigtableDataClient;
import com.google.cloud.bigtable.grpc.BigtableDataGrpcClient;
import com.google.cloud.bigtable.grpc.BigtableSession;
import com.google.cloud.bigtable.grpc.BigtableSessionSharedThreadPools;
import com.google.cloud.bigtable.grpc.io.ChannelPool;
import com.google.cloud.bigtable.grpc.io.CredentialInterceptorCache;
import com.google.cloud.bigtable.grpc.io.HeaderInterceptor;
import com.google.cloud.bigtable.hbase.DataGenerationHelper;
import com.google.cloud.bigtable.hbase.adapters.HBaseRequestAdapter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.protobuf.Empty;
import com.google.protobuf.ServiceException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.util.Bytes;

import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PutMicroBenchmark {
  static final int NUM_CELLS = 100;
  public static final byte[] COLUMN_FAMILY = Bytes.toBytes("test_family");
  final static int PUT_COUNT = 100;

  public static void main(String[] args)
      throws IOException, ServiceException, InterruptedException, GeneralSecurityException {
    String projectId = args.length > 0 ? args[0] : "project";
    String zoneId = args.length > 1 ? args[1] : "zone";
    String clusterId = args.length > 2 ? args[2] : "cluster";
    String tableId = args.length > 3 ? args[3] : "table";

    ChannelPool cp = getChannelPool(args.length >= 3);
    MutateRowRequest request = createRequest(projectId, zoneId, clusterId, tableId);
    run(request, cp);
  }

  protected static ChannelPool getChannelPool(final boolean useRealConnection)
      throws IOException, GeneralSecurityException {
    if (useRealConnection) {
      return createNettyChannelPool();
    } else {
      return new ChannelPool(ImmutableList.<HeaderInterceptor> of(), createFakeChannels());
    }
  }

  protected static ChannelPool createNettyChannelPool()
      throws IOException, GeneralSecurityException {
    final BigtableOptions options = new BigtableOptions.Builder().setUserAgent("put_microbenchmark").build();
    return new ChannelPool(getHeaders(options), new ChannelPool.ChannelFactory() {
      @Override
      public ManagedChannel create() throws IOException {
        return BigtableSession.createNettyChannel(options.getDataHost(), options);
      }
    });
  }

  protected static ImmutableList<HeaderInterceptor> getHeaders(BigtableOptions options)
      throws IOException, GeneralSecurityException {
    CredentialInterceptorCache credentialsCache = CredentialInterceptorCache.getInstance();
    HeaderInterceptor headerInterceptor = credentialsCache
        .getCredentialsInterceptor(options.getCredentialOptions(), options.getRetryOptions());
    Builder<HeaderInterceptor> headerInterceptorBuilder = new ImmutableList.Builder<>();
    if (headerInterceptor != null) {
      headerInterceptorBuilder.add(headerInterceptor);
    }
    return headerInterceptorBuilder.build();
  }

  protected static ChannelPool.ChannelFactory createFakeChannels() {
    final ManagedChannel channel = createFakeChannel();
    return new ChannelPool.ChannelFactory() {
      @Override
      public ManagedChannel create() throws IOException {
        return channel;
      }
    };
  }

  protected static MutateRowRequest createRequest(String projectId, String zoneId, String clusterId,
      String tableId) {
    HBaseRequestAdapter hbaseAdapter = new HBaseRequestAdapter(
        new BigtableOptions.Builder().build(), TableName.valueOf(tableId),
        new Configuration(false));
    DataGenerationHelper dataHelper = new DataGenerationHelper();
    byte[] rowKey = dataHelper.randomData("testrow-");
    byte[][] quals = dataHelper.randomData("testQualifier-", NUM_CELLS);
    byte[][] values = dataHelper.randomData("testValue-", NUM_CELLS);

    Put put = new Put(rowKey);
    List<QualifierValue> keyValues = new ArrayList<QualifierValue>(NUM_CELLS);
    for (int i = 0; i < NUM_CELLS; ++i) {
      put.addColumn(COLUMN_FAMILY, quals[i], values[i]);
      keyValues.add(new QualifierValue(quals[i], values[i]));
    }
    return hbaseAdapter.adapt(put);
  }

  protected static void run(final MutateRowRequest request, ChannelPool cp)
      throws InterruptedException {
    final BigtableDataClient client = new BigtableDataGrpcClient(cp,
        BigtableSessionSharedThreadPools.getInstance().getRetryExecutor(),
        new BigtableOptions.Builder().build());

    Runnable r = new Runnable() {
      @Override
      public void run() {
        long start = System.nanoTime();
        for (int i = 0; i < PUT_COUNT; i++) {
          try {
            client.mutateRow(request);
          } catch (ServiceException e) {
            // TODO(sduskis): Auto-generated catch block
            e.printStackTrace();
          }
        }
        long totalTime = System.nanoTime() - start;
        System.out.println(String.format("Put %d in %d ms.  %d nanos/put.  %f put/sec", PUT_COUNT,
          totalTime / 1000000, totalTime / PUT_COUNT, PUT_COUNT * 1000000000.0 / totalTime));
      }
    };

    int roundCount = 20;
    System.out.println("====== Running serially");
    for (int i = 0; i < roundCount; i++) {
      r.run();
    }
    System.out.println("====== Running in parallel");
    ExecutorService e = Executors.newFixedThreadPool(roundCount);
    for (int i = 0; i < roundCount; i++) {
      e.execute(r);
    }
    e.shutdown();
    e.awaitTermination(10, TimeUnit.HOURS);
    System.out.println("====== Running serially");
    for (int i = 0; i < roundCount; i++) {
      r.run();
    }
  }

  final static ManagedChannel channel = new ManagedChannel() {
    @Override
    public <RequestT, ResponseT> ClientCall<RequestT, ResponseT>
        newCall(MethodDescriptor<RequestT, ResponseT> methodDescriptor, CallOptions callOptions) {
      return createNewCall();
    }

    @Override
    public String authority() {
      return null;
    }

    @Override
    public ManagedChannel shutdownNow() {
      return null;
    }

    @Override
    public ManagedChannel shutdown() {
      return null;
    }

    @Override
    public boolean isTerminated() {
      return false;
    }

    @Override
    public boolean isShutdown() {
      return false;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
      return false;
    }
  };

  private static ManagedChannel createFakeChannel() {
    return channel;
  }

  /**
   * @return
   */
  protected static <RequestT, ResponseT> ClientCall<RequestT, ResponseT> createNewCall() {
    return new ClientCall<RequestT, ResponseT>() {

      private ClientCall.Listener<ResponseT> responseListener;

      @Override
      public void start(ClientCall.Listener<ResponseT> responseListener, Metadata headers) {
        this.responseListener = responseListener;
      }

      @Override
      public void request(int numMessages) {
      }

      @Override
      public void cancel() {
      }

      @Override
      public void halfClose() {
      }

      @Override
      @SuppressWarnings("unchecked")
      public void sendMessage(RequestT message) {
        responseListener.onMessage((ResponseT) Empty.getDefaultInstance());
        responseListener.onClose(Status.OK, null);
      }
    };
  }

  protected static class QualifierValue implements Comparable<QualifierValue> {
    protected final byte[] qualifier;
    protected final byte[] value;

    public QualifierValue(byte[] qualifier, byte[] value) {
      this.qualifier = qualifier;
      this.value = value;
    }

    @Override
    public int compareTo(QualifierValue qualifierValue) {
      return Bytes.compareTo(this.qualifier, qualifierValue.qualifier);
    }
  }
}
