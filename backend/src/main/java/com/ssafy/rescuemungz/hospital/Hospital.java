package com.ssafy.rescuemungz.hospital;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Hospital {
    private Long id;
    private String name;
    private String address;
    private String phone;
    private BigDecimal lat;
    private BigDecimal lng;
    private Boolean is24h;
    private String openingHours;
    private Boolean emergencyAvailable;
    private Boolean nightAvailable;
    private Boolean holidayAvailable;
    private String operatingStatus;
    private String kakaoPlaceId;
    private LocalDateTime lastVerifiedAt;
    private LocalDateTime updatedAt;
    private Double distanceKm;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public BigDecimal getLat() { return lat; }
    public void setLat(BigDecimal lat) { this.lat = lat; }
    public BigDecimal getLng() { return lng; }
    public void setLng(BigDecimal lng) { this.lng = lng; }
    public Boolean getIs24h() { return is24h; }
    public void setIs24h(Boolean is24h) { this.is24h = is24h; }
    public String getOpeningHours() { return openingHours; }
    public void setOpeningHours(String openingHours) { this.openingHours = openingHours; }
    public Boolean getEmergencyAvailable() { return emergencyAvailable; }
    public void setEmergencyAvailable(Boolean emergencyAvailable) { this.emergencyAvailable = emergencyAvailable; }
    public Boolean getNightAvailable() { return nightAvailable; }
    public void setNightAvailable(Boolean nightAvailable) { this.nightAvailable = nightAvailable; }
    public Boolean getHolidayAvailable() { return holidayAvailable; }
    public void setHolidayAvailable(Boolean holidayAvailable) { this.holidayAvailable = holidayAvailable; }
    public String getOperatingStatus() { return operatingStatus; }
    public void setOperatingStatus(String operatingStatus) { this.operatingStatus = operatingStatus; }
    public String getKakaoPlaceId() { return kakaoPlaceId; }
    public void setKakaoPlaceId(String kakaoPlaceId) { this.kakaoPlaceId = kakaoPlaceId; }
    public LocalDateTime getLastVerifiedAt() { return lastVerifiedAt; }
    public void setLastVerifiedAt(LocalDateTime lastVerifiedAt) { this.lastVerifiedAt = lastVerifiedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(Double distanceKm) { this.distanceKm = distanceKm; }
}
