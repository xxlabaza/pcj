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
package ru.xxlabaza.test.pcj.zuul.exception;

import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

import com.netflix.zuul.exception.ZuulException;
import javax.servlet.http.HttpServletRequest;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import ru.xxlabaza.test.pcj.zuul.AppProperties;

/**
 * @author Artem Labazin <xxlabaza@gmail.com>
 * @since 24.06.2016
 */
@Controller
class AppErrorController implements ErrorController {

  @Autowired
  private AppProperties appProperties;

  @Value("${error.path:/error}")
  private String errorPath;

  @Override
  public String getErrorPath() {
    return errorPath;
  }

  @RequestMapping("${error.path:/error}")
  @ResponseBody
  public ResponseEntity<?> error(HttpServletRequest request) {
    val status = getErrorStatus(request);
    val exception = getException(request);
    val responseBuilder = exception != null && isZuulForwardingException(request, exception)
                          ? ResponseEntity.status(SERVICE_UNAVAILABLE.value())
                          : ResponseEntity.status(status);
    return responseBuilder.body(null);
  }

  private int getErrorStatus(HttpServletRequest request) {
    Integer statusCode = (Integer)request.getAttribute("javax.servlet.error.status_code");
    return statusCode != null ? statusCode : HttpStatus.INTERNAL_SERVER_ERROR.value();
  }

  private Exception getException(HttpServletRequest request) {
    return (Exception) request.getAttribute("javax.servlet.error.exception");
  }

  private boolean isZuulForwardingException(HttpServletRequest request, Exception exception) {
    val message = exception.getMessage();
    return exception instanceof ZuulException && message != null
           && message.contains("Forwarding error")
           && request.getHeader(appProperties.getZuul().getTargetHostPortHeaderName()) != null;
  }
}
