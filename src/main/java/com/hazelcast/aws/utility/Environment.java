package com.hazelcast.aws.utility;

/**
 * This class is used to lookup env vars, so that we can use mocks in our tests,
 * when checking for the presence of an env var.
 */
public class Environment {
  public String getEnvVar(String name){
    return System.getenv(name);
  }
}
