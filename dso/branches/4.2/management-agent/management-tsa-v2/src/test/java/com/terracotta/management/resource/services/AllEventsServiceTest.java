package com.terracotta.management.resource.services;

import org.glassfish.jersey.media.sse.EventInput;
import org.glassfish.jersey.media.sse.InboundEvent;
import org.glassfish.jersey.media.sse.SseFeature;
import org.junit.Ignore;
import org.junit.Test;
import org.terracotta.management.resource.ResponseEntityV2;
import org.terracotta.management.resource.SubGenericType;
import org.terracotta.management.resource.events.EventEntityV2;

import com.terracotta.management.resource.ServerEntityV2;
import com.terracotta.management.resource.ServerGroupEntityV2;
import com.terracotta.management.resource.TopologyEntityV2;

import java.util.Collection;
import java.util.Set;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
public class AllEventsServiceTest {

  public static final String AGENT_ROOT_URI = "http://localhost:9540/tc-management-api";

  @Test
  @Ignore
  public void allEventsTest() {
    Client client = ClientBuilder.newBuilder().register(SseFeature.class).build();
    WebTarget target = client.target(AGENT_ROOT_URI + "/v2/events");
//    WebTarget target = client.target("http://localhost:9889/tmc/api/v2/events");

    EventInput eventInput = target.request().get(EventInput.class);
    while (!eventInput.isClosed()) {
      final InboundEvent inboundEvent = eventInput.read();
      if (inboundEvent == null) {
        // connection has been closed
        break;
      }
      // CacheManagerEntityEventV2 cacheManagerEntityEventV2 = inboundEvent.readData(CacheManagerEntityEventV2.class);
      //
      //
      // System.out.println(inboundEvent.getName() + " message was received from the L2 SSE Resource Service");
      // System.out.println("Here are the details :");
      // System.out.println("Type of the event : " + cacheManagerEntityEventV2.getType());
      //
      // System.out.println("CacheManager name : " + cacheManagerEntityEventV2.getCacheManagerName());
      // if(cacheManagerEntityEventV2.getCacheEntities() != null) {
      // System.out.println("Cache Attributes :");
      // for (Map<String, Object> cacheEntities : cacheManagerEntityEventV2.getCacheEntities()) {
      // for (Entry<String, Object> iterable_element : cacheEntities.entrySet()) {
      // System.out.println(iterable_element.getKey() + "  " + iterable_element.getValue());
      // }
      //
      // }
      // }
      // System.out.println("end of the event");


      System.out.println("EVENT: " + inboundEvent.readData());

      EventEntityV2 eventEntityV2 = inboundEvent.readData(EventEntityV2.class);
      String type = eventEntityV2.getType();

      if (type.equals("TSA.L2.STATE_CHANGE")) {
        String serverName = (String)eventEntityV2.getRootRepresentables().get("Server.Name");
        String state = (String)eventEntityV2.getRootRepresentables().get("State");
        if (state.equals("PASSIVE-STANDBY") || state.equals("ACTIVE-COORDINATOR")) {
          System.out.println("a L2 state changed to active or passive");
          boolean found = printServerTopology(client, serverName);
          if (found) {System.out.println("server found in topology");}
          else {System.out.println("server NOT found in topology");}
        }
      }

      if (type.equals("TSA.TOPOLOGY.NODE_JOINED")) {
        System.out.println("Node joined -> requesting topology");
        String serverName = (String)eventEntityV2.getRootRepresentables().get("Server.Name");
        boolean found = printServerTopology(client, serverName);
        if (found) {System.out.println("server found in topology");}
        else {System.out.println("server NOT found in topology");}
      }


    }

  }

  private boolean printServerTopology(Client client, String serverName) {
    WebTarget webTarget = client.target(AGENT_ROOT_URI + "/v2/agents/topologies/servers;names=" + serverName);
    Response response = webTarget.request().get();
    ResponseEntityV2<TopologyEntityV2> responseEntityV2 = response.readEntity(new SubGenericType<ResponseEntityV2, TopologyEntityV2>(ResponseEntityV2.class, TopologyEntityV2.class));

    for (TopologyEntityV2 topologyEntity : responseEntityV2.getEntities()) {
      for (ServerGroupEntityV2 serverGroupEntity : topologyEntity.getServerGroupEntities()) {
        for (ServerEntityV2 server : serverGroupEntity.getServers()) {
          Object name = server.getAttributes().get("Name");
          System.out.println("server name: " + name);
          if (name.equals(serverName)) {
            return true;
          }
        }
      }
    }
    return false;
  }

}
