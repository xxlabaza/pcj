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
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import javax.servlet.http.Cookie;
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
public class PreYodaCookieFilter extends AbstractZuulFilter {

  public static final String YODA_SID_KEY = "yoda-sid";
  public static final String YODA_NEED_SET_COOKIE_KEY = "need-to-set-yoda-cookie";

  @Autowired
  private AppProperties appProperties;

  PreYodaCookieFilter () {
    super(PRE_ROUTING_HANDLING, 503);
  }

  @Override
  public boolean shouldFilter () {
    val requestContext = RequestContext.getCurrentContext();
    val serviceId = requestContext.getOrDefault("serviceId", "").toString();
    val appPrefix = appProperties.getZuul().getYoda().getAppPrefix();
    return serviceId.toLowerCase().startsWith(appPrefix);
  }

  @Override
  protected void execute () {
    val cookieName = appProperties.getZuul().getYoda().getCookieName();
    val requestContext = RequestContext.getCurrentContext();
    val cookies = requestContext.getRequest().getCookies();
    val yodaSid = Optional.ofNullable(cookies)
        .flatMap(values -> Stream.of(values)
            .filter(it -> it.getName().equalsIgnoreCase(cookieName))
            .findFirst()
            .map(Cookie::getValue)
        )
        .orElseGet(() -> {
          requestContext.set(YODA_NEED_SET_COOKIE_KEY);
          return UUID.randomUUID().toString();
        });

    requestContext.set(YODA_SID_KEY, yodaSid);
  }
}
