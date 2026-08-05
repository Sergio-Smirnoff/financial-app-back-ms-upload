package com.financialapp.upload;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "com.financialapp.upload", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    @ArchTest
    public static final ArchRule layeredArchitecture = layeredArchitecture()
            .consideringOnlyDependenciesInAnyPackage(
                    "com.financialapp.upload.domain..",
                    "com.financialapp.upload.application..",
                    "com.financialapp.upload.infrastructure..",
                    "com.financialapp.upload.web..",
                    "com.financialapp.upload.controller..")
            .layer("Domain").definedBy("com.financialapp.upload.domain..")
            .layer("Application").definedBy("com.financialapp.upload.application..")
            .layer("Infrastructure").definedBy("com.financialapp.upload.infrastructure..")
            .layer("Web").definedBy("com.financialapp.upload.web..", "com.financialapp.upload.controller..")

            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure", "Web")
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Web", "Infrastructure")
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer();
}
