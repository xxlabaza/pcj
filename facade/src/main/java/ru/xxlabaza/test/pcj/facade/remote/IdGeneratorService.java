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
package ru.xxlabaza.test.pcj.facade.remote;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author Artem Labazin <xxlabaza@gmail.com>
 * @since 29.03.2017
 */
@Service
public class IdGeneratorService {

  @Autowired
  private IdGeneratorApi api;

  @HystrixCommand(fallbackMethod = "generateIdFallback")
  public String generateId() {
    return api.generateId();
  }

  public String generateIdFallback() {
    return "";
  }
}
