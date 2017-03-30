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
package ru.xxlabaza.test.pcj.zuul;

import static java.util.stream.Collectors.toMap;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import ru.xxlabaza.test.pcj.zuul.AppProperties.Zuul.ColorTrafficRule;

/**
 * @author Artem Labazin <xxlabaza@gmail.com>
 * @since 24.06.2016
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "app")
public class AppProperties {

  private Zuul zuul = new Zuul();

  @PostConstruct
  public void postConstruct() {
    Map<String, Zuul.ColorTrafficRule> trafficRules = zuul.getTrafficRules() == null
        ? Collections.emptyMap()
        : zuul.getTrafficRules().entrySet().stream()
            .map(it -> new SimpleEntry<>(it.getKey().toUpperCase(), it.getValue()))
            .collect(toMap(SimpleEntry::getKey, SimpleEntry::getValue, (newVal, oldVal) -> newVal));

    zuul.setTrafficRules(trafficRules);
  }

  @Data
  public static class Zuul {

    private String targetHostPortHeaderName = "X-Target-Host-Port";
    private String targetHostHeaderName = "X-Target-Host";
    private String targetColorHeaderName = "X-Target-Color";
    private String executorHostPortHeaderName = "X-Executor-Host-Port";
    private Set<String> headersAllowToPassThrough = Collections.singleton("Cache-Control");
    private Map<String, ColorTrafficRule> trafficRules;

    private Yoda yoda = new Yoda();

    @Data
    public static class ColorTrafficRule {

      private int blue;
      private int green;
    }

    @Data
    public static class Yoda {

      private String cookieName = "Yoda-SID";
      private String appPrefix = "yoda";
    }
  }
}