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
package ru.xxlabaza.test.pcj.zuul.filters.pre;

import static ru.xxlabaza.test.pcj.zuul.filters.AbstractZuulFilter.ZuulFilterType.PRE_ROUTING_HANDLING;
import static ru.xxlabaza.test.pcj.zuul.filters.FiltersOrder.PRE_TARGET_HOST_PORT_HEADER_EXCTRACTOR_ORDER;

import com.netflix.zuul.context.RequestContext;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.xxlabaza.test.pcj.zuul.AppProperties;
import ru.xxlabaza.test.pcj.zuul.filters.AbstractZuulFilter;

@Component
public class PreTargetHostPortHeaderExctractorFilter extends AbstractZuulFilter {

  public static final String TARGET_HOST_PORT_KEY = "targetHostPort";

  @Autowired
  private AppProperties appProperties;

  PreTargetHostPortHeaderExctractorFilter () {
    super(PRE_ROUTING_HANDLING, PRE_TARGET_HOST_PORT_HEADER_EXCTRACTOR_ORDER);
  }

  @Override
  public boolean shouldFilter () {
    val targetHostPortHeaderName = appProperties.getZuul().getTargetHostPortHeaderName();
    return RequestContext.getCurrentContext()
        .getRequest()
        .getHeader(targetHostPortHeaderName) != null;
  }

  @Override
  protected void execute () {
    val currentContext = RequestContext.getCurrentContext();
    val targetHostPortHeaderName = appProperties.getZuul().getTargetHostPortHeaderName();
    val value = currentContext.getRequest().getHeader(targetHostPortHeaderName);
    currentContext.set(TARGET_HOST_PORT_KEY, value);
  }
}
