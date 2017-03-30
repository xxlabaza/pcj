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
import java.util.regex.Pattern;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.xxlabaza.test.pcj.zuul.filters.AbstractZuulFilter;
import ru.xxlabaza.test.pcj.zuul.ribbon.MetadataBalancingProperties;

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
    val request = RequestContext.getCurrentContext().getRequest();
    val uri = request.getRequestURI();
    val targetServiceVersion = metadataBalancingProperties.getHeaderName();
    return request.getHeader(targetServiceVersion) != null ||
           VERSION_URI_PATTERN.matcher(uri).matches();
  }

  @Override
  protected void execute() {
    val requestContext = RequestContext.getCurrentContext();
    val request = requestContext.getRequest();

    val targetServiceVersion = metadataBalancingProperties.getHeaderName();
    String value = request.getHeader(targetServiceVersion);
    if (value != null) {
      requestContext.set(TARGET_SERVICE_VERSION_KEY, value);
      return;
    }

    val uri = request.getRequestURI();
    val matcher = VERSION_URI_PATTERN.matcher(uri);
    if (matcher.find()) {
      value = matcher.group(1);
    }
    requestContext.set(TARGET_SERVICE_VERSION_KEY, value);
  }
}
