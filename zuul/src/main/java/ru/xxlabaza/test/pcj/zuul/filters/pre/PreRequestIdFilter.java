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

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import java.util.Objects;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

@Slf4j
//@Component
public class PreRequestIdFilter extends ZuulFilter {

  @Override
  public boolean shouldFilter() {
    return true;
  }

  @Override
  public Object run() {
    execute();
    return Boolean.TRUE;
  }

  @Override
  public String filterType() {
    return "pre";
  }

  @Override
  public int filterOrder() {
    return 1500;
  }

  protected void execute() {
    RequestContext ctx = RequestContext.getCurrentContext();

    HttpServletRequest request = ctx.getRequest();
    String requestIdHeaderValue = request.getHeader("request.id");

    log.debug("passed to zuul {} request to {} with request.id header: {} ", request.getMethod(),
        request.getRequestURL(),  requestIdHeaderValue);

    if (Objects.isNull(request.getHeader("request.id")) || StringUtils.isEmpty(request.getHeader("request.id"))) {
      requestIdHeaderValue = UUID.randomUUID().toString();
      ctx.addZuulRequestHeader("request.id", requestIdHeaderValue);
    }

    log.debug("outcome from zuul  {} request to {} with request.id header: {} ", request.getMethod(),
        request.getRequestURL(),  requestIdHeaderValue);

    MDC.put("request.id", requestIdHeaderValue);
  }
}
