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
package ru.xxlabaza.test.pcj.zuul.apps.health;

import static com.netflix.appinfo.InstanceInfo.InstanceStatus.UP;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient.EurekaServiceInstance;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.xxlabaza.test.pcj.zuul.apps.ServiceNotFoundException;

/**
 * @author Artem Labazin <xxlabaza@gmail.com>
 * @since 05.10.2016
 */
@RestController
class ServiceHealthController {

  @Autowired
  private DiscoveryClient discoveryClient;

  @Autowired
  private ZuulProperties zuulProperties;

  @RequestMapping("/apps/{routeId}/admin/health")
  public ResponseEntity<Void> getServiceHealth (@PathVariable("routeId") String routeId) {
    HttpStatus status = Optional.ofNullable(zuulProperties.getRoutes().get(routeId))
        .map(ZuulProperties.ZuulRoute::getServiceId)
        .map(serviceId -> discoveryClient.getInstances(serviceId).stream()
            .map(it -> (EurekaServiceInstance) it)
            .map(it -> it.getInstanceInfo().getStatus())
            .filter(it -> it.equals(UP))
            .findAny()
            .map(it -> OK)
            .orElse(BAD_REQUEST)
        )
        .orElseThrow(ServiceNotFoundException::new);
    return ResponseEntity.status(status).body(null);
  }
}
