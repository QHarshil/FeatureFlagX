package com.featureflagx.repository;

import com.featureflagx.model.Flag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FlagRepository extends JpaRepository<Flag, String> {
    // JpaRepository provides common CRUD operations (save, findById, deleteById, findAll, etc.)
    // Custom query methods can be added here if needed, for example:
    // Optional<Flag> findByKeyAndSomeOtherCriteria(String key, String criteria);
    
    /**
     * Find a flag by its unique key
     * @param key the flag key
     * @return the flag if found, or empty optional if not found
     */
    Optional<Flag> findByKey(String key);
}

