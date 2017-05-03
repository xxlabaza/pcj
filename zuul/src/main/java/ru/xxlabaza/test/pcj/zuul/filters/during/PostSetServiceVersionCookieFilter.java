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

import static java.util.Calendar.DATE;
import static java.util.Locale.US;
import static ru.xxlabaza.test.pcj.zuul.filters.AbstractZuulFilter.ZuulFilterType.DURING_ROUTING_HANDLING;
import static ru.xxlabaza.test.pcj.zuul.filters.FiltersOrder.POST_SET_SERVICE_VERSION_COOKIE_ORDER;
import static ru.xxlabaza.test.pcj.zuul.filters.pre.version.AbstractTargetServiceVersionExctractorFilter.TARGET_SERVICE_VERSION_KEY;

import com.netflix.zuul.context.RequestContext;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;
import ru.xxlabaza.test.pcj.zuul.filters.AbstractZuulFilter;
import ru.xxlabaza.test.pcj.zuul.ribbon.MetadataBalancingProperties;

/**
 * @author Artem Labazin <xxlabaza@gmail.com>
 * @since 24.03.2017
 */
@Component
class PostSetServiceVersionCookieFilter extends AbstractZuulFilter {

  private static final SimpleDateFormat COOKIE_DATE_FORMAT;

  static {
    COOKIE_DATE_FORMAT = new SimpleDateFormat("EEE MMM dd yy HH:mm:ss 'GMT'Z", US);
  }

  @Autowired
  private MetadataBalancingProperties metadataBalancingProperties;

  PostSetServiceVersionCookieFilter() {
    super(DURING_ROUTING_HANDLING, POST_SET_SERVICE_VERSION_COOKIE_ORDER);
  }

  @Override
  public boolean shouldFilter() {
    val request = RequestContext.getCurrentContext().getRequest();
    val requestCookieName = metadataBalancingProperties.getRequestCookieName();
    return WebUtils.getCookie(request, requestCookieName) != null;
  }

  @Override
  protected void execute() {
    val requestContext = RequestContext.getCurrentContext();

    val calendar = Calendar.getInstance();
    calendar.setTime(new Date());
    calendar.add(DATE, 1);

    val cookie = new StringBuilder()
        .append(metadataBalancingProperties.getResponseCookieName())
        .append('=')
        .append(requestContext.get(TARGET_SERVICE_VERSION_KEY)).append(';')
        .append("domain=.jcpenney.com;")
        .append("expires=").append(COOKIE_DATE_FORMAT.format(calendar.getTime())).append(';')
        .append("path=/")
        .toString();

    requestContext.addZuulResponseHeader("Set-Cookie", cookie);
  }
}
