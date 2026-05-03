package com.organizationStructure;

import com.organizationStructure.repository.EmployeeRepository;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Each test writes a small temporary CSV fixture and calls repo.load(...) so
 * the repository performs its full validation pipeline.
 */
public class EmployeeRepositoryTest {

    @Test
    public void emptyFileProducesHeaderError() throws Exception {
        Path tmp = Files.createTempFile("empty", ".csv");
        try {
            // create an empty file (no header)
            Files.write(tmp, new byte[0]);

            EmployeeRepository repo = new EmployeeRepository();
            repo.importcsv(tmp.toString());

            List<String> errors = repo.getErrors();
            assertFalse("Expect at least one load error for empty file", errors.isEmpty());
            assertTrue(errors.stream().anyMatch(s -> s.toLowerCase().contains("empty") || s.toLowerCase().contains("missing header")));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    public void malformedRowIsReported() throws Exception {
        Path tmp = Files.createTempFile("malformed", ".csv");
        try {
            String csv = "id,firstName,lastName,salary,managerId\n"
                    + "1,Joe,Doe,60000,\n"
                    + "BAD_ROW\n"; // malformed
            Files.write(tmp, csv.getBytes());

            EmployeeRepository repo = new EmployeeRepository();
            repo.importcsv(tmp.toString());

            assertTrue(repo.getErrors().stream().anyMatch(s -> s.contains("missing required fields") || s.contains("invalid number format")));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    public void duplicateIdsMarkedInvalid() throws Exception {
        Path tmp = Files.createTempFile("duplicate", ".csv");
        try {
            String csv = "id,firstName,lastName,salary,managerId\n"
                    + "1,A,One,100000,\n"
                    + "1,B,Two,90000,1\n";
            Files.write(tmp, csv.getBytes());

            EmployeeRepository repo = new EmployeeRepository();
            repo.importcsv(tmp.toString());

            // duplicate id should be reported and that id marked invalid
            assertTrue(repo.getErrors().stream().anyMatch(s -> s.contains("Duplicate employee id")));
            assertTrue(repo.getInvalidEmployeeIds().contains(1));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    public void zeroOrNegativeSalaryMarkedInvalid() throws Exception {
        Path tmp = Files.createTempFile("bad-salary", ".csv");
        try {
            String csv = "id,firstName,lastName,salary,managerId\n"
                    + "1,Good,CEO,200000,\n"
                    + "2,Bad,Zero,0,1\n"
                    + "3,Bad,Neg,-100,1\n";
            Files.write(tmp, csv.getBytes());

            EmployeeRepository repo = new EmployeeRepository();
            repo.importcsv(tmp.toString());

            // both 2 and 3 should be invalid due to salary <= 0
            Set<Integer> invalid = repo.getInvalidEmployeeIds();
            assertTrue(invalid.contains(2));
            assertTrue(invalid.contains(3));
            assertTrue(repo.getErrors().stream().anyMatch(s -> s.contains("invalid salary")));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    public void missingManagerIsReportedAndMarkedInvalid() throws Exception {
        Path tmp = Files.createTempFile("missing-manager", ".csv");
        try {
            String csv = "id,firstName,lastName,salary,managerId\n"
                    + "1,CEO,Top,200000,\n"
                    + "2,Orphan,User,50000,999\n";
            Files.write(tmp, csv.getBytes());

            EmployeeRepository repo = new EmployeeRepository();
            repo.importcsv(tmp.toString());

            assertTrue(repo.getErrors().stream().anyMatch(s -> s.contains("references managerId")));
            assertTrue(repo.getInvalidEmployeeIds().contains(2));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    public void multipleRootsChooseSingleCeoAndCascadeInvalidation() throws Exception {
        Path tmp = Files.createTempFile("multiple-roots", ".csv");
        try {
            String csv = ""
                    + "id,firstName,lastName,salary,managerId\n"
                    + "1,CEO,Top,200000,\n"        // chosen CEO (highest salary)
                    + "2,Err,Root,50000,\n"        // mistaken root -> should be invalidated
                    + "3,Child,OfErr,40000,2\n"    // child of mistaken root -> invalidated by cascade
                    + "4,Good,Worker,45000,1\n";
            Files.write(tmp, csv.getBytes());

            EmployeeRepository repo = new EmployeeRepository();
            repo.importcsv(tmp.toString());

            List<String> errors = repo.getErrors();
            assertTrue("Expect message about extra root", errors.stream().anyMatch(s -> s.contains("has missing managerId but is not the CEO")));
            Set<Integer> invalid = repo.getInvalidEmployeeIds();
            assertTrue("Mistaken root should be invalid", invalid.contains(2));
            assertTrue("Child of mistaken root should be invalid due to cascade", invalid.contains(3));
            assertTrue("Real CEO should remain valid", repo.isValidForAnalysis(1));
            assertTrue("Subordinate of real CEO should remain valid", repo.isValidForAnalysis(4));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    public void circularReportingDetectedAndInvalidated() throws Exception {
        Path tmp = Files.createTempFile("circular", ".csv");
        try {
            String csv = ""
                    + "id,firstName,lastName,salary,managerId\n"
                    + "1,A,One,100000,3\n"
                    + "2,B,Two,90000,1\n"
                    + "3,C,Three,80000,2\n";
            Files.write(tmp, csv.getBytes());

            EmployeeRepository repo = new EmployeeRepository();
            repo.importcsv(tmp.toString());

            assertTrue(repo.getErrors().stream().anyMatch(s -> s.contains("Circular reporting detected")));
            Set<Integer> invalid = repo.getInvalidEmployeeIds();
            assertTrue(invalid.contains(1));
            assertTrue(invalid.contains(2));
            assertTrue(invalid.contains(3));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
