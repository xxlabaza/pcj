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

/**
 * @author Artem Labazin <xxlabaza@gmail.com>
 * @since 26.03.2017
 */
public final class PredicateContextHolder {

  private static final ThreadLocal<String> THREAD_LOCAL;

  static {
    THREAD_LOCAL = new ThreadLocal<>();
  }

  public static void set(String className) {
    THREAD_LOCAL.set(className);
  }

  public static String get() {
    return THREAD_LOCAL.get();
  }

  public static boolean isEmpty() {
    return THREAD_LOCAL.get() == null;
  }

  public static void remove() {
    THREAD_LOCAL.remove();
  }

  private PredicateContextHolder() {
  }
}
