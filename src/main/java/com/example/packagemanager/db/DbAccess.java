package com.example.packagemanager.db;

import com.example.packagemanager.config.Config;
import com.example.packagemanager.model.PackageSyncRow;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public final class DbAccess {

  /*
   * ────────────────────────────────────────────────────────────────
   * 0. Connectivity sanity-check
   * ────────────────────────────────────────────────────────────────
   */
  public static void testConnection() {
    try (Connection c = DriverManager.getConnection(
        Config.dbUrl(), Config.dbUser(), Config.dbPassword());
        Statement s = c.createStatement();
        ResultSet rs = s.executeQuery("SELECT SYSDATETIMEOFFSET() AS Now")) {

      if (rs.next())
        System.out.println("✅  Connected!  SQL Server time = " + rs.getString("Now"));
    } catch (SQLException ex) {
      System.err.println("❌  DB error → " + ex.getMessage());
    }
  }

  /*
   * ────────────────────────────────────────────────────────────────
   * 1. Rows that still need syncing
   * ────────────────────────────────────────────────────────────────
   */
  public static List<PackageSyncRow> getRowsNeedingSync() throws SQLException {

    final String sql = """
        SELECT  clientId,
                packageName,
                packageChecksum,
                packageVersion,      -- NEW
                packageLocation,
                syncStatus,
                lastSyncTime,
                modifiedTime
          FROM  PackageSync
         WHERE  syncStatus <> 'SUCCESS'
        """;

    try (Connection c = DriverManager.getConnection(
        Config.dbUrl(), Config.dbUser(), Config.dbPassword());
        PreparedStatement ps = c.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {

      List<PackageSyncRow> list = new ArrayList<>();

      while (rs.next()) {
        list.add(new PackageSyncRow(
            rs.getString("clientId"),
            rs.getString("packageName"),
            rs.getString("packageChecksum"),
            rs.getString("packageLocation"),
            rs.getString("packageVersion"), // NEW
            rs.getString("syncStatus"),
            rs.getObject("lastSyncTime", OffsetDateTime.class),
            rs.getObject("modifiedTime", OffsetDateTime.class)));
      }
      return list;
    }
  }

  /*
   * ────────────────────────────────────────────────────────────────
   * 2. Find on-disk target path
   * ────────────────────────────────────────────────────────────────
   */
  public static String getPackagePath(String clientId, String packageName) throws SQLException {

    final String sql = """
        SELECT packagePath
          FROM PackageTargets
         WHERE clientId    = ?
           AND packageName = ?
        """;

    try (Connection c = DriverManager.getConnection(
        Config.dbUrl(), Config.dbUser(), Config.dbPassword());
        PreparedStatement ps = c.prepareStatement(sql)) {

      ps.setString(1, clientId);
      ps.setString(2, packageName);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next())
          return rs.getString("packagePath");
      }
    }
    return null;
  }

  /*
   * ────────────────────────────────────────────────────────────────
   * 3. Update syncStatus only
   * ────────────────────────────────────────────────────────────────
   */
  public static void updateSyncStatus(String clientId,
      String packageName,
      String newStatus) throws SQLException {

    final String sql = """
        UPDATE PackageSync
           SET syncStatus   = ?,
               modifiedTime = SYSUTCDATETIME()
         WHERE clientId     = ?
           AND packageName  = ?
        """;

    try (Connection c = DriverManager.getConnection(
        Config.dbUrl(), Config.dbUser(), Config.dbPassword());
        PreparedStatement ps = c.prepareStatement(sql)) {

      ps.setString(1, newStatus);
      ps.setString(2, clientId);
      ps.setString(3, packageName);
      ps.executeUpdate();
    }
  }

  /*
   * ────────────────────────────────────────────────────────────────
   * 4. Mark SUCCESS + store checksum & version
   * ────────────────────────────────────────────────────────────────
   */
  public static void markSuccess(String clientId,
      String packageName,
      String sha256,
      String version) throws SQLException {

    final String sql = """
        UPDATE PackageSync
           SET syncStatus      = 'SUCCESS',
               packageChecksum = ?,
               packageVersion  = ?,            -- NEW
               modifiedTime    = SYSUTCDATETIME()
         WHERE clientId        = ?
           AND packageName     = ?
        """;

    try (Connection c = DriverManager.getConnection(
        Config.dbUrl(), Config.dbUser(), Config.dbPassword());
        PreparedStatement ps = c.prepareStatement(sql)) {

      ps.setString(1, sha256);
      ps.setString(2, version);
      ps.setString(3, clientId);
      ps.setString(4, packageName);
      ps.executeUpdate();
    }
  }

  private DbAccess() {
  } // static-only
}
