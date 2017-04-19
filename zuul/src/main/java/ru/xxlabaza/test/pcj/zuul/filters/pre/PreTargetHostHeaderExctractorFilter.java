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

import com.netflix.zuul.context.RequestContext;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.xxlabaza.test.pcj.zuul.AppProperties;
import ru.xxlabaza.test.pcj.zuul.filters.AbstractZuulFilter;

/**
 * @author Artem Labazin <xxlabaza@gmail.com>
 * @since 02.03.2017
 */
@Component
public class PreTargetHostHeaderExctractorFilter extends AbstractZuulFilter {

  public static final String TARGET_HOST_KEY = "targetHost";

  @Autowired
  private AppProperties appProperties;

  PreTargetHostHeaderExctractorFilter () {
    super(PRE_ROUTING_HANDLING, 501);
  }

  @Override
  public boolean shouldFilter () {
    val targetHostHeaderName = appProperties.getZuul().getTargetHostHeaderName();
    return RequestContext.getCurrentContext()
        .getRequest()
        .getHeader(targetHostHeaderName) != null;
  }

  @Override
  protected void execute () {
    val currentContext = RequestContext.getCurrentContext();
    val targetHostHeaderName = appProperties.getZuul().getTargetHostHeaderName();
    val value = currentContext.getRequest().getHeader(targetHostHeaderName);
    currentContext.set(TARGET_HOST_KEY, value);
  }
}
