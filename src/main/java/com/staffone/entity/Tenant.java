// FILE: Tenant.java
package com.staffone.entity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.util.*;

@Entity @Table(name="tenants")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Tenant {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Column(nullable=false) private String name;
    @Column(unique=true,nullable=false) private String slug;
    @Column(name="country_code",length=2,nullable=false) private String countryCode;
    @Column(length=50) private String sector;
    @Enumerated(EnumType.STRING) @Column(nullable=false) @Builder.Default private TenantPlan plan=TenantPlan.STARTER;
    @Enumerated(EnumType.STRING) @Column(nullable=false) @Builder.Default private TenantMode mode=TenantMode.RH_ONLY;
    @Column(length=10) @Builder.Default private String locale="fr";
    @Column(length=50) @Builder.Default private String timezone="Africa/Dakar";
    @Column(length=3) @Builder.Default private String currency="XOF";
    @Enumerated(EnumType.STRING) @Column(nullable=false) @Builder.Default private TenantStatus status=TenantStatus.TRIAL;
    @Column(name="trial_ends_at") private Instant trialEndsAt;
    @ElementCollection(fetch=FetchType.EAGER)
    @CollectionTable(name="tenant_modules",joinColumns=@JoinColumn(name="tenant_id"))
    @Column(name="module") @Builder.Default private Set<String> activeModules=new HashSet<>();
    @CreationTimestamp @Column(name="created_at",updatable=false) private Instant createdAt;
    @UpdateTimestamp @Column(name="updated_at") private Instant updatedAt;

    public boolean hasModule(String m){ return activeModules.contains(m); }
    public boolean isBundle(){ return mode==TenantMode.BUNDLE; }
    public boolean isEdukirasSyncEnabled(){ return isBundle()&&hasModule("edukira_sync"); }

    public enum TenantPlan   { STARTER, PRO, BUSINESS }
    public enum TenantMode   { RH_ONLY, EDUKIRA_ONLY, BUNDLE }
    public enum TenantStatus { TRIAL, ACTIVE, SUSPENDED, CANCELLED }
}
