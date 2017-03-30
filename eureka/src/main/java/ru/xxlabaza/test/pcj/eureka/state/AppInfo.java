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

import com.netflix.appinfo.InstanceInfo.InstanceStatus;
import lombok.Builder;
import lombok.Data;

/**
 * @author Artem Labazin <xxlabaza@gmail.com>
 * @since 22.06.2016
 */
@Data
@Builder
public class AppInfo {

  private final String name;

  private final String ip;

  private final Color color;

  private final InstanceStatus status;
}
