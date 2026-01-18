package com.example.packagemanager;

import com.example.packagemanager.config.Config;
import com.example.packagemanager.db.DbAccess;
import com.example.packagemanager.model.PackageSyncRow;
import com.example.packagemanager.service.PackageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Entry-point – runs forever, waking up every pollIntervalMs()
 * to ask the DB, download / upload packages, and update SyncStatus.
 */
public class MainController {

  /** SLF4J portable logger (Logback will be the runtime backend) */
  private static final Logger log = LoggerFactory.getLogger(MainController.class);

  public static void main(String[] args) {
    log.info("🚀 Commvault-Sync starting…");

    /* 0️⃣ Quick sanity-check – proves the DB creds still work */
    DbAccess.testConnection();

    /* 1️⃣ Echo a few config settings so we see they loaded */
    log.info("Poll interval (ms):   {}", Config.pollIntervalMs());
    log.info("DB URL              : {}", Config.dbUrl());
    log.info("Store Base URL      : {}", Config.storeBaseUrl());

    /*
     * ───────────────────────────────────────────────────────────
     * 2️⃣ Main loop – runs forever (Ctrl-C to exit)
     * ───────────────────────────────────────────────────────────
     */
    while (true) {
      try {
        syncAll();
      } catch (Exception e) {
        log.error("❌  Sync cycle failed", e);
      }

      /* 3️⃣ Sleep until the next cycle */
      try {
        Thread.sleep(Config.pollIntervalMs());
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt(); // graceful exit
        break;
      }
    }
  }

  /** Runs one complete sync cycle */
  private static void syncAll() throws Exception {
    List<PackageSyncRow> rows = DbAccess.getRowsNeedingSync();

    if (rows.isEmpty()) {
      log.info("✅ Nothing to sync – table is up-to-date.");
      return;
    }

    log.info("📦 {} row(s) require syncing", rows.size());
    rows.forEach(r -> log.debug("↳ {}", r));

    /* Download / upload and flip SyncStatus → SUCCESS */
    PackageService.downloadAndUpload(rows);
  }
}
