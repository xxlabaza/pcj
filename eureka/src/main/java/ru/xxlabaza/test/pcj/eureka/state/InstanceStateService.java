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
package ru.xxlabaza.test.pcj.eureka.state;

import static com.netflix.appinfo.InstanceInfo.InstanceStatus.OUT_OF_SERVICE;
import static com.netflix.appinfo.InstanceInfo.InstanceStatus.UP;
import static java.util.stream.Collectors.toList;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import com.netflix.discovery.shared.Application;
import com.netflix.eureka.EurekaServerContextHolder;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;

/**
 * @author Artem Labazin <xxlabaza@gmail.com>
 * @since 22.06.2016
 */
@Slf4j
@Service
class InstanceStateService implements ApplicationListener<RefreshScopeRefreshedEvent> {

  @Value("${app.colorPropertyName:container.color}")
  private String colorPropertyName;

  @Autowired
  private StateProperties stateProperties;

  public List<AppInfo> changeStatus (Set<String> names, Color color, InstanceStatus status) {
    val registry = EurekaServerContextHolder.getInstance().getServerContext().getRegistry();
    val lastDirtyTimestamp = String.valueOf(System.currentTimeMillis());
    return names.stream()
        .map(it -> registry.getApplication(it.toUpperCase(), true))
        .filter(Objects::nonNull)
        .map(Application::getInstancesAsIsFromEureka)
        .flatMap(List::stream)
        .filter(it -> color.name().equalsIgnoreCase(it.getMetadata().get(colorPropertyName)))
        .peek(it -> registry.statusUpdate(it.getAppName(),
                                          it.getId(),
                                          status,
                                          lastDirtyTimestamp,
                                          true)
        )
        .map(it -> AppInfo.builder()
            .name(it.getAppName())
            .ip(it.getIPAddr())
            .color(color)
            .status(it.getStatus())
            .build()
        )
        .collect(toList());
  }

  @Override
  public void onApplicationEvent (RefreshScopeRefreshedEvent event) {
    val registry = EurekaServerContextHolder.getInstance().getServerContext().getRegistry();
    val lastDirtyTimestamp = String.valueOf(System.currentTimeMillis());

    stateProperties.getActiveColors().keySet().stream()
        .map(it -> registry.getApplication(it, true))
        .filter(Objects::nonNull)
        .map(Application::getInstancesAsIsFromEureka)
        .flatMap(List::stream)
        .filter(it -> it.getMetadata().containsKey(colorPropertyName))
        .map(it -> new Tuple2<>(it, getNewStatus(it)))
        .filter(it -> it._1.getStatus() != it._2)
        .forEach(it -> registry.statusUpdate(it._1.getAppName(),
                                             it._1.getId(),
                                             it._2,
                                             lastDirtyTimestamp,
                                             true));
  }

  private InstanceStatus getNewStatus (InstanceInfo instanceInfo) {
    val colorName = instanceInfo.getMetadata().get(colorPropertyName);
    val activeColor = stateProperties.getActiveColors().get(instanceInfo.getAppName());
    return (colorName.equalsIgnoreCase("blue") && activeColor.isBlue())
            || (colorName.equalsIgnoreCase("green") && activeColor.isGreen())
            ? UP
            : OUT_OF_SERVICE;
  }

  @Data
  private static class Tuple2<A, B> {

    public final A _1;

    public final B _2;
  }
}
