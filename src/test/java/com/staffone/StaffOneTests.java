package com.staffone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.staffone.compliance.*;
import com.staffone.entity.*;
import com.staffone.repository.*;
import com.staffone.security.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// ════════════════════════════════════════════════════════════════
// 1. PayrollCalculator — Unit Tests
// ════════════════════════════════════════════════════════════════
@SpringBootTest
class PayrollCalculatorTest {
    @Autowired PayrollCalculator calc;
    @Autowired ComplianceService cs;

    @Test @DisplayName("SN: net < gross")
    void sn_netLtGross() {
        var r = calc.calculate(BigDecimal.valueOf(300_000), cs.get("SN"));
        assertThat(r.netSalary()).isLessThan(r.grossSalary());
    }

    @Test @DisplayName("SN: CNSS = 5.58% de 100k")
    void sn_cnss() {
        var r = calc.calculate(BigDecimal.valueOf(100_000), cs.get("SN"));
        assertThat(r.cnssEmployee()).isEqualByComparingTo(BigDecimal.valueOf(5580));
    }

    @Test @DisplayName("CI: CNPS = 3.36% de 100k")
    void ci_cnps() {
        var r = calc.calculate(BigDecimal.valueOf(100_000), cs.get("CI"));
        assertThat(r.cnssEmployee()).isEqualByComparingTo(BigDecimal.valueOf(3360));
    }

    @Test @DisplayName("CM (CEMAC): CNPS = 4.2% de 100k")
    void cm_cnps() {
        var r = calc.calculate(BigDecimal.valueOf(100_000), cs.get("CM"));
        assertThat(r.cnssEmployee()).isEqualByComparingTo(BigDecimal.valueOf(4200));
    }

    @Test @DisplayName("SN: impôt = 0 sous seuil (50k/mois)")
    void sn_noTaxBelowThreshold() {
        var r = calc.calculate(BigDecimal.valueOf(50_000), cs.get("SN"));
        assertThat(r.incomeTax()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test @DisplayName("SN: impôt > 0 salaire élevé (500k/mois)")
    void sn_taxHighSalary() {
        var r = calc.calculate(BigDecimal.valueOf(500_000), cs.get("SN"));
        assertThat(r.incomeTax()).isGreaterThan(BigDecimal.ZERO);
    }

    @Test @DisplayName("Équation: net = gross - cnss - tax")
    void netEquation() {
        var r = calc.calculate(BigDecimal.valueOf(300_000), cs.get("SN"));
        var expected = r.grossSalary().subtract(r.cnssEmployee()).subtract(r.incomeTax())
            .setScale(2, java.math.RoundingMode.HALF_UP);
        assertThat(r.netSalary()).isEqualByComparingTo(expected);
    }

    @Test @DisplayName("CM: currency = XAF")
    void cm_currency() {
        assertThat(cs.get("CM").getCurrency()).isEqualTo("XAF");
    }
}

// ════════════════════════════════════════════════════════════════
// 2. ComplianceService — Unit Tests
// ════════════════════════════════════════════════════════════════
@SpringBootTest
class ComplianceServiceTest {
    @Autowired ComplianceService cs;

    @Test @DisplayName("SN supporté (majuscule et minuscule)")
    void sn_supported() {
        assertThat(cs.supports("SN")).isTrue();
        assertThat(cs.supports("sn")).isTrue();
    }

    @Test @DisplayName("CI supporté")
    void ci_supported() { assertThat(cs.supports("CI")).isTrue(); }

    @Test @DisplayName("CM (CEMAC) supporté")
    void cm_supported() { assertThat(cs.supports("CM")).isTrue(); }

    @Test @DisplayName("ZZ lève CountryNotSupportedException")
    void zz_throws() {
        assertThatThrownBy(() -> cs.get("ZZ"))
            .isInstanceOf(ComplianceService.CountryNotSupportedException.class)
            .hasMessageContaining("ZZ");
    }

    @Test @DisplayName("SN: congés annuels = 30j")
    void sn_leave30() { assertThat(cs.get("SN").getLeave().getAnnualDays()).isEqualTo(30); }

    @Test @DisplayName("CI: congés annuels = 26j")
    void ci_leave26() { assertThat(cs.get("CI").getLeave().getAnnualDays()).isEqualTo(26); }

    @Test @DisplayName("CM: congés annuels = 24j")
    void cm_leave24() { assertThat(cs.get("CM").getLeave().getAnnualDays()).isEqualTo(24); }

    @Test @DisplayName("SN: SMIG > 0")
    void sn_smig() { assertThat(cs.get("SN").getPayroll().getSmig()).isGreaterThan(BigDecimal.ZERO); }

    @Test @DisplayName("supported() retourne ≥ 3 pays")
    void supported_size() { assertThat(cs.supported()).hasSizeGreaterThanOrEqualTo(3); }
}

// ════════════════════════════════════════════════════════════════
// 3. Employee API — Integration Tests
// ════════════════════════════════════════════════════════════════
@SpringBootTest @AutoConfigureMockMvc @Transactional
class EmployeeControllerTest {
    @Autowired MockMvc mvc;
    @Autowired TenantRepository   tenants;
    @Autowired EmployeeRepository employees;
    @Autowired JwtService         jwt;
    ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    Tenant tenant; Employee admin; String token;

    @BeforeEach
    void setup() {
        tenant = tenants.save(Tenant.builder()
            .name("Test School").slug("test-school-"+UUID.randomUUID())
            .countryCode("SN").plan(Tenant.TenantPlan.PRO)
            .mode(Tenant.TenantMode.RH_ONLY).status(Tenant.TenantStatus.ACTIVE)
            .activeModules(Set.of("employees","payroll","leaves")).build());
        admin = employees.save(Employee.builder().tenant(tenant)
            .firstName("Admin").lastName("Test").email("admin@test.sn")
            .role(Employee.EmployeeRole.ADMIN).hireDate(LocalDate.now().minusYears(1))
            .status(Employee.EmployeeStatus.ACTIVE).build());
        token = jwt.generate(new StaffOnePrincipal(
            admin.getId(), tenant.getId(), admin.getEmail(), "ADMIN", "RH_ONLY"));
    }

    @Test @DisplayName("POST /employees — 201 created")
    void create_201() throws Exception {
        mvc.perform(post("/api/v1/tenants/{id}/employees", tenant.getId())
                .header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "firstName","Aminata","lastName","Diallo",
                    "email","a.diallo@test.sn","role","TEACHER","hireDate","2024-09-01"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.firstName").value("Aminata"))
            .andExpect(jsonPath("$.data.role").value("TEACHER"));
    }

    @Test @DisplayName("POST /employees — email dupliqué 409")
    void create_duplicate_409() throws Exception {
        employees.save(Employee.builder().tenant(tenant).firstName("X").lastName("Y")
            .email("dup@test.sn").role(Employee.EmployeeRole.STAFF)
            .hireDate(LocalDate.now()).status(Employee.EmployeeStatus.ACTIVE).build());
        mvc.perform(post("/api/v1/tenants/{id}/employees", tenant.getId())
                .header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "firstName","Z","lastName","W","email","dup@test.sn","hireDate","2024-01-01"))))
            .andExpect(status().isConflict());
    }

    @Test @DisplayName("GET /employees — liste paginée 200")
    void list_200() throws Exception {
        for (int i=0;i<3;i++)
            employees.save(Employee.builder().tenant(tenant)
                .firstName("Emp"+i).lastName("Test").email("emp"+i+"@test.sn")
                .role(Employee.EmployeeRole.STAFF).hireDate(LocalDate.now())
                .status(Employee.EmployeeStatus.ACTIVE).build());
        mvc.perform(get("/api/v1/tenants/{id}/employees", tenant.getId())
                .header("Authorization","Bearer "+token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content").isArray())
            .andExpect(jsonPath("$.data.totalElements", greaterThanOrEqualTo(3)));
    }

    @Test @DisplayName("GET /employees — sans token 401")
    void list_noToken_401() throws Exception {
        mvc.perform(get("/api/v1/tenants/{id}/employees", tenant.getId()))
            .andExpect(status().isUnauthorized());
    }

    @Test @DisplayName("GET /employees/{id} — 200 ok")
    void get_200() throws Exception {
        Employee e = employees.save(Employee.builder().tenant(tenant)
            .firstName("Fatou").lastName("Sow").email("f.sow@test.sn")
            .role(Employee.EmployeeRole.STAFF).hireDate(LocalDate.now())
            .status(Employee.EmployeeStatus.ACTIVE).build());
        mvc.perform(get("/api/v1/tenants/{tid}/employees/{eid}", tenant.getId(), e.getId())
                .header("Authorization","Bearer "+token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.firstName").value("Fatou"));
    }

    @Test @DisplayName("GET /employees/{id} — inexistant 404")
    void get_404() throws Exception {
        mvc.perform(get("/api/v1/tenants/{tid}/employees/{eid}", tenant.getId(), UUID.randomUUID())
                .header("Authorization","Bearer "+token))
            .andExpect(status().isNotFound());
    }

    @Test @DisplayName("DELETE /employees/{id} — soft delete INACTIVE")
    void deactivate_200() throws Exception {
        Employee e = employees.save(Employee.builder().tenant(tenant)
            .firstName("Del").lastName("Me").email("del@test.sn")
            .role(Employee.EmployeeRole.STAFF).hireDate(LocalDate.now())
            .status(Employee.EmployeeStatus.ACTIVE).build());
        mvc.perform(delete("/api/v1/tenants/{tid}/employees/{eid}", tenant.getId(), e.getId())
                .header("Authorization","Bearer "+token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message", containsString("deactivated")));
        assertThat(employees.findById(e.getId()).orElseThrow().getStatus())
            .isEqualTo(Employee.EmployeeStatus.INACTIVE);
    }
}

// ════════════════════════════════════════════════════════════════
// 4. Payroll API — Integration Tests
// ════════════════════════════════════════════════════════════════
@SpringBootTest @AutoConfigureMockMvc @Transactional
class PayrollControllerTest {
    @Autowired MockMvc mvc;
    @Autowired TenantRepository   tenants;
    @Autowired EmployeeRepository employees;
    @Autowired JwtService         jwt;
    ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    Tenant tenant; Employee emp; String token;

    @BeforeEach
    void setup() {
        tenant = tenants.save(Tenant.builder()
            .name("Payroll School").slug("payroll-school-"+UUID.randomUUID())
            .countryCode("SN").plan(Tenant.TenantPlan.PRO)
            .mode(Tenant.TenantMode.RH_ONLY).status(Tenant.TenantStatus.ACTIVE)
            .activeModules(Set.of("employees","payroll")).build());
        emp = employees.save(Employee.builder().tenant(tenant)
            .firstName("Ibou").lastName("Kane").email("ibou@payroll.sn")
            .role(Employee.EmployeeRole.ADMIN).hireDate(LocalDate.now().minusYears(2))
            .status(Employee.EmployeeStatus.ACTIVE).build());
        token = jwt.generate(new StaffOnePrincipal(
            emp.getId(), tenant.getId(), emp.getEmail(), "ADMIN", "RH_ONLY"));
    }

    @Test @DisplayName("GET /payroll/countries — contient SN, CI, CM")
    void countries() throws Exception {
        mvc.perform(get("/api/v1/payroll/countries").header("Authorization","Bearer "+token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasItems("SN","CI","CM")));
    }

    @Test @DisplayName("POST /payroll/SN — bulletin 201 net < gross")
    void generate_sn_201() throws Exception {
        mvc.perform(post("/api/v1/payroll/SN").header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "employeeId",emp.getId(),"year",2026,"month",4,"overrideGross",300000))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.grossSalary").value(300000))
            .andExpect(jsonPath("$.data.netSalary", lessThan(300000.0)))
            .andExpect(jsonPath("$.data.currency").value("XOF"));
    }

    @Test @DisplayName("POST /payroll/CM — currency XAF")
    void generate_cm_xaf() throws Exception {
        mvc.perform(post("/api/v1/payroll/CM").header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "employeeId",emp.getId(),"year",2026,"month",5,"overrideGross",180000))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.currency").value("XAF"));
    }

    @Test @DisplayName("POST /payroll/ZZ — 400 pays invalide")
    void generate_zz_400() throws Exception {
        mvc.perform(post("/api/v1/payroll/ZZ").header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "employeeId",emp.getId(),"year",2026,"month",4))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("ZZ")));
    }

    @Test @DisplayName("POST /payroll/SN — doublon 409")
    void generate_duplicate_409() throws Exception {
        String body = mapper.writeValueAsString(Map.of(
            "employeeId",emp.getId(),"year",2026,"month",6,"overrideGross",300000));
        mvc.perform(post("/api/v1/payroll/SN").header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/payroll/SN").header("Authorization","Bearer "+token)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isConflict());
    }
}

// ════════════════════════════════════════════════════════════════
// 5. Leave API — Integration Tests
// ════════════════════════════════════════════════════════════════
@SpringBootTest @AutoConfigureMockMvc @Transactional
class LeaveControllerTest {
    @Autowired MockMvc mvc;
    @Autowired TenantRepository   tenants;
    @Autowired EmployeeRepository employees;
    @Autowired JwtService         jwt;
    ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    Tenant tenant; Employee manager; Employee staff; String mgrToken;

    @BeforeEach
    void setup() {
        tenant = tenants.save(Tenant.builder()
            .name("Leave School").slug("leave-school-"+UUID.randomUUID())
            .countryCode("CI").plan(Tenant.TenantPlan.PRO)
            .mode(Tenant.TenantMode.RH_ONLY).status(Tenant.TenantStatus.ACTIVE)
            .activeModules(Set.of("employees","leaves")).build());
        manager = employees.save(Employee.builder().tenant(tenant)
            .firstName("Mgr").lastName("Test").email("mgr@leave.ci")
            .role(Employee.EmployeeRole.MANAGER).hireDate(LocalDate.now().minusYears(2))
            .status(Employee.EmployeeStatus.ACTIVE).build());
        staff = employees.save(Employee.builder().tenant(tenant)
            .firstName("Staff").lastName("Test").email("staff@leave.ci")
            .role(Employee.EmployeeRole.STAFF).hireDate(LocalDate.now().minusYears(1))
            .status(Employee.EmployeeStatus.ACTIVE).build());
        mgrToken = jwt.generate(new StaffOnePrincipal(
            manager.getId(), tenant.getId(), manager.getEmail(), "MANAGER", "RH_ONLY"));
    }

    @Test @DisplayName("POST /leaves — PENDING créé 201")
    void submit_201() throws Exception {
        mvc.perform(post("/api/v1/leaves").header("Authorization","Bearer "+mgrToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "employeeId",staff.getId(),"leaveType","ANNUAL",
                    "startDate","2026-07-01","endDate","2026-07-07","reason","Vacances"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andExpect(jsonPath("$.data.daysCount").value(5));
    }

    @Test @DisplayName("POST /leaves — dates inversées 400")
    void submit_badDates_400() throws Exception {
        mvc.perform(post("/api/v1/leaves").header("Authorization","Bearer "+mgrToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "employeeId",staff.getId(),"leaveType","ANNUAL",
                    "startDate","2026-07-10","endDate","2026-07-05"))))
            .andExpect(status().isBadRequest());
    }

    @Test @DisplayName("GET /leaves/pending — liste non vide après soumission")
    void pending_notEmpty() throws Exception {
        mvc.perform(post("/api/v1/leaves").header("Authorization","Bearer "+mgrToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of(
                    "employeeId",staff.getId(),"leaveType","SICK",
                    "startDate","2026-08-01","endDate","2026-08-03"))))
            .andExpect(status().isCreated());
        mvc.perform(get("/api/v1/leaves/pending").header("Authorization","Bearer "+mgrToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }
}

// ════════════════════════════════════════════════════════════════
// 6. Multi-tenant Isolation Test
// ════════════════════════════════════════════════════════════════
@SpringBootTest @AutoConfigureMockMvc @Transactional
class MultiTenantIsolationTest {
    @Autowired MockMvc mvc;
    @Autowired TenantRepository   tenants;
    @Autowired EmployeeRepository employees;
    @Autowired JwtService         jwt;

    @Test @DisplayName("cross-tenant: tenant A não acessa dados do tenant B")
    void crossTenantBlocked() throws Exception {
        Tenant tA = tenants.save(Tenant.builder()
            .name("School A").slug("school-a-"+UUID.randomUUID())
            .countryCode("SN").plan(Tenant.TenantPlan.PRO)
            .mode(Tenant.TenantMode.RH_ONLY).status(Tenant.TenantStatus.ACTIVE)
            .activeModules(Set.of("employees")).build());
        Tenant tB = tenants.save(Tenant.builder()
            .name("School B").slug("school-b-"+UUID.randomUUID())
            .countryCode("CI").plan(Tenant.TenantPlan.PRO)
            .mode(Tenant.TenantMode.RH_ONLY).status(Tenant.TenantStatus.ACTIVE)
            .activeModules(Set.of("employees")).build());
        Employee adminA = employees.save(Employee.builder().tenant(tA)
            .firstName("Admin").lastName("A").email("admin@a.sn")
            .role(Employee.EmployeeRole.ADMIN).hireDate(LocalDate.now())
            .status(Employee.EmployeeStatus.ACTIVE).build());
        String tokenA = jwt.generate(new StaffOnePrincipal(
            adminA.getId(), tA.getId(), adminA.getEmail(), "ADMIN", "RH_ONLY"));

        // Tenant A tenta acessar Tenant B — deve ser 403
        mvc.perform(get("/api/v1/tenants/{id}/employees", tB.getId())
                .header("Authorization","Bearer "+tokenA))
            .andExpect(result ->
                assertThat(result.getResponse().getStatus()).isIn(403, 401));
    }
}
