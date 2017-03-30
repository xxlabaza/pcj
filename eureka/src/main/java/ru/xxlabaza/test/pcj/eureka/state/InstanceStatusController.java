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

import static com.netflix.appinfo.InstanceInfo.InstanceStatus.UNKNOWN;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static ru.xxlabaza.test.pcj.eureka.state.Color.UNDEFINED;

import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Artem Labazin <xxlabaza@gmail.com>
 * @since 22.06.2016
 */
@Slf4j
@RestController
class InstanceStatusController {

  @Autowired
  private InstanceStateService instanceStateService;

  @SneakyThrows
  @RequestMapping(value = "status", method = POST)
  public List<AppInfo> changeStatus (@RequestParam("names") Set<String> names,
                                     @RequestParam("color") String colorName,
                                     @RequestParam("status") String statusName,
                                     HttpServletResponse response
  ) {
    if (names == null || names.isEmpty()) {
      response.sendError(SC_BAD_REQUEST, "Request parameter 'names' is required");
      return null;
    }

    val color = Color.of(colorName);
    if (color == UNDEFINED) {
      response.sendError(SC_BAD_REQUEST, "Unset or unknown 'color' request parameter");
      return null;
    }

    val status = InstanceStatus.toEnum(statusName);
    if (status == UNKNOWN) {
      response.sendError(SC_BAD_REQUEST, "Unset or unknown 'status' request parameter");
      return null;
    }

    return instanceStateService.changeStatus(names, color, status);
  }
}
