package com.smelend.smelendbackend.repository;
import com.smelend.smelendbackend.entity.Charge;
import com.smelend.smelendbackend.entity.enums.ChargeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ChargeRepository extends JpaRepository<Charge, Long> {
    List<Charge> findByLoanAccount_LoanAccountId(Long loanAccountId);
    List<Charge> findByLoanAccount_LoanAccountIdAndStatus(Long loanAccountId, ChargeStatus status);
}
