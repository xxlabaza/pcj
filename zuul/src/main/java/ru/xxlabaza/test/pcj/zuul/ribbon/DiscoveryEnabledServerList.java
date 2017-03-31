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

import static com.netflix.appinfo.InstanceInfo.InstanceStatus.OUT_OF_SERVICE;
import static com.netflix.appinfo.InstanceInfo.InstanceStatus.UP;
import static com.netflix.client.config.CommonClientConfigKey.DeploymentContextBasedVipAddresses;
import static com.netflix.client.config.CommonClientConfigKey.ForceClientPortConfiguration;
import static com.netflix.client.config.CommonClientConfigKey.IsSecure;
import static com.netflix.client.config.CommonClientConfigKey.Port;
import static com.netflix.client.config.CommonClientConfigKey.SecurePort;
import static com.netflix.client.config.CommonClientConfigKey.TargetRegion;
import static com.netflix.client.config.CommonClientConfigKey.UseIPAddrForServer;
import static com.netflix.client.config.DefaultClientConfigImpl.DEFAULT_PORT;
import static com.netflix.client.config.DefaultClientConfigImpl.DEFAULT_USEIPADDRESS_FOR_SERVER;
import static java.util.stream.Collectors.toList;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.config.ConfigurationManager;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.DiscoveryManager;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import com.netflix.loadbalancer.AbstractServerList;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
public class DiscoveryEnabledServerList extends AbstractServerList<DiscoveryEnabledServer> {

  private String clientName;

  private String vipAddresses;

  private boolean isSecure;

  private String datacenter;

  private String targetRegion;

  private int overridePort = DefaultClientConfigImpl.DEFAULT_PORT;

  private boolean shouldUseOverridePort = false;

  private boolean shouldUseIpAddr;

  public DiscoveryEnabledServerList (String vipAddresses) {
    IClientConfig clientConfig = DefaultClientConfigImpl.getClientConfigWithDefaultValues();
    clientConfig.set(DeploymentContextBasedVipAddresses, vipAddresses);
    initWithNiwsConfig(clientConfig);
  }

  public DiscoveryEnabledServerList (IClientConfig clientConfig) {
    initWithNiwsConfig(clientConfig);
  }

  @Override
  public final void initWithNiwsConfig (IClientConfig clientConfig) {
    clientName = clientConfig.getClientName();
    vipAddresses = clientConfig.resolveDeploymentContextbasedVipAddresses();
    if (vipAddresses == null && ConfigurationManager.getConfigInstance().getBoolean(
        "DiscoveryEnabledNIWSServerList.failFastOnNullVip", true)) {
      throw new NullPointerException("VIP address for client " + clientName + " is null");
    }

    isSecure = clientConfig.getPropertyAsBoolean(IsSecure, false);
    datacenter = ConfigurationManager.getDeploymentContext().getDeploymentDatacenter();
    targetRegion = clientConfig.getPropertyAsString(TargetRegion, null);
    shouldUseIpAddr = clientConfig.getPropertyAsBoolean(UseIPAddrForServer,
                                                        DEFAULT_USEIPADDRESS_FOR_SERVER);

    // override client configuration and use client-defined port
    if (clientConfig.getPropertyAsBoolean(ForceClientPortConfiguration, false)) {
      if (isSecure) {
        if (clientConfig.containsProperty(SecurePort)) {
          overridePort = clientConfig.getPropertyAsInteger(SecurePort, DEFAULT_PORT);
          shouldUseOverridePort = true;
        } else {
          log.warn(clientName + " set to force client port but no secure port is set, so ignoring");
        }
      } else if (clientConfig.containsProperty(Port)) {
        overridePort = clientConfig.getPropertyAsInteger(Port, DEFAULT_PORT);
        shouldUseOverridePort = true;
      } else {
        log.warn(clientName + " set to force client port but no port is set, so ignoring");
      }
    }
  }

  @Override
  public List<DiscoveryEnabledServer> getInitialListOfServers () {
    return obtainServersViaDiscovery();
  }

  @Override
  public List<DiscoveryEnabledServer> getUpdatedListOfServers () {
    return obtainServersViaDiscovery();
  }

  public String getVipAddresses () {
    return vipAddresses;
  }

  public void setVipAddresses (String vipAddresses) {
    this.vipAddresses = vipAddresses;
  }

  @Override
  public String toString () {
    StringBuilder sb = new StringBuilder("DiscoveryEnabledNIWSServerList:");
    sb.append("; clientName:").append(clientName);
    sb.append("; Effective vipAddresses:").append(vipAddresses);
    sb.append("; isSecure:").append(isSecure);
    sb.append("; datacenter:").append(datacenter);
    return sb.toString();
  }

  private List<DiscoveryEnabledServer> obtainServersViaDiscovery () {
    DiscoveryClient discoveryClient = DiscoveryManager.getInstance().getDiscoveryClient();
    if (discoveryClient == null || vipAddresses == null) {
      return Collections.emptyList();
    }

    Applications applications = discoveryClient.getApplicationsForARegion(targetRegion);
    if (applications == null) {
      return Collections.emptyList();
    }

    return Stream.of(getVipAddresses().split(","))
        .map(it -> applications.getRegisteredApplications(it))
        .filter(Objects::nonNull)
        .map(Application::getInstancesAsIsFromEureka)
        .filter(Objects::nonNull)
        .flatMap(it -> it.stream())
        .filter(it -> it.getStatus().equals(UP) || it.getStatus().equals(OUT_OF_SERVICE))
        .map(it -> {
          if (!shouldUseOverridePort) {
            return it;
          }
          InstanceInfo copy = new InstanceInfo(it);
          return isSecure
                 ? new InstanceInfo.Builder(copy).setSecurePort(overridePort).build()
                 : new InstanceInfo.Builder(copy).setPort(overridePort).build();
        })
        .map(it -> new DiscoveryEnabledServer(it, isSecure, shouldUseIpAddr))
        .peek(it -> it.setZone(DiscoveryClient.getZone(it.getInstanceInfo())))
        .collect(toList());
  }
}
