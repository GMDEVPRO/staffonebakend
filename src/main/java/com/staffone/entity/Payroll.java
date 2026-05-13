package com.staffone.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="payrolls",
    uniqueConstraints=@UniqueConstraint(
        columnNames={"tenant_id","employee_id","period_year","period_month"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payroll {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="tenant_id",nullable=false)   private Tenant tenant;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="employee_id",nullable=false) private Employee employee;
    @Column(name="period_year",nullable=false)  private Short periodYear;
    @Column(name="period_month",nullable=false) private Short periodMonth;
    @Column(name="gross_salary",nullable=false,precision=12,scale=2) private BigDecimal grossSalary;
    @Column(name="net_salary",nullable=false,precision=12,scale=2)   private BigDecimal netSalary;
    @Column(name="total_deductions",precision=12,scale=2) private BigDecimal totalDeductions;
    @Column(name="total_additions",precision=12,scale=2)  private BigDecimal totalAdditions;
    @Column(name="deductions_detail",columnDefinition="TEXT") private String deductionsDetail;
    @Column(name="additions_detail",columnDefinition="TEXT")  private String additionsDetail;
    @Column(name="country_config_version",length=20) private String countryConfigVersion;
    @Column(name="pdf_url") private String pdfUrl;
    @Enumerated(EnumType.STRING) @Column(nullable=false) @Builder.Default
    private PayrollStatus status=PayrollStatus.DRAFT;
    @Column(name="generated_at") private Instant generatedAt;
    @CreationTimestamp @Column(name="created_at",updatable=false) private Instant createdAt;

    public enum PayrollStatus { DRAFT, GENERATED, SENT, PAID }
}
