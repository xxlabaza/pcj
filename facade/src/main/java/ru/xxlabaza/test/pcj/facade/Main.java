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
package ru.xxlabaza.test.pcj.facade;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.xxlabaza.test.pcj.facade.remote.IdGeneratorService;

import java.util.Arrays;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author Artem Labazin <xxlabaza@gmail.com>
 * @since 29.03.2017
 */
@RestController
@EnableFeignClients
@EnableEurekaClient
@EnableCircuitBreaker
@SpringBootApplication
public class Main {

  public static void main (String[] args) {
    String classpath = System.getProperty("java.class.path").split(":")[0];
    String appFolder = classpath.substring(0, classpath.lastIndexOf("/target"));
    String[] newArgs = Arrays.copyOf(args, args.length + 1);
    newArgs[newArgs.length - 1] = "--app.folder=" + appFolder;
    SpringApplication.run(Main.class, newArgs);
  }

  @Autowired
  private IdGeneratorService idGeneratorService;

  @RequestMapping("/")
  public void returnOk(HttpServletResponse response) {
    response.addHeader("X-Frame-Options", "SAMEORIGIN");
  }

  @PostMapping("/")
  public String doPost(@RequestBody Pojo pojo) {
    return pojo.toString();
  }

  @RequestMapping("/id")
  public String generateId() {
    return idGeneratorService.generateId();
  }
}
