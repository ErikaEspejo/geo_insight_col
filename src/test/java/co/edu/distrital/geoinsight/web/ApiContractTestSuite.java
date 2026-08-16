package co.edu.distrital.geoinsight.web;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("Contratos REST de GeoInsight Colombia")
@SelectClasses({
        AuthWebTest.class,
        LayerWebTest.class,
        AnalysisWebTest.class,
        AdminWebTest.class
})
public class ApiContractTestSuite {
}
