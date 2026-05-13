package com.staffone.repository;
import com.staffone.entity.Payroll;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll,UUID> {
    Optional<Payroll> findByTenantIdAndEmployeeIdAndPeriodYearAndPeriodMonth(
        UUID tid, UUID eid, short year, short month);
    @Query("SELECT p FROM Payroll p WHERE p.tenant.id=:tid AND p.employee.id=:eid ORDER BY p.periodYear DESC, p.periodMonth DESC")
    List<Payroll> findHistoryByEmployee(@Param("tid") UUID tid, @Param("eid") UUID eid, Pageable p);
}
