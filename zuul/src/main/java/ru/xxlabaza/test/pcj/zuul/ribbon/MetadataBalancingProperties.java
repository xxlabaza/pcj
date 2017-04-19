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
package ru.xxlabaza.test.pcj.zuul.ribbon;

import static java.util.stream.Collectors.toMap;

import javax.annotation.PostConstruct;
import lombok.Data;
import lombok.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Artem Labazin <xxlabaza@gmail.com>
 * @since 27.03.2017
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "app.metadata.balancing")
public class MetadataBalancingProperties {

  private String headerName = "X-Service-Version";
  private String requestCookieName = "SVCVER";
  private String responseCookieName = "SVCVER";
  private Map<String, Map<String, Integer>> rules;
  private Map<String, Map<String, Range>> ranges;

  @PostConstruct
  public void postConstruct() {
    if (rules == null) {
      ranges = Collections.emptyMap();
      return;
    }
    ranges = rules.entrySet().stream()
        .collect(toMap(Entry::getKey, it -> {
          Range.currentFrom = 0;
          return it.getValue()
              .entrySet()
              .stream()
              .collect(toMap(Entry::getKey, Range::new));
        }));
  }

  @Value
  public static class Range {

    static int currentFrom;

    int from;
    int to;

    public Range(int from, int to) {
      this.from = from;
      this.to = to;
    }

    Range(Map.Entry<String, Integer> entry) {
      from = currentFrom;
      to = from + entry.getValue();
      currentFrom = to;
    }
  }
}
