package com.staffone.repository;
import com.staffone.entity.Employee;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.*;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee,UUID> {
    Page<Employee> findByTenantId(UUID tenantId, Pageable p);
    Optional<Employee> findByIdAndTenantId(UUID id, UUID tenantId);
    Optional<Employee> findByEmailAndTenantId(String email, UUID tenantId);
    boolean existsByEmailAndTenantId(String email, UUID tenantId);
    Optional<Employee> findByEdukiraTeacherIdAndTenantId(String edukiraTeacherId, UUID tenantId);
    List<Employee> findByTenantIdAndEdukiraTeacherIdIsNotNull(UUID tenantId);

    @Query("""
        SELECT e FROM Employee e
        WHERE e.tenant.id=:tid AND e.status='ACTIVE'
          AND (:s IS NULL
               OR LOWER(e.firstName) LIKE LOWER(CONCAT('%',:s,'%'))
               OR LOWER(e.lastName)  LIKE LOWER(CONCAT('%',:s,'%'))
               OR LOWER(e.email)     LIKE LOWER(CONCAT('%',:s,'%')))
        """)
    Page<Employee> searchByTenant(@Param("tid") UUID tid, @Param("s") String s, Pageable p);
}
