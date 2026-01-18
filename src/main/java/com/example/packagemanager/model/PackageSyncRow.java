package com.example.packagemanager.model;

import java.time.OffsetDateTime;

/** Immutable record-type representing one row from PackageSync. */
public final class PackageSyncRow {

  private final String clientId;
  private final String packageName;
  private final String packageChecksum;
  private final String packageLocation;
  private final String packageVersion;
  private final SyncStatus syncStatus;
  private final OffsetDateTime lastSyncTime;
  private final OffsetDateTime modifiedTime;

  /* Constructor used by code (enum form) */
  public PackageSyncRow(String clientId,
      String packageName,
      String packageChecksum,
      String packageLocation,
      String packageVersion,
      SyncStatus syncStatus,
      OffsetDateTime lastSyncTime,
      OffsetDateTime modifiedTime) {

    this.clientId = clientId;
    this.packageName = packageName;
    this.packageChecksum = packageChecksum;
    this.packageLocation = packageLocation;
    this.packageVersion = packageVersion;
    this.syncStatus = syncStatus;
    this.lastSyncTime = lastSyncTime;
    this.modifiedTime = modifiedTime;
  }

  /* Overload that accepts raw text from the DB */
  public PackageSyncRow(String clientId,
      String packageName,
      String packageChecksum,
      String packageLocation,
      String packageVersion,
      String syncStatusText,
      OffsetDateTime lastSyncTime,
      OffsetDateTime modifiedTime) {

    this(clientId,
        packageName,
        packageChecksum,
        packageLocation,
        packageVersion,
        SyncStatus.valueOf(syncStatusText.toUpperCase()),
        lastSyncTime,
        modifiedTime);
  }

  /* —— getters —— */
  public String clientId() {
    return clientId;
  }

  public String packageName() {
    return packageName;
  }

  public String packageChecksum() {
    return packageChecksum;
  }

  public String packageLocation() {
    return packageLocation;
  }

  public String packageVersion() {
    return packageVersion;
  }

  public SyncStatus syncStatus() {
    return syncStatus;
  }

  public OffsetDateTime lastSyncTime() {
    return lastSyncTime;
  }

  public OffsetDateTime modifiedTime() {
    return modifiedTime;
  }

  @Override
  public String toString() {
    return "PackageSyncRow[" +
        "clientId=" + clientId +
        ", packageName=" + packageName +
        ", version=" + packageVersion +
        ", syncStatus=" + syncStatus + ']';
  }
}
