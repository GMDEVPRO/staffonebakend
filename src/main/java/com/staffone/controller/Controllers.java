package com.staffone.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.staffone.compliance.*;
import com.staffone.entity.*;
import com.staffone.repository.*;
import com.staffone.security.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

// ── AUTH ─────────────────────────────────────────────────────────
@RestController @RequestMapping("/api/v1/auth") @RequiredArgsConstructor
class AuthController {
    private final TenantRepository   tenants;
    private final EmployeeRepository employees;
    private final JwtService         jwt;

    record LoginReq(@NotBlank String email, @NotBlank String password, @NotBlank String tenantSlug) {}
    record LoginRes(String token, String tenantId, String mode, String role) {}

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginReq req) {
        Tenant t = tenants.findBySlug(req.tenantSlug())
            .orElseThrow(() -> new RuntimeException("Tenant not found: " + req.tenantSlug()));
        Employee e = employees.findByEmailAndTenantId(req.email(), t.getId())
            .orElseThrow(() -> new RuntimeException("Bad credentials"));
        String token = jwt.generate(new StaffOnePrincipal(
            e.getId(), t.getId(), e.getEmail(), e.getRole().name(), t.getMode().name()));
        return ResponseEntity.ok(ApiResponse.ok(
            new LoginRes(token, t.getId().toString(), t.getMode().name(), e.getRole().name())));
    }
}

// ── TENANT ───────────────────────────────────────────────────────
@RestController @RequestMapping("/api/v1/tenants") @RequiredArgsConstructor
class TenantController {
    private final TenantRepository tenants;
    record TenantReq(@NotBlank String name, @NotBlank String slug,
                     @NotBlank @Size(min=2,max=2) String countryCode,
                     String sector, Tenant.TenantPlan plan, Tenant.TenantMode mode,
                     String locale, String currency, Set<String> activeModules) {}

    @PostMapping @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@Valid @RequestBody TenantReq req) {
        if (tenants.existsBySlug(req.slug()))
            return ResponseEntity.status(409).body(ApiResponse.err("Slug already used"));
        Tenant t = Tenant.builder()
            .name(req.name()).slug(req.slug()).countryCode(req.countryCode().toUpperCase())
            .sector(req.sector())
            .plan(req.plan()!=null?req.plan():Tenant.TenantPlan.STARTER)
            .mode(req.mode()!=null?req.mode():Tenant.TenantMode.RH_ONLY)
            .locale(req.locale()!=null?req.locale():"fr")
            .currency(req.currency()!=null?req.currency():"XOF")
            .status(Tenant.TenantStatus.TRIAL)
            .trialEndsAt(Instant.now().plus(java.time.Duration.ofDays(30)))
            .activeModules(req.activeModules()!=null?req.activeModules():
                Set.of("employees","attendance","payroll","leaves","documents"))
            .build();
        return ResponseEntity.status(201).body(ApiResponse.ok(tenants.save(t),"Tenant created"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable UUID id) {
        return tenants.findById(id).map(t->ResponseEntity.ok(ApiResponse.ok(t)))
            .orElse(ResponseEntity.notFound().build());
    }
}

// ── EMPLOYEES ────────────────────────────────────────────────────
@RestController @RequestMapping("/api/v1/tenants/{tenantId}/employees") @RequiredArgsConstructor
class EmployeeController {
    private final EmployeeRepository employees;
    private final TenantRepository   tenants;
    record EmpReq(@NotBlank String firstName, @NotBlank String lastName,
                  @Email String email, String phone, Employee.EmployeeRole role,
                  String department, String position, @NotNull LocalDate hireDate) {}
    record EmpRes(UUID id, String firstName, String lastName, String email, String phone,
                  String role, String department, String position,
                  LocalDate hireDate, String status, boolean linkedToEdukira) {}

    @PostMapping @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> create(@PathVariable UUID tenantId, @Valid @RequestBody EmpReq req) {
        assertAccess(tenantId);
        Tenant t = tenants.findById(tenantId).orElseThrow(()->new RuntimeException("Tenant not found"));
        if (req.email()!=null && employees.existsByEmailAndTenantId(req.email(),tenantId))
            return ResponseEntity.status(409).body(ApiResponse.err("Email already used"));
        Employee e = Employee.builder().tenant(t)
            .firstName(req.firstName()).lastName(req.lastName())
            .email(req.email()).phone(req.phone())
            .role(req.role()!=null?req.role():Employee.EmployeeRole.STAFF)
            .department(req.department()).position(req.position())
            .hireDate(req.hireDate()).status(Employee.EmployeeStatus.ACTIVE).build();
        return ResponseEntity.status(201).body(ApiResponse.ok(toRes(employees.save(e))));
    }

    @GetMapping
    public ResponseEntity<?> list(@PathVariable UUID tenantId,
                                  @RequestParam(defaultValue="0") int page,
                                  @RequestParam(defaultValue="20") int size,
                                  @RequestParam(required=false) String search) {
        assertAccess(tenantId);
        Pageable pg = PageRequest.of(page,size,Sort.by("lastName","firstName"));
        Page<Employee> res = search!=null&&!search.isBlank()
            ? employees.searchByTenant(tenantId,search,pg)
            : employees.findByTenantId(tenantId,pg);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
            "content",res.getContent().stream().map(this::toRes).toList(),
            "totalElements",res.getTotalElements(),"totalPages",res.getTotalPages())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable UUID tenantId, @PathVariable UUID id) {
        assertAccess(tenantId);
        return employees.findByIdAndTenantId(id,tenantId)
            .map(e->ResponseEntity.ok(ApiResponse.ok(toRes(e))))
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> update(@PathVariable UUID tenantId, @PathVariable UUID id,
                                    @RequestBody EmpReq req) {
        assertAccess(tenantId);
        return employees.findByIdAndTenantId(id,tenantId).map(e->{
            e.setFirstName(req.firstName()); e.setLastName(req.lastName());
            if (req.phone()!=null) e.setPhone(req.phone());
            if (req.department()!=null) e.setDepartment(req.department());
            if (req.position()!=null) e.setPosition(req.position());
            if (req.role()!=null) e.setRole(req.role());
            return ResponseEntity.ok(ApiResponse.ok(toRes(employees.save(e))));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deactivate(@PathVariable UUID tenantId, @PathVariable UUID id) {
        assertAccess(tenantId);
        return employees.findByIdAndTenantId(id,tenantId).map(e->{
            e.setStatus(Employee.EmployeeStatus.INACTIVE); employees.save(e);
            return ResponseEntity.ok(ApiResponse.ok(null,"Employee deactivated"));
        }).orElse(ResponseEntity.notFound().build());
    }

    private EmpRes toRes(Employee e) {
        return new EmpRes(e.getId(),e.getFirstName(),e.getLastName(),e.getEmail(),
            e.getPhone(),e.getRole().name(),e.getDepartment(),e.getPosition(),
            e.getHireDate(),e.getStatus().name(),e.isLinkedToEdukira());
    }
    // cross-tenant isolation: block access to other tenants
    private void assertAccess(UUID tenantId) {
        UUID cur = TenantContext.get();
        if (cur!=null&&!tenantId.equals(cur))
            throw new org.springframework.security.access.AccessDeniedException(
                "cross-tenant access denied");
    }
}

// ── TIMESHEETS ───────────────────────────────────────────────────
@RestController @RequestMapping("/api/v1/timesheets") @RequiredArgsConstructor
class TimesheetController {
    private final TimesheetRepository timesheets;
    private final EmployeeRepository  employees;
    private final TenantRepository    tenants;
    record CheckInReq(@NotNull UUID employeeId, LocalDate date, LocalTime checkIn,
                      LocalTime checkOut, String source, BigDecimal geoLat, BigDecimal geoLng,
                      boolean offlineQueued, String note) {}
    record BulkReq(@NotNull @Size(max=500) List<CheckInReq> records) {}

    @GetMapping
    public ResponseEntity<?> get(@RequestParam UUID employeeId,
                                 @RequestParam(required=false) LocalDate from,
                                 @RequestParam(required=false) LocalDate to) {
        UUID tid = SecurityConfig.currentTenantId();
        LocalDate f = from!=null?from:LocalDate.now().withDayOfMonth(1);
        LocalDate t2 = to!=null?to:LocalDate.now();
        return ResponseEntity.ok(ApiResponse.ok(
            timesheets.findByEmployeeAndPeriod(tid,employeeId,f,t2)));
    }

    @PostMapping
    public ResponseEntity<?> checkIn(@Valid @RequestBody CheckInReq req) {
        UUID tid = SecurityConfig.currentTenantId();
        Employee e = employees.findByIdAndTenantId(req.employeeId(),tid)
            .orElseThrow(()->new RuntimeException("Employee not found"));
        Tenant t = tenants.findById(tid).orElseThrow();
        LocalDate d = req.date()!=null?req.date():LocalDate.now();
        Timesheet ts = Timesheet.builder().tenant(t).employee(e).date(d)
            .checkIn(req.checkIn()!=null?req.checkIn():LocalTime.now())
            .checkOut(req.checkOut()).source(src(req.source()))
            .geoLat(req.geoLat()).geoLng(req.geoLng())
            .offlineQueued(req.offlineQueued())
            .syncedAt(req.offlineQueued()?null:Instant.now())
            .note(req.note()).build();
        return ResponseEntity.status(201).body(ApiResponse.ok(timesheets.save(ts)));
    }

    @PostMapping("/bulk")
    public ResponseEntity<?> bulk(@Valid @RequestBody BulkReq req) {
        UUID tid = SecurityConfig.currentTenantId();
        Tenant t = tenants.findById(tid).orElseThrow();
        int done=0; List<String> errs=new ArrayList<>();
        for (CheckInReq r : req.records()) {
            try {
                LocalDate d = r.date()!=null?r.date():LocalDate.now();
                if (!timesheets.existsByTenantIdAndEmployeeIdAndDate(tid,r.employeeId(),d)) {
                    Employee e = employees.findByIdAndTenantId(r.employeeId(),tid)
                        .orElseThrow(()->new RuntimeException("Not found: "+r.employeeId()));
                    timesheets.save(Timesheet.builder().tenant(t).employee(e).date(d)
                        .checkIn(r.checkIn()).checkOut(r.checkOut()).source(src(r.source()))
                        .offlineQueued(true).syncedAt(Instant.now()).build());
                    done++;
                }
            } catch (Exception ex) { errs.add(r.employeeId()+": "+ex.getMessage()); }
        }
        return ResponseEntity.ok(ApiResponse.ok(Map.of("processed",done,"errors",errs.size())));
    }

    private Timesheet.AttendanceSource src(String s) {
        if (s==null) return Timesheet.AttendanceSource.MOBILE;
        return switch(s.toUpperCase()) {
            case "MANUAL"       -> Timesheet.AttendanceSource.MANUAL;
            case "GEOFENCE"     -> Timesheet.AttendanceSource.GEOFENCE;
            case "EDUKIRA_SYNC" -> Timesheet.AttendanceSource.EDUKIRA_SYNC;
            default             -> Timesheet.AttendanceSource.MOBILE;
        };
    }
}

// ── PAYROLL ──────────────────────────────────────────────────────
@RestController @RequestMapping("/api/v1/payroll") @RequiredArgsConstructor
class PayrollController {
    private final PayrollRepository  payrolls;
    private final EmployeeRepository employees;
    private final TenantRepository   tenants;
    private final ComplianceService  compliance;
    private final PayrollCalculator  calculator;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    record PayReq(@NotNull UUID employeeId, @NotNull Short year,
                  @NotNull Short month, BigDecimal overrideGross) {}

    @PostMapping("/{country}") @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> generate(@PathVariable String country,
                                      @Valid @RequestBody PayReq req) {
        UUID tid = SecurityConfig.currentTenantId();
        if (!compliance.supports(country))
            return ResponseEntity.badRequest().body(ApiResponse.err(
                "Unsupported country: "+country+". Available: "+compliance.supported()));
        Employee emp = employees.findByIdAndTenantId(req.employeeId(),tid)
            .orElseThrow(()->new RuntimeException("Employee not found"));
        if (payrolls.findByTenantIdAndEmployeeIdAndPeriodYearAndPeriodMonth(
            tid,req.employeeId(),req.year(),req.month()).isPresent())
            return ResponseEntity.status(409).body(ApiResponse.err(
                "Payslip already generated for "+req.year()+"/"+req.month()));
        Tenant t = tenants.findById(tid).orElseThrow();
        CountryConfig cfg = compliance.get(country);
        BigDecimal gross = req.overrideGross()!=null?req.overrideGross():BigDecimal.valueOf(300000);
        PayrollCalculator.Result res = calculator.calculate(gross,cfg);
        String deductJson="[]";
        try { deductJson=mapper.writeValueAsString(res.deductions()); } catch(Exception ignored){}
        Payroll p = Payroll.builder().tenant(t).employee(emp)
            .periodYear(req.year()).periodMonth(req.month())
            .grossSalary(res.grossSalary()).netSalary(res.netSalary())
            .totalDeductions(res.cnssEmployee().add(res.incomeTax()))
            .totalAdditions(BigDecimal.ZERO).deductionsDetail(deductJson)
            .countryConfigVersion(country.toUpperCase()+"-v1.0")
            .status(Payroll.PayrollStatus.GENERATED).generatedAt(Instant.now()).build();
        Payroll saved = payrolls.save(p);
        return ResponseEntity.status(201).body(ApiResponse.ok(Map.of(
            "payrollId",saved.getId(),"employee",emp.getFullName(),
            "period",req.year()+"/"+String.format("%02d",req.month()),
            "grossSalary",res.grossSalary(),"cnss",res.cnssEmployee(),
            "incomeTax",res.incomeTax(),"netSalary",res.netSalary(),
            "currency",cfg.getCurrency())));
    }

    @GetMapping("/countries")
    public ResponseEntity<?> countries() {
        return ResponseEntity.ok(ApiResponse.ok(compliance.supported()));
    }

    @GetMapping
    public ResponseEntity<?> history(@RequestParam UUID employeeId) {
        UUID tid = SecurityConfig.currentTenantId();
        return ResponseEntity.ok(ApiResponse.ok(
            payrolls.findHistoryByEmployee(tid,employeeId,PageRequest.of(0,24))));
    }
}

// ── LEAVES ───────────────────────────────────────────────────────
@RestController @RequestMapping("/api/v1/leaves") @RequiredArgsConstructor
class LeaveController {
    private final LeaveRepository    leaves;
    private final EmployeeRepository employees;
    private final TenantRepository   tenants;
    record LeaveReq(@NotNull UUID employeeId, @NotNull Leave.LeaveType leaveType,
                    @NotNull LocalDate startDate, @NotNull LocalDate endDate, String reason) {}
    record ReviewReq(String note) {}

    @PostMapping
    public ResponseEntity<?> submit(@Valid @RequestBody LeaveReq req) {
        UUID tid = SecurityConfig.currentTenantId();
        Employee e = employees.findByIdAndTenantId(req.employeeId(),tid)
            .orElseThrow(()->new RuntimeException("Employee not found"));
        Tenant t = tenants.findById(tid).orElseThrow();
        if (req.endDate().isBefore(req.startDate()))
            return ResponseEntity.badRequest().body(ApiResponse.err("endDate must be after startDate"));
        long days = req.startDate().datesUntil(req.endDate().plusDays(1))
            .filter(d->d.getDayOfWeek().getValue()<=5).count();
        Leave l = Leave.builder().tenant(t).employee(e).leaveType(req.leaveType())
            .startDate(req.startDate()).endDate(req.endDate())
            .daysCount((int)days).reason(req.reason())
            .status(Leave.LeaveStatus.PENDING).build();
        return ResponseEntity.status(201).body(ApiResponse.ok(leaves.save(l)));
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam UUID employeeId,
                                  @RequestParam(defaultValue="0") int page,
                                  @RequestParam(defaultValue="20") int size) {
        UUID tid = SecurityConfig.currentTenantId();
        return ResponseEntity.ok(ApiResponse.ok(
            leaves.findByTenantIdAndEmployeeId(tid,employeeId,
                PageRequest.of(page,size,Sort.by("createdAt").descending())).getContent()));
    }

    @GetMapping("/pending") @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> pending() {
        return ResponseEntity.ok(ApiResponse.ok(
            leaves.findByTenantIdAndStatus(SecurityConfig.currentTenantId(),Leave.LeaveStatus.PENDING)));
    }

    @PutMapping("/{id}/approve") @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> approve(@PathVariable UUID id, @RequestBody ReviewReq req) {
        return review(id,Leave.LeaveStatus.APPROVED,req.note());
    }
    @PutMapping("/{id}/reject") @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> reject(@PathVariable UUID id, @RequestBody ReviewReq req) {
        return review(id,Leave.LeaveStatus.REJECTED,req.note());
    }
    private ResponseEntity<?> review(UUID id, Leave.LeaveStatus s, String note) {
        UUID tid = SecurityConfig.currentTenantId();
        return leaves.findById(id).filter(l->l.getTenant().getId().equals(tid)).map(l->{
            l.setStatus(s); l.setReviewNote(note); l.setReviewedAt(Instant.now());
            return ResponseEntity.ok(ApiResponse.ok(leaves.save(l)));
        }).orElse(ResponseEntity.notFound().build());
    }
}

// ── EXCEPTION HANDLER ────────────────────────────────────────────
@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<?> access(Exception e) {
        return ResponseEntity.status(403).body(ApiResponse.err("Access denied: "+e.getMessage()));
    }
    @ExceptionHandler(ComplianceService.CountryNotSupportedException.class)
    public ResponseEntity<?> country(Exception e) {
        return ResponseEntity.badRequest().body(ApiResponse.err(e.getMessage()));
    }
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> runtime(RuntimeException e) {
        return ResponseEntity.status(500).body(ApiResponse.err(e.getMessage()));
    }
}
