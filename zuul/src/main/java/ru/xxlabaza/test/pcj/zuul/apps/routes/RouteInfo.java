package ru.xxlabaza.test.pcj.zuul.apps.routes;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import java.util.Map;
import lombok.Value;
import ru.xxlabaza.test.pcj.zuul.AppProperties.Zuul.ColorTrafficRule;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

/**
 *
 * @author alabazin
 */
@Value
@JsonInclude(NON_NULL)
@JsonPropertyOrder({
  "serviceId",
  "path",
  "instances",
  "versions",
  "colors"
})
class RouteInfo {

  String serviceId;

  String path;

  List<RouteInstance> instances;

  Map<String, Integer> versions;

  ColorTrafficRule colors;
}
