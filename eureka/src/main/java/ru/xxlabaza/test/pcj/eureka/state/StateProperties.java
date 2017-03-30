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
package ru.xxlabaza.test.pcj.eureka.state;

import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Artem Labazin <xxlabaza@gmail.com>
 * @since 27.06.2016
 */
@Data
@ConfigurationProperties("app.eureka")
public class StateProperties {

  private Map<String, ActiveColor> activeColors;

  @PostConstruct
  public void postConstruct () {
    if (activeColors == null) {
      activeColors = Collections.emptyMap();
      return;
    }

    activeColors = activeColors.entrySet().stream()
        .collect(toMap(it -> it.getKey().toUpperCase(), Entry::getValue));
  }

  @Data
  public static class ActiveColor {

    private boolean blue = true;

    private boolean green = true;
  }
}
