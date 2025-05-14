package com.featureflagx.repository;

import com.featureflagx.model.Flag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FlagRepository extends JpaRepository<Flag, String> {
    // JpaRepository provides common CRUD operations (save, findById, deleteById, findAll, etc.)
    // Custom query methods can be added here if needed, for example:
    // Optional<Flag> findByKeyAndSomeOtherCriteria(String key, String criteria);
}

