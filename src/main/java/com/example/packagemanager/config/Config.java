package com.example.packagemanager.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {

  private static final String PROPERTIES_FILE = "config.properties";
  private static final Properties props = new Properties();

  static {
    try (InputStream in = Config.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
      if (in == null) {
        throw new IllegalStateException("File " + PROPERTIES_FILE + " not found in classpath!");
      }
      props.load(in);
    } catch (IOException e) {
      throw new ExceptionInInitializerError("Failed to load config.properties: " + e);
    }
  }

  public static long pollIntervalMs() {
    return Long.parseLong(props.getProperty("poll.interval.ms", "300000"));
  }

  public static String dbUrl() {
    return props.getProperty("db.url");
  }

  public static String dbUser() {
    return props.getProperty("db.user");
  }

  public static String dbPassword() {
    return props.getProperty("db.password");
  }

  public static String storeBaseUrl() {
    return props.getProperty("store.base.url");
  }

  public static String authToken() {
    return props.getProperty("auth.token");
  }

  public static String get(String key) {
    return props.getProperty(key);
  }
}
