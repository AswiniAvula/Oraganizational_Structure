package com.organizationStructure;

import com.organizationStructure.repository.EmployeeRepository;
import com.organizationStructure.services.SalaryImpl;
import com.organizationStructure.services.SalaryInterface;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.*;


public class SalaryAnalyzerTest {

    @Test
    public void managerExactlyAtBoundariesNotReported() throws Exception {
        Path tmp = Files.createTempFile("salary-boundaries", ".csv");
        try {
            // subordinate salary 50000 -> avg 50000 -> min=60000 max=75000
            String csv = ""
                    + "id,firstName,lastName,salary,managerId\n"
                    + "1,Manager,Min,60000,\n"
                    + "2,Sub,A,50000,1\n";
            Files.write(tmp, csv.getBytes());

            EmployeeRepository repo = new EmployeeRepository();
            repo.importcsv(tmp.toString());

            SalaryInterface analyzer = new SalaryImpl(repo);
            List<String> issues = analyzer.checkSalaries();
            assertTrue("Manager at min should not be reported", issues.isEmpty());

            // manager at max
            Path tmp2 = Files.createTempFile("salary-boundaries-2", ".csv");
            String csv2 = ""
                    + "id,firstName,lastName,salary,managerId\n"
                    + "1,Manager,Max,75000,\n"
                    + "2,Sub,A,50000,1\n";
            Files.write(tmp2, csv2.getBytes());

            repo = new EmployeeRepository();
            repo.importcsv(tmp2.toString());
            analyzer = new SalaryImpl(repo);
            issues = analyzer.checkSalaries();
            assertTrue("Manager at max should not be reported", issues.isEmpty());

            Files.deleteIfExists(tmp2);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    public void floatingPointAverageHandledCorrectly() throws Exception {
        Path tmp = Files.createTempFile("salary-fp", ".csv");
        try {
            // three subs with salaries that produce fractional average
            String csv = ""
                    + "id,firstName,lastName,salary,managerId\n"
                    + "1,Manager,FP,60000,\n"
                    + "2,S1,A,33333.33,1\n"
                    + "3,S2,B,33333.33,1\n"
                    + "4,S3,C,33333.34,1\n";
            Files.write(tmp, csv.getBytes());

            EmployeeRepository repo = new EmployeeRepository();
            repo.importcsv(tmp.toString());

            SalaryInterface analyzer = new SalaryImpl(repo);
            List<String> issues = analyzer.checkSalaries();

            // manager 60000 should be > max (~50000) and reported as too high
            assertFalse("Expect an issue due to manager too high", issues.isEmpty());
            assertTrue(issues.get(0).contains("too high"));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    public void managerWithOnlyInvalidSubordinatesIsSkipped() throws Exception {
        Path tmp = Files.createTempFile("salary-invalidsubs", ".csv");
        try {
            // subordinate 3 has salary 0 -> invalid; subordinate 2 valid
            String csv = ""
                    + "id,firstName,lastName,salary,managerId\n"
                    + "1,Manager,Test,60000,\n"
                    + "2,Sub,Valid,40000,1\n"
                    + "3,Sub,Invalid,0,1\n";
            Files.write(tmp, csv.getBytes());

            EmployeeRepository repo = new EmployeeRepository();
            repo.importcsv(tmp.toString());

            // ensure subordinate 3 is invalid
            assertTrue(repo.getInvalidEmployeeIds().contains(3));

            SalaryInterface analyzer = new SalaryImpl(repo);
            List<String> issues = analyzer.checkSalaries();

            // average computed only from subordinate 2 -> manager within range -> no issues
            assertTrue("Manager should not be reported when invalid subordinate excluded", issues.isEmpty());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
