package com.staffone.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.*;
import java.util.UUID;

@Entity @Table(name="leave_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Leave {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="tenant_id",nullable=false)   private Tenant tenant;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="employee_id",nullable=false) private Employee employee;
    @Enumerated(EnumType.STRING) @Column(name="leave_type",nullable=false) private LeaveType leaveType;
    @Column(name="start_date",nullable=false) private LocalDate startDate;
    @Column(name="end_date",nullable=false)   private LocalDate endDate;
    @Column(name="days_count",nullable=false) private Integer daysCount;
    private String reason;
    @Column(name="document_url") private String documentUrl;
    @Enumerated(EnumType.STRING) @Column(nullable=false) @Builder.Default
    private LeaveStatus status=LeaveStatus.PENDING;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="reviewed_by") private Employee reviewedBy;
    @Column(name="review_note") private String reviewNote;
    @Column(name="reviewed_at") private Instant reviewedAt;
    @CreationTimestamp @Column(name="created_at",updatable=false) private Instant createdAt;

    public enum LeaveType   { ANNUAL, SICK, MATERNITY, PATERNITY, UNPAID, PUBLIC_HOLIDAY }
    public enum LeaveStatus { PENDING, APPROVED, REJECTED, CANCELLED }
}
