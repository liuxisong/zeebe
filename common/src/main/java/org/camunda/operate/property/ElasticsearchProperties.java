/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.operate.property;

public class ElasticsearchProperties {

  public static final String DATE_FORMAT_DEFAULT = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ";

  public static final String ELS_DATE_FORMAT_DEFAULT = "date_time";
  
  public static final String DEFAULT_INDEX_PREFIX = "operate";

  private String indexPrefix = DEFAULT_INDEX_PREFIX;

  private String clusterName= "elasticsearch";

  private String host = "localhost";

  private int port = 9200;

  private String dateFormat = DATE_FORMAT_DEFAULT;

  private String elsDateFormat = ELS_DATE_FORMAT_DEFAULT;

  private int batchSize = 2000;

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getDateFormat() {
    return dateFormat;
  }

  public void setDateFormat(String dateFormat) {
    this.dateFormat = dateFormat;
  }

  public String getElsDateFormat() {
    return elsDateFormat;
  }

  public void setElsDateFormat(String elsDateFormat) {
    this.elsDateFormat = elsDateFormat;
  }

  public int getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }

  public String getIndexPrefix() {
    return indexPrefix;
  }

  public void setIndexPrefix(String indexPrefix) {
    this.indexPrefix = indexPrefix;
  }
}
