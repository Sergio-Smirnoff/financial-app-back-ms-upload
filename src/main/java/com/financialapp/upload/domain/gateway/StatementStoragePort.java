package com.financialapp.upload.domain.gateway;

import java.io.InputStream;
import java.time.Duration;

public interface StatementStoragePort {

    String store(String objectPath, InputStream data, long size, String contentType);

    InputStream retrieve(String objectPath);

    void move(String fromPath, String toPath);

    void deleteOlderThan(String prefix, Duration ageThreshold);
}
