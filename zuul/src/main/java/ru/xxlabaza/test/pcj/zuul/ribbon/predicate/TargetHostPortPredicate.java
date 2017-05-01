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

import static ru.xxlabaza.test.pcj.zuul.filters.pre.PreTargetHostHeaderExctractorFilter.TARGET_HOST_KEY;
import static ru.xxlabaza.test.pcj.zuul.filters.pre.PreTargetHostPortHeaderExctractorFilter.TARGET_HOST_PORT_KEY;
import static ru.xxlabaza.test.pcj.zuul.ribbon.predicate.PredicateOrders.TARGET_HOST_PORT_ORDER;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.loadbalancer.Server;
import com.netflix.zuul.context.RequestContext;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;

/**
 * @author Artem Labazin <xxlabaza@gmail.com>
 * @since 02.03.2017
 */
@Slf4j
@Component
class TargetHostPortPredicate extends AbstractPredicate {

  TargetHostPortPredicate() {
    super(TARGET_HOST_PORT_ORDER);
  }

  @Override
  protected boolean shouldApply (RequestContext requestContext) {
    return requestContext.containsKey(TARGET_HOST_PORT_KEY) ||
           requestContext.containsKey(TARGET_HOST_KEY);
  }

  @Override
  protected boolean apply (Server server, InstanceInfo instanceInfo) {
    val requestContext = RequestContext.getCurrentContext();

    val targetHost = requestContext.getOrDefault(TARGET_HOST_KEY, "").toString();
    if (!targetHost.isEmpty()) {
      log.debug("Server host: {}", server.getHost());
      log.debug("Target host: {}", targetHost);
      return server.getHost().equals(targetHost);
    }

    val targetHostPort = requestContext.getOrDefault(TARGET_HOST_PORT_KEY, "").toString();
    if (!targetHostPort.isEmpty()) {
      log.debug("Server host port: {}", server.getHostPort());
      log.debug("Target host port: {}", targetHostPort);
      return server.getHostPort().equals(targetHostPort);
    }
    return false;
  }
}
