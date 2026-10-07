package com.umc.puppymode2.domain.complaint.repository;

import com.umc.puppymode2.domain.complaint.entity.UserComplaint;
import com.umc.puppymode2.domain.complaint.entity.enums.ComplaintReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserComplaintRepository extends JpaRepository<UserComplaint, Long> {

    boolean existsByReporterIdAndTargetUserIdAndReason(Long reporterId, Long targetUserId, ComplaintReason reason);
}
