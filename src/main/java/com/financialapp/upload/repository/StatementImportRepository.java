package com.financialapp.upload.repository;

import com.financialapp.upload.model.entity.StatementImport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StatementImportRepository extends JpaRepository<StatementImport, Long> {

    Optional<StatementImport> findByUserIdAndFileHash(Long userId, String fileHash);
}
