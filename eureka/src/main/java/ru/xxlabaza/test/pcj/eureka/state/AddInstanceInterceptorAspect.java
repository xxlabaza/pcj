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

import com.netflix.appinfo.InstanceInfo;
import com.netflix.eureka.EurekaServerContextHolder;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author Artem Labazin <xxlabaza@gmail.com>
 * @since 27.06.2016
 */
@Slf4j
@Aspect
@Component
class AddInstanceInterceptorAspect {

  @Value("${app.colorPropertyName:container.color}")
  private String colorPropertyName;

  @Autowired
  private StateProperties stateProperties;

  @Before(
      value = "execution(public " + // method access level
              "* " + // method return type
              "com.netflix.eureka.registry.PeerAwareInstanceRegistry.register" + // method name
              "(com.netflix.appinfo.InstanceInfo, ..)) && args(info, ..)", // method arguments
      argNames = "info"
  )
  public void beforeRegistration (InstanceInfo info) {
    // Only for correct log message, what some instance is UP or OUT_OF_SERVICE,
    // because default registration method ignores instanceInfo.getStatus() value
    if (shouldBeOutOfService(info)) {
      info.setStatus(OUT_OF_SERVICE);
    }
  }

  @AfterReturning(
      value = "execution(public " + // method access level
              "* " + // method return type
              "com.netflix.eureka.registry.PeerAwareInstanceRegistry.register" + // method name
              "(com.netflix.appinfo.InstanceInfo, ..)) && args(info, ..)", // method arguments
      argNames = "info"
  )
  public void afterSuccessfulRegistration (InstanceInfo info) {
    if (!shouldBeOutOfService(info)) {
      return;
    }

    val registry = EurekaServerContextHolder.getInstance().getServerContext().getRegistry();
    val lastDirtyTimestamp = String.valueOf(System.currentTimeMillis());
    registry.statusUpdate(info.getAppName(), info.getId(), OUT_OF_SERVICE, lastDirtyTimestamp, true);
  }

  private boolean shouldBeOutOfService (InstanceInfo info) {
    if (info == null) {
      return false;
    }

    val name = info.getAppName();
    if (name == null) {
      return false;
    }

    val activeColor = stateProperties.getActiveColors().get(name);
    if (activeColor == null) {
      return false;
    }

    val instanceMetadataColor = Optional.ofNullable(info.getMetadata())
        .map(it -> it.getOrDefault(colorPropertyName, null))
        .orElse(null);
    if (instanceMetadataColor == null) {
      return false;
    }

    if ((instanceMetadataColor.equalsIgnoreCase("green") && activeColor.isGreen()) ||
         (instanceMetadataColor.equalsIgnoreCase("blue") && activeColor.isBlue())) {
      // status is UP, all services already have this status after registration
      return false;
    }

    return true;
  }
}
