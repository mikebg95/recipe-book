package dev.michaelgoldman.recipebookbackend;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;
import jakarta.persistence.Entity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(
        packages = "dev.michaelgoldman.recipebookbackend",
        importOptions = { ImportOption.DoNotIncludeTests.class, ArchitectureTest.DoNotIncludeGeneratedApi.class }
)
public class ArchitectureTest {
    static class DoNotIncludeGeneratedApi implements ImportOption {
        @Override
        public boolean includes(Location location) {
            return !location.contains("/recipebookbackend/api/");
        }
    }

    @ArchTest
    static final ArchRule layered_architecture_is_respected =
            layeredArchitecture().consideringOnlyDependenciesInLayers()
                    .layer("Controller").definedBy("..controller..")
                    .layer("Service").definedBy("..service..")
                    .layer("Repository").definedBy("..repository..")
                    .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
                    .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service");


    @ArchTest
    static final ArchRule transactional_should_only_be_in_service_layer =
            methods().that().areAnnotatedWith(Transactional.class)
                    .should().beDeclaredInClassesThat().resideInAPackage("..service..");

    @ArchTest
    static final ArchRule no_field_injection_allowed =
            noFields().should().beAnnotatedWith(Autowired.class);

    @ArchTest
    static final ArchRule web_layer_should_not_depend_on_entities =
            noClasses().that().resideInAPackage("..controller..")
                    .should().dependOnClassesThat().areAnnotatedWith(Entity.class);

    @ArchTest
    static final ArchRule jpa_only_used_in_entity_and_repository_packages =
            noClasses().that().resideOutsideOfPackages("..repository..", "..entity..")
                    .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..");

    @ArchTest
    static final ArchRule no_package_cycles_allowed =
            slices().matching("dev.michaelgoldman.recipebookbackend.(*)..")
                    .should().beFreeOfCycles();

    @ArchTest
    static final ArchRule no_access_to_standard_streams =
            GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

    @ArchTest
    static final ArchRule no_generic_exceptions_thrown =
            GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;

    @ArchTest
    static final ArchRule domain_exceptions_are_unchecked =
            classes().that().haveSimpleNameEndingWith("Exception")
                    .should().beAssignableTo(RuntimeException.class);

}
