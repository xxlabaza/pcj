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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * @author Artem Labazin <xxlabaza@gmail.com>
 * @since 28.04.2017
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "zuul")
public class FilterProperties {

  private Map<String, Filters> routes = new HashMap<>();

  @Data
  @NoArgsConstructor
  public static class Filters {

    private Set<String> disableFilters = new HashSet<>();

    public Filters(String text) {
    }
  }
}
