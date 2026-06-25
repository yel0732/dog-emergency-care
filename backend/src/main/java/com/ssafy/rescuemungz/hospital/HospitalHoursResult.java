package com.ssafy.rescuemungz.hospital;

public record HospitalHoursResult(int requested, int updated, int open24, int night, int failed, String message) {
}
