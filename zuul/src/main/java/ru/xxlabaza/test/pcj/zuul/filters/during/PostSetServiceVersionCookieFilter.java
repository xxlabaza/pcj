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
import static ru.xxlabaza.test.pcj.zuul.ribbon.predicate.MetadataVersionPredicate.CURRENT_REQUEST_CONTEXT_VERSION;

import com.netflix.zuul.context.RequestContext;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.xxlabaza.test.pcj.zuul.filters.AbstractZuulFilter;
import ru.xxlabaza.test.pcj.zuul.ribbon.MetadataBalancingProperties;

/**
 * @author Artem Labazin <xxlabaza@gmail.com>
 * @since 24.03.2017
 */
@Component
class PostSetServiceVersionCookieFilter extends AbstractZuulFilter {

  @Autowired
  private MetadataBalancingProperties metadataBalancingProperties;

  PostSetServiceVersionCookieFilter() {
    super(DURING_ROUTING_HANDLING, 2002);
  }

  @Override
  public boolean shouldFilter() {
    val requestContext = RequestContext.getCurrentContext();
    return requestContext.containsKey(CURRENT_REQUEST_CONTEXT_VERSION);
  }

  @Override
  protected void execute() {
    val requestContext = RequestContext.getCurrentContext();

    val cookie = new StringBuilder()
        .append(metadataBalancingProperties.getResponseCookieName())
        .append('=')
        .append(requestContext.get(CURRENT_REQUEST_CONTEXT_VERSION))
        .toString();

    requestContext.addZuulResponseHeader("Set-Cookie", cookie);
  }
}
