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
package ru.xxlabaza.test.pcj.zuul.filters;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.springframework.http.HttpStatus;

/**
 * @author Artem Labazin <xxlabaza@gmail.com>
 * @since 24.06.2016
 */
public abstract class AbstractZuulFilter extends ZuulFilter {

  private final ZuulFilterType type;

  private final int order;

  public AbstractZuulFilter(ZuulFilterType type, int order) {
    this.type = type;
    this.order = order;
  }

  @Override
  public final int filterOrder() {
    return order;
  }

  @Override
  public final String filterType() {
    return type.getName();
  }

  @Override
  public String disablePropertyName() {
    return "zuul." + this.getClass().getSimpleName() + ".disable";
  }

  @Override
  public final Object run() {
    execute();
    return Boolean.TRUE;
  }

  protected boolean is2xxSuccessfulResponse() {
    int statusValue = RequestContext.getCurrentContext().getResponse().getStatus();
    HttpStatus status = HttpStatus.valueOf(statusValue);
    return status.is2xxSuccessful();
  }

  protected abstract void execute();

  public enum ZuulFilterType {

    PRE_ROUTING_HANDLING("pre"),
    DURING_ROUTING_HANDLING("route"),
    POST_ROUTING_HANDLING("post"),
    ERROR_HANDLING("error");

    private final String name;

    private ZuulFilterType(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }
}
