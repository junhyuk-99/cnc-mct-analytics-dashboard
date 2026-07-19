package com.demo.cnc.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Raw cumulative machine signal point collected from the shop floor.
 *
 * <p>Each document is a monotonically increasing counter reading for a single
 * machine and signal (for example {@code RunTime} or {@code CutTime}). The rollup
 * engine converts consecutive readings into hourly deltas.
 *
 * <p>This mirrors the raw signal-pool collection used by the production system,
 * reconstructed here with synthetic values only.
 */
@Document(collection = "machine_signal_pool")
public class MachineSignal {

    @Id
    private String id;
    private String machineId;
    private String signalName;
    private double value;
    private Instant endDate;
    private Long timespan;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getMachineId() { return machineId; }
    public void setMachineId(String machineId) { this.machineId = machineId; }
    public String getSignalName() { return signalName; }
    public void setSignalName(String signalName) { this.signalName = signalName; }
    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
    public Instant getEndDate() { return endDate; }
    public void setEndDate(Instant endDate) { this.endDate = endDate; }
    public Long getTimespan() { return timespan; }
    public void setTimespan(Long timespan) { this.timespan = timespan; }
}
