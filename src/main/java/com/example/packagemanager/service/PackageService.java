package com.example.packagemanager.service;

import com.example.packagemanager.config.Config;
import com.example.packagemanager.db.DbAccess;
import com.example.packagemanager.model.PackageSyncRow;
import com.example.packagemanager.model.SyncStatus;
import com.example.packagemanager.util.ChecksumUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

public final class PackageService {

  private static final Logger log = LoggerFactory.getLogger(PackageService.class);

  /** Top-level orchestrator, run once per sync cycle */
  public static void downloadAndUpload(List<PackageSyncRow> rows) throws Exception {

    /* 1️⃣ Fetch latest catalog from store */
    Map<String, StoreClient.CatalogEntry> catalog = StoreClient.fetchCatalog(Config.storeBaseUrl());

    for (PackageSyncRow row : rows) {

      StoreClient.CatalogEntry entry = catalog.get(row.packageName());

      if (entry == null) { // package removed from store
        log.warn("⚠️  {} not in catalog – skipping", row.packageName());
        continue;
      }

      /* Already up-to-date? Compare VERSION first (fast path) */
      if (entry.version().equals(row.packageVersion())) {
        DbAccess.markSuccess(row.clientId(),
            row.packageName(),
            entry.sha256(),
            entry.version());
        continue;
      }

      /* 2️⃣ Download */
      log.info("⬇️  Downloading {} → {}", row.packageName(), entry.version());
      Path localFile = downloadFile(row.packageName());

      /* Verify checksum */
      String downloadedSha = ChecksumUtil.sha256(localFile);
      if (!downloadedSha.equalsIgnoreCase(entry.sha256())) {
        log.error("❌ SHA mismatch – expected {}, got {}", entry.sha256(), downloadedSha);
        DbAccess.updateSyncStatus(row.clientId(), row.packageName(),
            SyncStatus.FAILED.name());
        Files.deleteIfExists(localFile);
        continue;
      }

      /* 3️⃣ Upload (copy to target path) */
      log.info("⬆️  Uploading {}", row.packageName());
      if (!uploadFile(localFile, row, entry.sha256()))
        continue; // uploadFile already logged + marked FAILED

      /* 4️⃣ Mark SUCCESS + store SHA-256 + version */
      DbAccess.markSuccess(row.clientId(),
          row.packageName(),
          downloadedSha,
          entry.version());
      log.info("✅ Marked {} v{} as SUCCESS", row.packageName(), entry.version());
    }
  }

  /* --------------------------------------------------------------- */
  private static Path downloadFile(String packageName) throws Exception {
    Path cachePath = Path.of("cache", packageName);
    return StoreClient.downloadPackage(
        Config.storeBaseUrl(), packageName, cachePath);
  }

  /* --------------------------------------------------------------- */
  private static boolean uploadFile(Path localZip,
      PackageSyncRow row,
      String expectedSha) throws Exception {

    String target = DbAccess.getPackagePath(row.clientId(), row.packageName());
    if (target == null) {
      log.warn("No PackageTargets entry for {} – skipping", row.packageName());
      return false;
    }

    Path targetPath = Path.of(target);
    Files.createDirectories(targetPath.getParent());
    Files.copy(localZip, targetPath, StandardCopyOption.REPLACE_EXISTING);

    String freshSha = ChecksumUtil.sha256(targetPath);
    if (!freshSha.equalsIgnoreCase(expectedSha)) {
      log.error("SHA mismatch after copy – expected {}, got {}", expectedSha, freshSha);
      DbAccess.updateSyncStatus(row.clientId(), row.packageName(),
          SyncStatus.FAILED.name());
      return false;
    }

    log.info("✅ Copied to {} ({} bytes)", targetPath, Files.size(targetPath));
    return true;
  }

  private PackageService() {
  }
}
