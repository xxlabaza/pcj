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
import static ru.xxlabaza.test.pcj.zuul.filters.pre.PreTargetServiceVersionExtractorFilter.TARGET_SERVICE_VERSION_KEY;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.loadbalancer.Server;
import com.netflix.zuul.context.RequestContext;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import lombok.val;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient.EurekaServiceInstance;
import org.springframework.stereotype.Component;
import ru.xxlabaza.test.pcj.zuul.ribbon.MetadataBalancingProperties;
import ru.xxlabaza.test.pcj.zuul.ribbon.MetadataBalancingProperties.Range;

/**
 * @author Artem Labazin <xxlabaza@gmail.com>
 * @since 22.03.2017
 */
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
    super(700);
  }

  @Override
  protected boolean shouldApply(RequestContext requestContext) {
    val rules = metadataBalancingProperties.getRules();
    val serviceId = requestContext.getOrDefault("serviceId", "").toString();
    return (rules != null && rules.containsKey(serviceId)) ||
           requestContext.containsKey(TARGET_SERVICE_VERSION_KEY);
  }

  @Override
  protected boolean apply(Server server, InstanceInfo instanceInfo) {
    val appName = instanceInfo.getAppName().toLowerCase();
    val appVersion = instanceInfo.getMetadata().getOrDefault("version", "0");
    val requestContextVersion = getRequestContextServiceVersion(appName, appVersion);
    return requestContextVersion != null
           ? requestContextVersion.equals(appVersion)
           : true;
  }

  private String getRequestContextServiceVersion(String appName, String appVersion) {
    val requestContext = RequestContext.getCurrentContext();
    if (requestContext.containsKey(CURRENT_REQUEST_CONTEXT_VERSION)) {
      return requestContext.get(CURRENT_REQUEST_CONTEXT_VERSION).toString();
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

    val targetVersion = requestContext.getOrDefault(TARGET_SERVICE_VERSION_KEY, "").toString();
    if (!targetVersion.isEmpty()) {
      if (targetVersion.equalsIgnoreCase("latest")) {
        requestContext.set(CURRENT_REQUEST_CONTEXT_VERSION, latestVersion);
        return latestVersion;
      }
      if (isHeaderVersionRequest()) {
        val version = getBestMatchingVersion(availableVersions, latestVersion, targetVersion);
        requestContext.set(CURRENT_REQUEST_CONTEXT_VERSION, version);
        return version;
      }
      if (!isMigration(appName)) {
        availableVersions.removeIf(it -> !it.startsWith(targetVersion));
      }
    }

    val trafficRules = metadataBalancingProperties.getRules();
    if (trafficRules == null || !trafficRules.containsKey(appName)) {
      requestContext.set(CURRENT_REQUEST_CONTEXT_VERSION, appVersion);
      return appVersion;
    }

    val ranges = new HashMap<>(metadataBalancingProperties.getRanges().get(appName));

    if (isMigration(appName)) {
      val rangeRule = trafficRules.get(appName);
      val value = rangeRule.values().iterator().next();
      rangeRule.put("0", 100 - value);
      ranges.put("0", new Range(value, 100));
      availableVersions.add("0");
    }

    val bound = trafficRules.get(appName).values().stream().mapToInt(it -> it).sum();
    val value = RANDOM.nextInt(bound);
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

    requestContext.set(CURRENT_REQUEST_CONTEXT_VERSION, version);
    return version;
  }

  private boolean isHeaderVersionRequest() {
    val requestContext = RequestContext.getCurrentContext();
    val request = requestContext.getRequest();
    val targetServiceVersion = metadataBalancingProperties.getHeaderName();
    return request.getHeader(targetServiceVersion) != null;
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

    return ranges.size() == 1 && hasOldVersions;
  }
}
