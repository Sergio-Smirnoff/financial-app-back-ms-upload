package com.financialapp.upload.repository;

import com.financialapp.upload.model.entity.UploadSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UploadSessionRepository extends JpaRepository<UploadSession, String> {
}
