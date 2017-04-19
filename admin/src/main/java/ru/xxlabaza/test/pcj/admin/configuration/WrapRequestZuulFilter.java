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

package ru.xxlabaza.test.pcj.admin.configuration;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import lombok.val;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Workaround for issue:
 * https://github.com/spring-cloud/spring-cloud-netflix/issues/1663
 *
 * @author Artem Labazin <xxlabaza@gmail.com>
 * @since 02.04.2017
 */
@Component
class WrapRequestZuulFilter extends ZuulFilter {

  @Override
  public String filterType () {
    return "route";
  }

  @Override
  public int filterOrder () {
    return 80;
  }

  @Override
  public boolean shouldFilter () {
    return RequestContext.getCurrentContext().getRouteHost() != null &&
           RequestContext.getCurrentContext().sendZuulResponse();
  }

  @Override
  public Object run () {
    val context = RequestContext.getCurrentContext();
    val request = context.getRequest();
    context.setRequest(new HttpServletRequestWrapper(request) {

      @Override
      public String getContentType () {
        val value = super.getContentType();
        return value != null && value.startsWith(APPLICATION_JSON_VALUE)
               ? APPLICATION_JSON_VALUE
               : value;
      }

    });
    return null;
  }
}
