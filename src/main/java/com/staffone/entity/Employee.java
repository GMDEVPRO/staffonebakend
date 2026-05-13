package com.staffone.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name="employees",
    uniqueConstraints=@UniqueConstraint(columnNames={"tenant_id","email"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Employee {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)
    @JoinColumn(name="tenant_id",nullable=false) private Tenant tenant;
    @Column(name="first_name",nullable=false,length=100) private String firstName;
    @Column(name="last_name",nullable=false,length=100)  private String lastName;
    @Column(length=255) private String email;
    @Column(length=30)  private String phone;
    @Enumerated(EnumType.STRING) @Column(nullable=false) @Builder.Default
    private EmployeeRole role=EmployeeRole.STAFF;
    @Column(length=100) private String department;
    @Column(length=100) private String position;
    @Column(name="hire_date",nullable=false) private LocalDate hireDate;
    @Enumerated(EnumType.STRING) @Column(nullable=false) @Builder.Default
    private EmployeeStatus status=EmployeeStatus.ACTIVE;
    @Column(name="edukira_teacher_id",length=100) private String edukiraTeacherId;
    @Column(name="edukira_tenant_id",length=100)  private String edukiraTenantId;
    @Column(name="last_edukira_sync") private Instant lastEdukiraSync;
    @CreationTimestamp @Column(name="created_at",updatable=false) private Instant createdAt;
    @UpdateTimestamp   @Column(name="updated_at")                 private Instant updatedAt;

    public String  getFullName()        { return firstName+" "+lastName; }
    public boolean isLinkedToEdukira()  { return edukiraTeacherId!=null&&!edukiraTeacherId.isBlank(); }

    public enum EmployeeRole   { ADMIN, MANAGER, STAFF, TEACHER }
    public enum EmployeeStatus { ACTIVE, INACTIVE, ON_LEAVE }
}
