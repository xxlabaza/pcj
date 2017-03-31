/*
 * Copyright 2017 Artem Labazin <xxlabaza@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.xxlabaza.test.pcj.balancing.predicate;

import static com.netflix.appinfo.InstanceInfo.InstanceStatus.UP;
import static java.util.stream.Collectors.toSet;

import com.netflix.loadbalancer.AbstractServerPredicate;
import com.netflix.loadbalancer.PredicateKey;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient.EurekaServiceInstance;
import org.springframework.stereotype.Component;
import ru.xxlabaza.test.pcj.balancing.MetadataBalancingProperties;
import ru.xxlabaza.test.pcj.balancing.MetadataBalancingProperties.Range;
import ru.xxlabaza.test.pcj.balancing.PredicateContextHolder;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;

/**
 * @author Artem Labazin <xxlabaza@gmail.com>
 * @since 22.03.2017
 */
@Slf4j
@Component
public class MetadataVersionPredicate extends AbstractServerPredicate {

  public final static String CURRENT_REQUEST_CONTEXT_VERSION;

  private final static Random RANDOM;

  static {
    CURRENT_REQUEST_CONTEXT_VERSION = "current-request-context-version";
    RANDOM = new Random(System.currentTimeMillis());
  }

  @Autowired
  private MetadataBalancingProperties metadataBalancingProperties;

  @Autowired
  private DiscoveryClient discoveryClient;

  @Override
  public boolean apply(PredicateKey predicateKey) {
    val server = predicateKey.getServer();
    if (!(server instanceof DiscoveryEnabledServer)) {
      return true;
    }
    val instanceInfo = ((DiscoveryEnabledServer) server).getInstanceInfo();

    val appName = instanceInfo.getAppName().toLowerCase();
    val appVersion = instanceInfo.getMetadata().getOrDefault("version", "0");
    log.debug("Checking service '{}:{}'", appName, appVersion);

    val requestContextVersion = getRequestContextServiceVersion(appName, appVersion);
    val result = requestContextVersion.equals(appVersion);

    if (result) {
      log.info("Service '{}:{}' was chosen", appName, appVersion);
    } else {
      log.info("Service '{}:{}' was not chosen", appName, appVersion);
    }
    return result;
  }

  private String getRequestContextServiceVersion(String appName, String appVersion) {
    PredicateContextHolder currentContext = PredicateContextHolder.getCurrentContext();
    if (currentContext.containsKey(CURRENT_REQUEST_CONTEXT_VERSION)) {
      val version = currentContext.getString(CURRENT_REQUEST_CONTEXT_VERSION);
      log.debug("Request context version is {}", version);
      return version;
    }

    val availableVersions = discoveryClient.getInstances(appName)
        .stream()
        .filter(it -> it instanceof EurekaServiceInstance)
        .map(it -> ((EurekaServiceInstance) it).getInstanceInfo())
        .filter(it -> it.getStatus() == UP)
        .map(it -> it.getMetadata().get("version"))
        .filter(Objects::nonNull)
        .collect(toSet());

    val latestVersion = availableVersions
        .stream()
        .map(DefaultArtifactVersion::new)
        .max(Comparator.naturalOrder())
        .map(DefaultArtifactVersion::toString)
        .orElse(appVersion);
    log.debug("Latest version of {} is {}", appName, latestVersion);

    val trafficRules = metadataBalancingProperties.getRules();
    if (trafficRules == null || !trafficRules.containsKey(appName)) {
      log.debug("No traffic rules for {}, set context version as {}",
               appName, appVersion);
      currentContext.put(CURRENT_REQUEST_CONTEXT_VERSION, appVersion);
      return appVersion;
    }

    val ranges = new HashMap<>(metadataBalancingProperties.getRanges().get(appName));

    if (isMigration(appName)) {
      log.debug("{} has migration version", appName);
      val rangeRule = trafficRules.get(appName);
      val value = rangeRule.values().iterator().next();
      rangeRule.put("0", 100 - value);
      ranges.put("0", new Range(value, 100));
      availableVersions.add("0");
    }

    val bound = trafficRules.get(appName).values().stream().mapToInt(it -> it).sum();
    val value = RANDOM.nextInt(bound);
    log.debug("Random value is {}", value);

    val version = ranges
        .entrySet()
        .stream()
        .filter(it -> {
          Range range = it.getValue();
          return range.getFrom() <= value && value < range.getTo() &&
                 availableVersions.contains(it.getKey());
        })
        .findFirst()
        .map(Entry::getKey)
        .orElse(latestVersion);

    log.debug("Set context version for {} as {}", appName, version);
    currentContext.put(CURRENT_REQUEST_CONTEXT_VERSION, version);
    return version;
  }

  private boolean isMigration(String appName) {
    val allRanges = metadataBalancingProperties.getRanges();
    if (allRanges == null) {
      return true;
    }

    val ranges = allRanges.get(appName);
    if (ranges == null) {
      return true;
    }

    val hasOldVersions = discoveryClient.getInstances(appName).stream()
        .map(it -> it.getMetadata().get("version"))
        .anyMatch(Objects::isNull);

    return ranges.size() == 1 && hasOldVersions;
  }
}
