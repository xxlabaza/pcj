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
package ru.xxlabaza.test.pcj.zuul.ribbon.predicate;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.loadbalancer.AbstractServerPredicate;
import com.netflix.loadbalancer.PredicateKey;
import com.netflix.loadbalancer.Server;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;
import com.netflix.zuul.context.RequestContext;
import lombok.val;

/**
 * @author Artem Labazin <xxlabaza@gmail.com>
 * @since 23.03.2017
 */
public abstract class AbstractPredicate extends AbstractServerPredicate
    implements Comparable<AbstractPredicate> {

  private static final String IS_PROCESSED_BY_PREDICATE;

  static {
    IS_PROCESSED_BY_PREDICATE = "is_processed_by_predicate";
  }

  private final int order;

  public AbstractPredicate(int order) {
    this.order = order;
  }

  @Override
  public boolean apply(PredicateKey predicateKey) {
//    if (!PredicateContextHolder.isEmpty() && !PredicateContextHolder.get().equals(getClass().getName())) {
//      return true;
//    }
    RequestContext requestContext = RequestContext.getCurrentContext();
    String className = requestContext.getOrDefault(IS_PROCESSED_BY_PREDICATE, "").toString();
    if (!className.isEmpty() && !className.equals(getClass().getName())) {
      return true;
    }

    if (shouldApply(requestContext)) {
//      PredicateContextHolder.set(getClass().getName());
      requestContext.set(IS_PROCESSED_BY_PREDICATE, className);
    } else {
      return true;
    }

    val server = predicateKey.getServer();
    if (!(server instanceof DiscoveryEnabledServer)) {
      return true;
    }

    val instanceInfo = ((DiscoveryEnabledServer) server).getInstanceInfo();
    return apply(predicateKey.getServer(), instanceInfo);
  }

  @Override
  public int compareTo(AbstractPredicate predicate) {
    return Integer.compare(getOrder(), predicate.getOrder());
  }

  public int getOrder() {
    return order;
  }

  protected abstract boolean shouldApply(RequestContext requestContext);

  protected abstract boolean apply(Server server, InstanceInfo instanceInfo);
}
