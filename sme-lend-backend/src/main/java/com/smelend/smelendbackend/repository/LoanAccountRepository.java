package com.smelend.smelendbackend.repository;
 
import com.smelend.smelendbackend.entity.LoanAccount;
import com.smelend.smelendbackend.entity.enums.LoanAccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
 
import java.util.Collection;
import java.util.List;
import java.util.Optional;
 
public interface LoanAccountRepository extends JpaRepository<LoanAccount, Long> {
    Optional<LoanAccount> findByApplication_ApplicationId(Long applicationId);
 
    List<LoanAccount> findByStatus(LoanAccountStatus status);

    List<LoanAccount> findByStatusIn(Collection<LoanAccountStatus> statuses);
 
    Optional<LoanAccount> findByAccountNumber(String accountNumber);
}