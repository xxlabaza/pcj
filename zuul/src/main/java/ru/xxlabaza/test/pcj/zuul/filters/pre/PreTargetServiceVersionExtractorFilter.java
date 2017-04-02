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
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.xxlabaza.test.pcj.zuul.filters.AbstractZuulFilter;
import ru.xxlabaza.test.pcj.zuul.ribbon.MetadataBalancingProperties;

import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.servlet.http.Cookie;

/**
 * @author Artem Labazin <xxlabaza@gmail.com>
 * @since 22.03.2017
 */
@Component
public class PreTargetServiceVersionExtractorFilter extends AbstractZuulFilter {

  public static final String TARGET_SERVICE_VERSION_KEY = "targetServiceVersion";
  private static final Pattern VERSION_URI_PATTERN = Pattern.compile("^\\S*\\/v([\\d\\.]+)\\/\\S+$");

  @Autowired
  private MetadataBalancingProperties metadataBalancingProperties;

  public PreTargetServiceVersionExtractorFilter() {
    super(PRE_ROUTING_HANDLING, 504);
  }

  @Override
  public boolean shouldFilter() {
    return true;
  }

  @Override
  protected void execute() {
    val requestContext = RequestContext.getCurrentContext();

    val headerValue = getServiceVersionHeaderValue();
    if (headerValue.isPresent()) {
      requestContext.set(TARGET_SERVICE_VERSION_KEY, headerValue.get());
      return;
    }

    val cookieValue = getServiceVersionCookieValue();
    if (cookieValue.isPresent()) {
      requestContext.set(TARGET_SERVICE_VERSION_KEY, cookieValue.get());
      return;
    }

    val uriValue = getServiceVersionUriValue();
    if (uriValue.isPresent()) {
      requestContext.set(TARGET_SERVICE_VERSION_KEY, uriValue.get());
    }
  }

  private Optional<String> getServiceVersionHeaderValue() {
    val request = RequestContext.getCurrentContext().getRequest();
    val targetServiceVersion = metadataBalancingProperties.getHeaderName();
    val headerValue = request.getHeader(targetServiceVersion);
    return Optional.ofNullable(headerValue);
  }

  private Optional<String> getServiceVersionUriValue() {
    val request = RequestContext.getCurrentContext().getRequest();
    val uri = request.getRequestURI();
    val matcher = VERSION_URI_PATTERN.matcher(uri);
    return matcher.find()
           ? Optional.of(matcher.group(1))
           : Optional.empty();
  }

  private Optional<String> getServiceVersionCookieValue() {
    val request = RequestContext.getCurrentContext().getRequest();
    val requestCookieName = metadataBalancingProperties.getRequestCookieName();
    return Optional.ofNullable(request.getCookies())
        .flatMap(cookies -> Stream.of(cookies)
            .filter(it -> requestCookieName.equalsIgnoreCase(it.getName()))
            .findAny()
            .map(Cookie::getValue)
        );
  }
}
