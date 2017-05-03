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

/**
 * @author Artem Labazin <xxlabaza@gmail.com>
 * @since 28.04.2017
 */
public interface FiltersOrder {

  int PRE_TARGET_HOST_HEADER_EXCTRACTOR_ORDER = 501;
  int PRE_TARGET_HOST_PORT_HEADER_EXCTRACTOR_ORDER = 502;
  int PRE_TARGET_SERVICE_VERSION_HEADER_EXCTRACTOR_ORDER = 504;
  int PRE_TARGET_SERVICE_VERSION_COOKIE_EXCTRACTOR_ORDER = 505;
  int PRE_TARGET_SERVICE_VERSION_URL_EXCTRACTOR_ORDER = 506;
  int PRE_TARGET_COLOR_HEADER_EXTRACTOR_ORDER = 507;
  int PRE_YODA_COOKIE = 508;
  int PRE_PEND_PATH_ORDER = 1110;
  int PRE_REQUEST_ID_ORDER = 1500;

  int POST_ATTACH_RESPONSE_PRODUCER_HOST_PORT_ORDER = 2000;
  int POST_SET_YODA_COOKIE_ORDER = 2001;
  int POST_SET_SERVICE_VERSION_COOKIE_ORDER = 2002;

  int ERROR_LOGGING_ORDER = 2001;
}
