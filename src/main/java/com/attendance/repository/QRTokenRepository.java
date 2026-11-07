package com.attendance.repository;

import com.attendance.entity.QRToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for QRToken entity operations.
 */
@Repository
public interface QRTokenRepository extends JpaRepository<QRToken, Long> {

    Optional<QRToken> findByTokenHash(String tokenHash);

    List<QRToken> findBySessionSessionIdAndTokenStatus(Long sessionId, QRToken.TokenStatus tokenStatus);

    List<QRToken> findBySessionSessionId(Long sessionId);

    /**
     * Expires all active tokens for a given session by setting their status to EXPIRED.
     *
     * @param sessionId the session ID whose tokens should be expired
     * @return the number of tokens updated
     */
    @Modifying
    @Query("UPDATE QRToken q SET q.tokenStatus = 'EXPIRED' WHERE q.session.sessionId = :sessionId AND q.tokenStatus = 'ACTIVE'")
    int expireTokensBySessionId(@Param("sessionId") Long sessionId);

    /**
     * Find the currently active token for a given session.
     *
     * @param sessionId the session ID
     * @return the active QRToken, if any
     */
    @Query("SELECT q FROM QRToken q WHERE q.session.sessionId = :sessionId AND q.tokenStatus = 'ACTIVE' ORDER BY q.createdAt DESC")
    Optional<QRToken> findActiveBySessionId(@Param("sessionId") Long sessionId);
}
