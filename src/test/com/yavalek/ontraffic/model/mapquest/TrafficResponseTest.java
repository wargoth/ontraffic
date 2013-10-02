package com.yavalek.ontraffic.model.mapquest;


import com.google.gson.Gson;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class TrafficResponseTest {
    @Test
    public void testFromJson() {
        String data = "{\"incidents\":[{\"delayFromFreeFlow\":0,\"delayFromTypical\":0,\"fullDesc\":\"Parking restrictions " +
                "in force due to construction on Clarendon Avenue both ways between Laguna Honda Boulevard and Twin Peaks " +
                "Boulevard.\",\"severity\":2,\"lng\":-122.448469,\"shortDesc\":\"Clarendon Avenue : Parking restrictions " +
                "in force between Laguna Honda Boulevard and Twin Peaks Boulevard \",\"type\":1,\"endTime\":\"2015-04-02T02:59:00\"," +
                "\"id\":\"330625178\",\"startTime\":\"2012-07-20T03:00:00\",\"distance\":0,\"impacting\":true,\"tmcs\":[]," +
                "\"eventCode\":701,\"iconURL\":\"http://api.mqcdn.com/mqtraffic/const_mod.png\",\"lat\":37.759046}]," +
                "\"mqURL\":\"http://www.mapquest.com/maps?traffic=1&latitude=37.778313&longitude=-122.419333\",\"info\"" +
                ":{\"copyright\":{\"text\":\"© 2013 MapQuest, Inc.\",\"imageUrl\":\"http://api.mqcdn.com/res/mqlogo.gif\"," +
                "\"imageAltText\":\"© 2013 MapQuest, Inc.\"},\"statuscode\":0,\"messages\":[]}}";

        Gson gson = new Gson();
        TrafficResponse trafficResponse = gson.fromJson(data, TrafficResponse.class);

        assertEquals(1, trafficResponse.incidents.size());

        Incident incident = trafficResponse.incidents.get(0);
        assertEquals(37.759046, incident.lat);
        assertEquals(-122.448469, incident.lng);
        assertEquals("Parking restrictions in force due to construction on Clarendon Avenue both ways between " +
                "Laguna Honda Boulevard and Twin Peaks Boulevard.", incident.fullDesc);
    }
}
