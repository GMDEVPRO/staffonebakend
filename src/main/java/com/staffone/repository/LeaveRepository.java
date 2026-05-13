package com.staffone.repository;
import com.staffone.entity.Leave;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface LeaveRepository extends JpaRepository<Leave,UUID> {
    Page<Leave> findByTenantIdAndEmployeeId(UUID tid, UUID eid, Pageable p);
    List<Leave> findByTenantIdAndStatus(UUID tid, Leave.LeaveStatus status);
}
