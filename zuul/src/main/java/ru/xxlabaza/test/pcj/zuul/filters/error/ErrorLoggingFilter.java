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
package ru.xxlabaza.test.pcj.zuul.filters.error;

import static com.netflix.appinfo.InstanceInfo.InstanceStatus.UP;
import static ru.xxlabaza.test.pcj.zuul.filters.AbstractZuulFilter.ZuulFilterType.ERROR_HANDLING;
import static ru.xxlabaza.test.pcj.zuul.filters.FiltersOrder.ERROR_LOGGING_ORDER;

import com.netflix.zuul.context.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient.EurekaServiceInstance;
import org.springframework.stereotype.Component;
import ru.xxlabaza.test.pcj.zuul.AppProperties;
import ru.xxlabaza.test.pcj.zuul.filters.AbstractZuulFilter;
import ru.xxlabaza.test.pcj.zuul.ribbon.MetadataBalancingProperties;

import java.util.Map;
import javax.servlet.http.Cookie;
import lombok.val;

/**
 * @author Artem Labazin <xxlabaza@gmail.com>
 * @since 03.03.2017
 */
@Slf4j
@Component
class ErrorLoggingFilter extends AbstractZuulFilter {

  @Autowired
  private DiscoveryClient discoveryClient;

  @Autowired
  private AppProperties appProperties;

  @Autowired
  private MetadataBalancingProperties metadataBalancingProperties;

  ErrorLoggingFilter() {
    super(ERROR_HANDLING, ERROR_LOGGING_ORDER);
  }

  @Override
  public boolean shouldFilter() {
    val requestContext = RequestContext.getCurrentContext();
    val exception = requestContext.get("error.exception");
    return exception != null && exception instanceof Exception &&
           "Forwarding error".equals(((Throwable) exception).getMessage());
  }

  @Override
  protected void execute() {
    log.error("Forwarding error with request:\n\n{}\n{}\n{}\n",
              getRequestInfo(), getServicesInfo(), getMetadataAndColorInfo());
  }

  private String getRequestInfo() {
    val requestContext = RequestContext.getCurrentContext();
    val request = requestContext.getRequest();

    val sb = new StringBuilder();
    sb.append(request.getRequestURI()).append('\n');

    sb.append("Headers:\n");
    val headerNames = request.getHeaderNames();
    while (headerNames.hasMoreElements()) {
      val headerName = headerNames.nextElement();
      val headers = request.getHeaders(headerName);
      while (headers.hasMoreElements()) {
        val headerValue = headers.nextElement();
        sb.append("  ").append(headerName).append(':').append(headerValue).append('\n');
      }
    }

    sb.append("Cookies:\n");
    val cookies = request.getCookies();
    if (cookies == null) {
      sb.append("  No cookie\n");
    } else {
      for (Cookie cookie : cookies) {
        sb.append("  ").append(cookie.getName()).append('=').append(cookie.getValue()).append('\n');
      }
    }

    return sb.toString();
  }

  private String getServicesInfo() {
    val requestContext = RequestContext.getCurrentContext();
    val serviceId = requestContext.getOrDefault("serviceId", "").toString();

    val sb = new StringBuilder("Service ID: ").append(serviceId).append('\n');

    discoveryClient.getInstances(serviceId)
        .stream()
        .filter(it -> it instanceof EurekaServiceInstance)
        .map(it -> ((EurekaServiceInstance) it).getInstanceInfo())
        .filter(it -> UP.equals(it.getStatus()))
        .forEach(it -> {
          sb.append(" - ").append(it.getHostName());

          Map<String, String> metadata = it.getMetadata();
          sb.append(" | ").append(metadata.getOrDefault("version", "0"));
          sb.append(" | ").append(metadata.getOrDefault("container.color", "no color"));
          sb.append('\n');
        });

    if (sb.indexOf(" | ") < 1) {
      sb.append("  No available services at Eureka");
    }
    return sb.toString();
  }

  private String getMetadataAndColorInfo() {
    val requestContext = RequestContext.getCurrentContext();
    val serviceId = requestContext.getOrDefault("serviceId", "").toString();

    val sb = new StringBuilder();

    sb.append("Metadata version info:\n");
    val rules = metadataBalancingProperties.getRules().get(serviceId);
    if (rules == null) {
      sb.append("  No metadata version info\n");
    } else {
      rules.entrySet().stream().forEach(it -> sb.append("  ")
          .append(it.getKey()).append(": ").append(it.getValue()).append('\n'));
    }
    sb.append('\n');

    sb.append("Color balancing info:\n");
    val trafficRules = appProperties.getZuul().getTrafficRules();
    if (trafficRules == null) {
      sb.append("  No color balancing info at all\n");
    } else {
      val colorTrafficRule = trafficRules.get(serviceId);
      if (colorTrafficRule == null) {
        sb.append("  No color balancing info for: ").append(serviceId).append('\n');
      } else {
        sb.append("  green: ").append(colorTrafficRule.getGreen()).append('\n');
        sb.append("  blue:  ").append(colorTrafficRule.getBlue()).append('\n');
      }
    }

    return sb.toString();
  }
}
