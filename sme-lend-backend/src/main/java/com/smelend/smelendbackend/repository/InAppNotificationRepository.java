package com.smelend.smelendbackend.repository;

import com.smelend.smelendbackend.entity.InAppNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface InAppNotificationRepository extends JpaRepository<InAppNotification, Long> {

    List<InAppNotification> findByUser_UserIdOrderByCreatedAtDesc(Long userId);

    List<InAppNotification> findByUser_UserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    long countByUser_UserIdAndIsReadFalse(Long userId);

    @Modifying @Transactional
    @Query("UPDATE InAppNotification n SET n.isRead = true WHERE n.user.userId = :userId")
    void markAllReadForUser(Long userId);

    @Modifying @Transactional
    @Query("UPDATE InAppNotification n SET n.isRead = true WHERE n.notificationId = :id AND n.user.userId = :userId")
    void markOneRead(Long id, Long userId);
}
