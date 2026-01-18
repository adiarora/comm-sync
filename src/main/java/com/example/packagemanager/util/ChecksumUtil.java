package com.example.packagemanager.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;

/**
 * Tiny utility for computing SHA-256 checksums of files.
 */
public final class ChecksumUtil {

  /** Returns the lowercase hex SHA-256 of the entire file. */
  public static String sha256(Path file) throws Exception {
    byte[] bytes = Files.readAllBytes(file);
    byte[] digest = MessageDigest
        .getInstance("SHA-256")
        .digest(bytes);

    StringBuilder sb = new StringBuilder(digest.length * 2);
    for (byte b : digest)
      sb.append(String.format("%02x", b));
    return sb.toString();
  }

  private ChecksumUtil() {
  } // static-only
}
