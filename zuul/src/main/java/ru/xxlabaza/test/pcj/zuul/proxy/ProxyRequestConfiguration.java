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
package ru.xxlabaza.test.pcj.zuul.proxy;

import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.trace.TraceRepository;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.netflix.zuul.ZuulProxyConfiguration;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandFactory;
import org.springframework.cloud.netflix.zuul.filters.route.apache.HttpClientRibbonCommandFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import ru.xxlabaza.test.pcj.zuul.AppProperties;

import java.util.Set;

@Configuration
class ProxyRequestConfiguration {

  @Autowired
  private ZuulProperties zuulProperties;

  @Autowired(required = false)
  private TraceRepository traces;

  @Bean
  public RibbonCommandFactory ribbonCommandFactory(SpringClientFactory clientFactory) {
    return new HttpClientRibbonCommandFactory(clientFactory);
  }

  @Bean
  public ServiceRouteMapper serviceRouteMapper() {
    return new ServiceRouteMapper();
  }

  @Bean
  @Primary
  public ProxyRequestHelper customProxyRequestHelper(AppProperties appProperties) {
    ProxyRequestHelper helper = new ProxyRequestHelper();
    if (traces != null) {
        helper.setTraces(traces);
    }

    Set<String> ignoredHeaders = zuulProperties.getIgnoredHeaders();
    ignoredHeaders.removeAll(appProperties.getZuul().getHeadersAllowToPassThrough());

    helper.setIgnoredHeaders(ignoredHeaders);
    helper.setTraceRequestBody(zuulProperties.isTraceRequestBody());
    return helper;
  }

  @Configuration
  @AutoConfigureBefore(ZuulProxyConfiguration.class)
  public static class PreZuulConfiguration {

    @Bean(name = "zuul.CONFIGURATION_PROPERTIES")
    @RefreshScope
    @ConfigurationProperties("zuul")
    public ZuulProperties zuulProperties () {
      return new ZuulProperties() {

        @Override
        @PostConstruct
        public void init() {
          getRoutes().remove("service_health");
          super.init();
        }
      };
    }
  }
}
