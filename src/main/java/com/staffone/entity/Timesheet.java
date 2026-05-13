package com.staffone.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

// ── Timesheet ───────────────────────────────────────────────────
@Entity
@Table(name="timesheets",
    uniqueConstraints=@UniqueConstraint(columnNames={"tenant_id","employee_id","date"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Timesheet {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="tenant_id",nullable=false) private Tenant tenant;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="employee_id",nullable=false) private Employee employee;
    @Column(nullable=false) private LocalDate date;
    @Column(name="check_in")  private LocalTime checkIn;
    @Column(name="check_out") private LocalTime checkOut;
    @Enumerated(EnumType.STRING) @Column(nullable=false) @Builder.Default
    private AttendanceSource source=AttendanceSource.MOBILE;
    @Column(name="geo_lat",precision=9,scale=6) private BigDecimal geoLat;
    @Column(name="geo_lng",precision=9,scale=6) private BigDecimal geoLng;
    @Column(name="offline_queued") @Builder.Default private boolean offlineQueued=false;
    @Column(name="synced_at") private Instant syncedAt;
    private String note;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="created_by") private Employee createdBy;
    @CreationTimestamp @Column(name="created_at",updatable=false) private Instant createdAt;

    public enum AttendanceSource { MOBILE, MANUAL, GEOFENCE, EDUKIRA_SYNC }
}
