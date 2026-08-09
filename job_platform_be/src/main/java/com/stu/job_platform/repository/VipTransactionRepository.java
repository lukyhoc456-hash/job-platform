package com.stu.job_platform.repository;

import com.stu.job_platform.entity.VipTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VipTransactionRepository extends JpaRepository<VipTransaction, Integer> {
    Optional<VipTransaction> findByTxnRef(String txnRef);

    List<VipTransaction> findByRecruiterIdAndStatusOrderByPaidAtDesc(Integer recruiterId, String status);
}