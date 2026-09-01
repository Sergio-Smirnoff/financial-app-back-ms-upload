package com.financialapp.upload.application.importrun.impl;

import com.financialapp.upload.domain.common.model.UserId;
import com.financialapp.upload.domain.model.importrun.ImportRun;
import com.financialapp.upload.domain.model.importrun.ImportRunId;
import com.financialapp.upload.domain.repository.ImportRunRepository;
import com.financialapp.upload.domain.usecase.importrun.GetImportRun;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetImportRunUseCaseImpl implements GetImportRun {

    private final ImportRunRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<ImportRun> execute(UserId userId, ImportRunId id) {
        return repository.findByIdOwnedBy(id, userId);
    }
}
