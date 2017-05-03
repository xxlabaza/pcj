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
package ru.xxlabaza.test.pcj.zuul.filters.during;

import static ru.xxlabaza.test.pcj.zuul.filters.AbstractZuulFilter.ZuulFilterType.DURING_ROUTING_HANDLING;
import static ru.xxlabaza.test.pcj.zuul.filters.FiltersOrder.POST_ATTACH_RESPONSE_PRODUCER_HOST_PORT_ORDER;
import static ru.xxlabaza.test.pcj.zuul.ribbon.predicate.MetadataVersionPredicate.CURRENT_REQUEST_CONTEXT_VERSION;

import com.netflix.client.http.HttpResponse;
import com.netflix.zuul.context.RequestContext;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient.EurekaServiceInstance;
import org.springframework.stereotype.Component;
import ru.xxlabaza.test.pcj.zuul.AppProperties;
import ru.xxlabaza.test.pcj.zuul.filters.AbstractZuulFilter;

import java.util.Objects;

/**
 * @author Artem Labazin <xxlabaza@gmail.com>
 * @since 24.06.2016
 */
@Component
class PostAttachResponseProducerHostPortFilter extends AbstractZuulFilter {

  @Autowired
  private DiscoveryClient discoveryClient;

  @Autowired
  private AppProperties appProperties;

  @Autowired
  PostAttachResponseProducerHostPortFilter (AppProperties appProperties) {
    super(DURING_ROUTING_HANDLING, POST_ATTACH_RESPONSE_PRODUCER_HOST_PORT_ORDER);
  }

  @Override
  public boolean shouldFilter () {
    val requestContext = RequestContext.getCurrentContext();
    return requestContext.get("ribbonResponse") != null;
  }

  @Override
  protected void execute () {
    val requestContext = RequestContext.getCurrentContext();
    val response = (HttpResponse) requestContext.get("ribbonResponse");
    val headerName = appProperties.getZuul().getExecutorHostPortHeaderName();

    val headerValue = new StringBuilder()
        .append(response.getRequestedURI())
        .append(" (v ");
    if (requestContext.containsKey(CURRENT_REQUEST_CONTEXT_VERSION)) {
      headerValue.append(requestContext.get(CURRENT_REQUEST_CONTEXT_VERSION));
    } else {
      val serviceId = requestContext.getOrDefault("serviceId", "").toString();
      val responseHost = response.getRequestedURI().getHost();
      val version = discoveryClient.getInstances(serviceId)
          .stream()
          .filter(it -> it instanceof EurekaServiceInstance)
          .map(it -> ((EurekaServiceInstance) it).getInstanceInfo())
          .filter(it -> it.getHostName().equals(responseHost))
          .map(it -> it.getMetadata())
          .filter(Objects::nonNull)
          .map(it -> it.get("version"))
          .filter(Objects::nonNull)
          .map(Object::toString)
          .findFirst()
          .orElse("0");

      headerValue.append(version);
    }
    headerValue.append(')');

    requestContext.addZuulResponseHeader(headerName, headerValue.toString());
  }
}
