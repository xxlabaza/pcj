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

import static ru.xxlabaza.test.pcj.zuul.filters.pre.PreTargetColorHeaderExtractorFilter.TARGET_COLOR_KEY;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.loadbalancer.Server;
import com.netflix.zuul.context.RequestContext;
import java.util.Random;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.xxlabaza.test.pcj.zuul.AppProperties;

/**
 * @author Artem Labazin <xxlabaza@gmail.com>
 * @since 02.03.2017
 */
@Component
class ColorBasedTrafficRulePredicate extends AbstractPredicate {

  private final static String CURRENT_REQUEST_CONTEXT_COLOR_KEY;

  private final static String CONTAINER_COLOR_KEY_NAME;

  private final static Random RANDOM;

  static {
    CURRENT_REQUEST_CONTEXT_COLOR_KEY = "current-request-context-color";
    CONTAINER_COLOR_KEY_NAME = "container.color";
    RANDOM = new Random(System.currentTimeMillis());
  }

  @Autowired
  private AppProperties appProperties;

  ColorBasedTrafficRulePredicate() {
    super(700);
  }

  @Override
  protected boolean shouldApply(RequestContext requestContext) {
    return requestContext.containsKey(TARGET_COLOR_KEY);
  }

  @Override
  protected boolean apply(Server server, InstanceInfo instanceInfo) {
    val metadata = instanceInfo.getMetadata();
    if (!metadata.containsKey(CONTAINER_COLOR_KEY_NAME)) {
      return true;
    }

    val appName = instanceInfo.getAppName();
    val appColor = metadata.get(CONTAINER_COLOR_KEY_NAME);
    val requestContextColor = getRequestContextColor(appName);
    return requestContextColor != null
           ? appColor.equalsIgnoreCase(requestContextColor)
           : true;
  }

  private String getRequestContextColor (String appName) {
    val requestContext = RequestContext.getCurrentContext();
    if (requestContext.contains(CURRENT_REQUEST_CONTEXT_COLOR_KEY)) {
      return requestContext.get(CURRENT_REQUEST_CONTEXT_COLOR_KEY).toString();
    }

    val targetColor = requestContext.getOrDefault(TARGET_COLOR_KEY, "").toString();
    if (!targetColor.isEmpty()) {
      requestContext.set(CURRENT_REQUEST_CONTEXT_COLOR_KEY, targetColor);
      return targetColor;
    }

    val trafficRules = appProperties.getZuul().getTrafficRules();
    if (trafficRules == null || !trafficRules.containsKey(appName)) {
      return null;
    }

    val trafficRule = trafficRules.get(appName);
    val nextInt = RANDOM.nextInt(trafficRule.getBlue() + trafficRule.getGreen());
    val color = nextInt <= trafficRule.getBlue()
                ? "blue"
                : "green";

    requestContext.set(CURRENT_REQUEST_CONTEXT_COLOR_KEY, color);
    return color;
  }
}
