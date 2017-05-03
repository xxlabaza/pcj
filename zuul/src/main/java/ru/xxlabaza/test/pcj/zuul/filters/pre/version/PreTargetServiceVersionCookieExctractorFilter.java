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

package ru.xxlabaza.test.pcj.zuul.filters.pre.version;

import static ru.xxlabaza.test.pcj.zuul.filters.AbstractZuulFilter.ZuulFilterType.PRE_ROUTING_HANDLING;
import static ru.xxlabaza.test.pcj.zuul.filters.FiltersOrder.PRE_TARGET_SERVICE_VERSION_COOKIE_EXCTRACTOR_ORDER;
import static ru.xxlabaza.test.pcj.zuul.filters.pre.version.AbstractTargetServiceVersionExctractorFilter.TARGET_SERVICE_VERSION_KEY;

import com.netflix.zuul.context.RequestContext;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;
import ru.xxlabaza.test.pcj.zuul.ribbon.MetadataBalancingProperties;

/**
 * @author Artem Labazin <xxlabaza@gmail.com>
 * @since 29.04.2017
 */
@Component
class PreTargetServiceVersionCookieExctractorFilter extends AbstractTargetServiceVersionExctractorFilter {

  @Autowired
  private MetadataBalancingProperties metadataBalancingProperties;

  PreTargetServiceVersionCookieExctractorFilter() {
    super(PRE_ROUTING_HANDLING, PRE_TARGET_SERVICE_VERSION_COOKIE_EXCTRACTOR_ORDER);
  }

  @Override
  public boolean shouldFilter() {
    val parentResult = super.shouldFilter();
    if (!parentResult) {
      return false;
    }
    val request = RequestContext.getCurrentContext().getRequest();
    val requestCookieName = metadataBalancingProperties.getRequestCookieName();
    return WebUtils.getCookie(request, requestCookieName) != null;
  }

  @Override
  protected void execute() {
    val requestContext = RequestContext.getCurrentContext();
    val request = requestContext.getRequest();
    val requestCookieName = metadataBalancingProperties.getRequestCookieName();
    val cookieValue = WebUtils.getCookie(request, requestCookieName).getValue();
    requestContext.set(TARGET_SERVICE_VERSION_KEY, cookieValue);
  }
}