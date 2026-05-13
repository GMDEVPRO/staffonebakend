package com.staffone.repository;
import com.staffone.entity.Timesheet;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.*;

@Repository
public interface TimesheetRepository extends JpaRepository<Timesheet,UUID> {
    @Query("SELECT t FROM Timesheet t WHERE t.tenant.id=:tid AND t.employee.id=:eid AND t.date BETWEEN :f AND :t2 ORDER BY t.date DESC")
    List<Timesheet> findByEmployeeAndPeriod(@Param("tid") UUID tid, @Param("eid") UUID eid,
                                            @Param("f") LocalDate f, @Param("t2") LocalDate t2);
    boolean existsByTenantIdAndEmployeeIdAndDate(UUID tid, UUID eid, LocalDate date);
    @Query("SELECT COUNT(t) FROM Timesheet t WHERE t.tenant.id=:tid AND t.date=:d AND t.checkIn IS NOT NULL")
    long countPresentToday(@Param("tid") UUID tid, @Param("d") LocalDate d);
}
