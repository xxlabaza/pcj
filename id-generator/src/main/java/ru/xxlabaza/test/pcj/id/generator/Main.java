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
package ru.xxlabaza.test.pcj.id.generator;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Artem Labazin <xxlabaza@gmail.com>
 * @since 29.03.2017
 */
@Slf4j
@RestController
@EnableEurekaClient
@SpringBootApplication
public class Main {

  public static void main (String[] args) {
    SpringApplication.run(Main.class, args);
  }

  @Value("${eureka.instance.metadataMap.version:0}")
  private String version;

  @RequestMapping("/")
  public String generateId() {
    log.error("Version: " + version);
    return UUID.randomUUID().toString();
  }
}
