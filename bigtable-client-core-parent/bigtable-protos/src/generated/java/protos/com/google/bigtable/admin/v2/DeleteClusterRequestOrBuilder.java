// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: google/bigtable/admin/v2/bigtable_instance_admin.proto

package com.google.bigtable.admin.v2;

public interface DeleteClusterRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:google.bigtable.admin.v2.DeleteClusterRequest)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>optional string name = 1;</code>
   *
   * <pre>
   * The unique name of the cluster to be deleted. Values are of the form
   * projects/&lt;project&gt;/instances/&lt;instance&gt;/clusters/&lt;cluster&gt;
   * </pre>
   */
  java.lang.String getName();
  /**
   * <code>optional string name = 1;</code>
   *
   * <pre>
   * The unique name of the cluster to be deleted. Values are of the form
   * projects/&lt;project&gt;/instances/&lt;instance&gt;/clusters/&lt;cluster&gt;
   * </pre>
   */
  com.google.protobuf.ByteString
      getNameBytes();
}
