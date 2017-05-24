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
package ru.xxlabaza.test.pcj.balancing;

import static ru.xxlabaza.test.pcj.balancing.predicate.MetadataVersionPredicate.CURRENT_REQUEST_CONTEXT_VERSION;

import com.netflix.loadbalancer.AbstractServerPredicate;
import com.netflix.loadbalancer.CompositePredicate;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ZoneAvoidanceRule;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.LinkedList;
import java.util.List;

/**
 * @author Artem Labazin <xxlabaza@gmail.com>
 * @since 22.06.2016
 */
@Slf4j
public class JCPenneyPredicateBasedRule extends ZoneAvoidanceRule {

  private CompositePredicate predicate;

  @Autowired
  private List<AbstractServerPredicate> predicates;

  @Override
  public AbstractServerPredicate getPredicate() {
    if (predicate == null) {
      predicate = createPredicate();
    }
    return predicate;
  }

  @Override
  public Server choose (Object key) {
    Server result = super.choose(key);
    PredicateContextHolder currentContext = PredicateContextHolder.getCurrentContext();
    if (currentContext != null && currentContext.containsKey(CURRENT_REQUEST_CONTEXT_VERSION)) {
        log.debug("Remove current service version from context: {}",
                  currentContext.get(CURRENT_REQUEST_CONTEXT_VERSION));
        currentContext.remove(CURRENT_REQUEST_CONTEXT_VERSION);
    }
    return result;
  }

  private CompositePredicate createPredicate() {
    val linkedList = new LinkedList<>(predicates);
    // turn off zone avoidance:
    // linkedList.addLast(super.getPredicate());
    val array = linkedList.toArray(new AbstractServerPredicate[linkedList.size()]);
    return CompositePredicate.withPredicates(array).build();
  }
}
