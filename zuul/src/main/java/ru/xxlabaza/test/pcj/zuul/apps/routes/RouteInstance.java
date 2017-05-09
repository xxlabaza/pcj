package ru.xxlabaza.test.pcj.zuul.apps.routes;

import java.util.Map;
import java.util.Optional;
import lombok.Value;
import org.springframework.cloud.client.ServiceInstance;

/**
 *
 * @author alabazin
 */
@Value
class RouteInstance {

    String host;

    String version;

    String color;

    boolean secure;

    static RouteInstance create (ServiceInstance serviceInstance) {
        Map<String, String> metadata = serviceInstance.getMetadata();
        String version = Optional.ofNullable(metadata)
                .map(it -> it.get("version"))
                .orElse("no version");
        String color = Optional.ofNullable(metadata)
                .map(it -> it.get("container.color"))
                .orElse("no color");
        return new RouteInstance(
                serviceInstance.getUri().toString(),
                version,
                color,
                serviceInstance.isSecure()
        );
    }
}
