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

import de.codecentric.boot.admin.config.AdminServerCoreConfiguration;
import de.codecentric.boot.admin.config.AdminServerProperties;
import de.codecentric.boot.admin.config.AdminServerWebConfiguration;
import de.codecentric.boot.admin.config.DiscoveryClientConfiguration;
import de.codecentric.boot.admin.config.NotifierConfiguration;
import de.codecentric.boot.admin.config.RevereseZuulProxyConfiguration;
import de.codecentric.boot.admin.web.servlet.resource.ConcatenatingResourceResolver;
import de.codecentric.boot.admin.web.servlet.resource.PreferMinifiedFilteringResourceResolver;
import de.codecentric.boot.admin.web.servlet.resource.ResourcePatternResolvingResourceResolver;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

/**
 * @author Artem Labazin <xxlabaza@gmail.com>
 * @since 05.04.2017
 */
@Configuration
@Import({
  NotifierConfiguration.class,
  DiscoveryClientConfiguration.class,
  RevereseZuulProxyConfiguration.class,
  AdminServerCoreConfiguration.class,
  DiscoveryClientConfiguration.class,
  RevereseZuulProxyConfiguration.class
})
class AdminConfiguration extends AdminServerWebConfiguration {

  private final ResourcePatternResolver resourcePatternResolver;

  private final AdminServerProperties adminServerProperties;

  AdminConfiguration(ApplicationEventPublisher publisher,
                     ServerProperties server,
                     ResourcePatternResolver resourcePatternResolver,
                     AdminServerProperties adminServerProperties
  ) {
    super(publisher, server, resourcePatternResolver, adminServerProperties);
    this.resourcePatternResolver = resourcePatternResolver;
    this.adminServerProperties = adminServerProperties;
  }

  @Override
  public void addResourceHandlers (ResourceHandlerRegistry registry) {
    registry.addResourceHandler(adminServerProperties.getContextPath() + "/**")
        .addResourceLocations(
            "classpath:/META-INF/override/",
            "classpath:/META-INF/spring-boot-admin-server-ui/"
        )
        .resourceChain(true)
        .addResolver(new PreferMinifiedFilteringResourceResolver(".min"));

    registry.addResourceHandler(adminServerProperties.getContextPath() + "/all-modules.css")
        .resourceChain(true)
        .addResolver(new ResourcePatternResolvingResourceResolver(resourcePatternResolver,
                                                                  "classpath*:/META-INF/spring-boot-admin-server-ui/*/module.css"))
        .addResolver(new ConcatenatingResourceResolver("\n".getBytes()));

    registry.addResourceHandler(adminServerProperties.getContextPath() + "/all-modules.js")
        .resourceChain(true)
        .addResolver(new ResourcePatternResolvingResourceResolver(resourcePatternResolver,
                                                                  "classpath*:/META-INF/spring-boot-admin-server-ui/*/module.js"))
        .addResolver(new PreferMinifiedFilteringResourceResolver(".min"))
        .addResolver(new ConcatenatingResourceResolver(";\n".getBytes()));
  }
}
