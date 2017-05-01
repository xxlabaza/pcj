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
import static ru.xxlabaza.test.pcj.zuul.filters.FiltersOrder.POST_SET_YODA_COOKIE_ORDER;
import static ru.xxlabaza.test.pcj.zuul.filters.pre.PreYodaCookieFilter.YODA_NEED_SET_COOKIE_KEY;
import static ru.xxlabaza.test.pcj.zuul.filters.pre.PreYodaCookieFilter.YODA_SID_KEY;

import com.netflix.zuul.context.RequestContext;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.xxlabaza.test.pcj.zuul.AppProperties;
import ru.xxlabaza.test.pcj.zuul.filters.AbstractZuulFilter;

/**
 * @author Artem Labazin <xxlabaza@gmail.com>
 * @since 03.03.2017
 */
@Component
class PostSetYodaCookieFilter extends AbstractZuulFilter {

  @Autowired
  private AppProperties appProperties;

  PostSetYodaCookieFilter() {
    super(DURING_ROUTING_HANDLING, POST_SET_YODA_COOKIE_ORDER);
  }

  @Override
  public boolean shouldFilter() {
    val requestContext = RequestContext.getCurrentContext();
    return requestContext.containsKey(YODA_NEED_SET_COOKIE_KEY);
  }

  @Override
  protected void execute() {
    val requestContext = RequestContext.getCurrentContext();

    val cookie = new StringBuilder()
        .append(appProperties.getZuul().getYoda().getCookieName())
        .append('=')
        .append(requestContext.get(YODA_SID_KEY))
        .toString();

    requestContext.addZuulResponseHeader("Set-Cookie", cookie);
  }
}
