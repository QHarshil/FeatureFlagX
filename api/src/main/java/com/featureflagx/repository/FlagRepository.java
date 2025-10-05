package com.featureflagx.repository;

import com.featureflagx.model.Flag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Flag entities.
 */
@Repository
public interface FlagRepository extends JpaRepository<Flag, String> {

    /**
     * Find all enabled flags.
     */
    List<Flag> findByEnabledTrue();

    /**
     * Find all disabled flags.
     */
    List<Flag> findByEnabledFalse();

    /**
     * Find flags by key pattern (case-insensitive).
     */
    List<Flag> findByKeyContainingIgnoreCase(String pattern);

    /**
     * Count enabled flags.
     */
    long countByEnabledTrue();

    /**
     * Count disabled flags.
     */
    long countByEnabledFalse();

    /**
     * Check if a flag exists by key (case-insensitive).
     */
    boolean existsByKeyIgnoreCase(String key);

    /**
     * Find flag by key (case-insensitive).
     */
    Optional<Flag> findByKeyIgnoreCase(String key);
}
