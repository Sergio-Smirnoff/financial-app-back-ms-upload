package com.financialapp.upload.application.importrun.impl;

import com.financialapp.upload.domain.common.model.UserId;
import com.financialapp.upload.domain.model.importrun.ImportRun;
import com.financialapp.upload.domain.repository.ImportRunRepository;
import com.financialapp.upload.domain.usecase.importrun.ListImportRuns;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListImportRunsUseCaseImpl implements ListImportRuns {

    private final ImportRunRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<ImportRun> execute(UserId userId) {
        return repository.findByUser(userId);
    }
}
