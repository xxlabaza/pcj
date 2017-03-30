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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import ru.xxlabaza.test.pcj.zuul.filters.AbstractZuulFilter;

/**
 * This is a Zuul Filter that will get the version from the URL and
 * set it in the Accept Header. The version will be removed the URL
 * and an optional path prefix will be added to the beginning of the URL
 *
 * @author Devron Thompson
 */
@Slf4j
//@Component
class PrePendPathFilter extends AbstractZuulFilter {

  private static final String REQUEST_URI_KEY;

  static {
    REQUEST_URI_KEY = "requestURI";
  }

  @Value("${zuul.addPrefix:}")
  private String addPrefix;

  @Value("#{'${zuul.skipAddPrefix:}'.split(',')}")
  private List<String> skipAddPrefix;

  PrePendPathFilter() {
    super(PRE_ROUTING_HANDLING, 1110);
  }

  @Override
  public boolean shouldFilter() {
    return true;
  }

  @Override
  protected void execute() {
    final RequestContext requestContext = RequestContext.getCurrentContext();
    Path path = Paths.get(requestContext.getRequest().getRequestURI());

    String url = path.toString();
    String firstPath = path.getName(0).toString();

    //if first path not equal to addPrefix then prepend addPrefix
    if ((!skipAddPrefix.isEmpty() && !skipAddPrefix.contains("/" + firstPath)) && !firstPath.equals(addPrefix.substring(1, addPrefix.length()))) {
      url = addPrefix + url;
    }

    //Override the request URI if they are different
    if (!url.equals(requestContext.getRequest().getRequestURI())) {
      requestContext.put(REQUEST_URI_KEY, url);
    }

    log.debug("VersionFilter Endpoint URL: " + url);
  }
}
