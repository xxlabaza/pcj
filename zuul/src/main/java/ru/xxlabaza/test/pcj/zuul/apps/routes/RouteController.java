package ru.xxlabaza.test.pcj.zuul.apps.routes;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties.ZuulRoute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.xxlabaza.test.pcj.zuul.AppProperties;
import ru.xxlabaza.test.pcj.zuul.AppProperties.Zuul.ColorTrafficRule;
import ru.xxlabaza.test.pcj.zuul.apps.ServiceNotFoundException;
import ru.xxlabaza.test.pcj.zuul.ribbon.MetadataBalancingProperties;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 *
 * @author alabazin
 */
@RestController
class RouteController {

  @Autowired
  private DiscoveryClient discoveryClient;

  @Autowired
  private ZuulProperties zuulProperties;

  @Autowired
  private AppProperties appProperties;

  @Autowired
  private MetadataBalancingProperties metadataBalancingProperties;

  @RequestMapping("/admin/ext/routes")
  public Map<String, RouteInfo> getRoutes () {
    return zuulProperties.getRoutes().values().stream()
            .filter(it -> it.getServiceId() != null)
            .collect(toMap(ZuulRoute::getId, this::create));
  }

  @RequestMapping("/admin/ext/routes/{id}")
  public RouteInfo getRoute (@PathVariable("id") String id) {
    return zuulProperties.getRoutes().values().stream()
            .filter(it -> it.getId().equalsIgnoreCase(id) || it.getServiceId().equalsIgnoreCase(id))
            .findAny()
            .map(this::create)
            .orElseThrow(ServiceNotFoundException::new);
  }

  private RouteInfo create (ZuulRoute zuulRoute) {
    String serviceId = zuulRoute.getServiceId();

    List<ServiceInstance> serviceInstances = discoveryClient.getInstances(serviceId);

    List<RouteInstance> instances = Optional.ofNullable(serviceInstances)
            .map(it -> it.stream().map(RouteInstance::create).collect(toList()))
            .orElse(Collections.emptyList());

    Map<String, Map<String, Integer>> rules = metadataBalancingProperties.getRules();
    Map<String, Integer> versions = rules != null
                                    ? rules.get(serviceId)
                                    : null;

    Map<String, ColorTrafficRule> colorTrafficRules = appProperties.getZuul().getTrafficRules();
    ColorTrafficRule color = colorTrafficRules != null
                             ? colorTrafficRules.get(serviceId.toUpperCase())
                             : null;

    return new RouteInfo(serviceId, zuulRoute.getPath(), instances, versions, color);
  }
}
