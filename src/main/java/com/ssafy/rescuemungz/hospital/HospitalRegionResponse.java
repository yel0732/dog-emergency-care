package com.ssafy.rescuemungz.hospital;

import java.util.List;
import java.util.Map;

public record HospitalRegionResponse(List<String> sidos, Map<String, List<String>> sigungus, List<HospitalRegionCount> counts) {
}
