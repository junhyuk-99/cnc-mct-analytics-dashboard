package com.demo.cnc.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Pre-aggregated hourly cutting-time bucket produced by the rollup engine.
 *
 * <p>One document per {@code (machineId, baseDate, hour)}, mirroring
 * {@link RuntimeDaily} for the cutting-time signal.
 */
@Document(collection = "cuttime_daily")
public class CuttimeDaily {

    @Id
    private String id;
    private String machineId;
    private Instant baseDate;
    private int year;
    private int month;
    private int day;
    private int hour;
    private double cutTime;
    private Instant createdAt;
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getMachineId() { return machineId; }
    public void setMachineId(String machineId) { this.machineId = machineId; }
    public Instant getBaseDate() { return baseDate; }
    public void setBaseDate(Instant baseDate) { this.baseDate = baseDate; }
    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }
    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }
    public int getDay() { return day; }
    public void setDay(int day) { this.day = day; }
    public int getHour() { return hour; }
    public void setHour(int hour) { this.hour = hour; }
    public double getCutTime() { return cutTime; }
    public void setCutTime(double cutTime) { this.cutTime = cutTime; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
