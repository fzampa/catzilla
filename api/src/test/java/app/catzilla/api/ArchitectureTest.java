package app.catzilla.api;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Feature packages (app.catzilla.api.&lt;feature&gt;) must not depend on each other in cycles. */
@AnalyzeClasses(packages = "app.catzilla.api", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule featuresAreFreeOfCycles = slices().matching("app.catzilla.api.(*)..")
            .should()
            .beFreeOfCycles()
            .allowEmptyShould(true);
}
