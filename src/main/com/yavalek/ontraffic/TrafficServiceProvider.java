package com.yavalek.ontraffic;

import com.google.api.services.mirror.model.Location;
import com.yavalek.ontraffic.model.NearLog;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public interface TrafficServiceProvider {
    boolean AUTOZOOM_ENABLED = false;

    StringBuilder getMapLink(Location location, List<NearLog> nearestLogs, HttpServletRequest reqest);

    List<NearLog> getNearestLogs(Location location);
}
