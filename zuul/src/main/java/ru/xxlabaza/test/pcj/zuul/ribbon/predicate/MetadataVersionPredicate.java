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
package ru.xxlabaza.test.pcj.zuul.ribbon.predicate;

import static com.netflix.appinfo.InstanceInfo.InstanceStatus.UP;
import static java.util.stream.Collectors.toSet;
import static ru.xxlabaza.test.pcj.zuul.filters.pre.version.AbstractTargetServiceVersionExctractorFilter.TARGET_SERVICE_VERSION_KEY;
import static ru.xxlabaza.test.pcj.zuul.ribbon.predicate.PredicateOrders.MATADATA_VERSION_ORDER;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.loadbalancer.Server;
import com.netflix.zuul.context.RequestContext;
import lombok.val;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient.EurekaServiceInstance;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;
import ru.xxlabaza.test.pcj.zuul.ribbon.MetadataBalancingProperties;
import ru.xxlabaza.test.pcj.zuul.ribbon.MetadataBalancingProperties.Range;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

/**
 * @author Artem Labazin <xxlabaza@gmail.com>
 * @since 22.03.2017
 */
@Slf4j
@Component
public class MetadataVersionPredicate extends AbstractPredicate {

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

  public MetadataVersionPredicate() {
    super(MATADATA_VERSION_ORDER);
  }

  @Override
  protected boolean shouldApply(RequestContext requestContext) {
    val rules = metadataBalancingProperties.getRules();
    val serviceId = requestContext.getOrDefault("serviceId", "").toString();
    return (rules != null && rules.containsKey(serviceId)) ||
           (requestContext.containsKey(TARGET_SERVICE_VERSION_KEY) && isHeaderVersionRequest());
  }

  @Override
  protected boolean apply(Server server, InstanceInfo instanceInfo) {
    val appName = instanceInfo.getAppName().toLowerCase();
    val metadata = instanceInfo.getMetadata();
    if (metadata == null) {
      return true;
    }
    val appVersion = metadata.getOrDefault("version", "0");
    log.debug("Checking service '{}:{}'", appName, appVersion);

    val requestContextVersion = getRequestContextServiceVersion(appName, appVersion);
    val result =  requestContextVersion != null
                  ? requestContextVersion.equals(appVersion)
                  : true;

    if (result) {
      log.debug("Service '{}:{}' was chosen", instanceInfo.getHostName(), appVersion);
    } else {
      log.debug("Service '{}:{}' was not chosen", instanceInfo.getHostName(), appVersion);
    }
    return result;
  }

  private String getRequestContextServiceVersion(String appName, String appVersion) {
    val requestContext = RequestContext.getCurrentContext();
    if (requestContext.containsKey(CURRENT_REQUEST_CONTEXT_VERSION)) {
      val version = requestContext.get(CURRENT_REQUEST_CONTEXT_VERSION).toString();
      log.debug("Request context version is {}", version);
      return version;
    }

    val availableVersions = discoveryClient.getInstances(appName)
        .stream()
        .filter(it -> it instanceof EurekaServiceInstance)
        .map(it -> ((EurekaServiceInstance) it).getInstanceInfo())
        .filter(it -> it.getStatus() == UP)
        .map(it -> it.getMetadata())
        .filter(Objects::nonNull)
        .map(it -> it.get("version"))
        .filter(Objects::nonNull)
        .collect(toSet());

    val isVersionMigration = isMigration(appName);
    if (isVersionMigration) {
      log.debug("{} has migration version", appName);
      availableVersions.add("0");
    }

    val latestVersion = availableVersions
        .stream()
        .map(DefaultArtifactVersion::new)
        .max(Comparator.naturalOrder())
        .map(DefaultArtifactVersion::toString)
        .orElse(appVersion);
    log.debug("Latest version of {} is {}", appName, latestVersion);

    val targetVersion = requestContext.getOrDefault(TARGET_SERVICE_VERSION_KEY, "").toString();
    if (!targetVersion.isEmpty()) {
      if (targetVersion.equalsIgnoreCase("latest")) {
        log.debug("Set context version for {} as {} (the latest)", appName, latestVersion);
        requestContext.set(CURRENT_REQUEST_CONTEXT_VERSION, latestVersion);
        return latestVersion;
      }
      if (isHeaderVersionRequest()) {
        log.debug("Header version detected, trying to find best matching version...");
        val version = getBestMatchingVersion(availableVersions, latestVersion, targetVersion);
        log.debug("Set context version for {} as {}", appName, version);
        requestContext.set(CURRENT_REQUEST_CONTEXT_VERSION, version);
        return version;
      }
      if (!isVersionMigration) {
        availableVersions.removeIf(it -> !it.startsWith(targetVersion));
      }
    }

    val trafficRules = metadataBalancingProperties.getRules();
    if (trafficRules == null || !trafficRules.containsKey(appName)) {
      requestContext.set(CURRENT_REQUEST_CONTEXT_VERSION, appVersion);
      log.debug("No traffic rules for {}, set context version as {}",
                appName, appVersion);
      return appVersion;
    }

    val ranges = new HashMap<>(metadataBalancingProperties.getRanges().get(appName));

    val rangeRule = trafficRules.get(appName);
    if (isVersionMigration && !rangeRule.containsKey("0")) {
      val value = rangeRule.values().iterator().next();
      rangeRule.put("0", 100 - value);
      ranges.put("0", new Range(value, 100));
    }

    val bound = rangeRule.values().stream().mapToInt(it -> it).sum();
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
    requestContext.set(CURRENT_REQUEST_CONTEXT_VERSION, version);
    return version;
  }

  private boolean isHeaderVersionRequest() {
    val requestContext = RequestContext.getCurrentContext();
    val request = requestContext.getRequest();
    val targetServiceVersion = metadataBalancingProperties.getHeaderName();
    val requestCookieName = metadataBalancingProperties.getRequestCookieName();
    return request.getHeader(targetServiceVersion) != null ||
           WebUtils.getCookie(request, requestCookieName) != null;
  }

  private String getBestMatchingVersion(Set<String> available, String latest, String version) {
    if (available.contains(version)) {
      return version;
    }
    return available.stream()
        .filter(it -> it.startsWith(version))
        .map(DefaultArtifactVersion::new)
        .max(Comparator.naturalOrder())
        .map(DefaultArtifactVersion::toString)
        .orElse(latest);
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

    return (ranges.size() == 1 || ranges.containsKey("0")) && hasOldVersions;
  }
}
