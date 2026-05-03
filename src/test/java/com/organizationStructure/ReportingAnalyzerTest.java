package com.organizationStructure;

import com.organizationStructure.repository.EmployeeRepository;
import com.organizationStructure.services.ReportingHeadLineImpl;
import com.organizationStructure.services.ReportingHeadLineInterface;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.*;


public class ReportingAnalyzerTest {

    @Test
    public void leafWithDepthGreaterThanFourIsReported() throws Exception {
        Path tmp = Files.createTempFile("report-depth", ".csv");
        try {
            // chain length: 1->2->3->4->5->6 (depth for 6 = 5)
            String csv = ""
                    + "id,firstName,lastName,salary,managerId\n"
                    + "1,CEO,Top,200000,\n"
                    + "2,M1,A,150000,1\n"
                    + "3,M2,B,120000,2\n"
                    + "4,M3,C,100000,3\n"
                    + "5,M4,D,90000,4\n"
                    + "6,Worker,E,50000,5\n";
            Files.write(tmp, csv.getBytes());

            EmployeeRepository repo = new EmployeeRepository();
            repo.importcsv(tmp.toString());

            ReportingHeadLineInterface analyzer = new ReportingHeadLineImpl(repo);
            List<String> issues = analyzer.checkReportingHeadLines();

            assertFalse("Worker with depth 5 should be reported", issues.isEmpty());
            assertTrue(issues.stream().anyMatch(s -> s.contains("Worker E")));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    public void depthStopsWhenEncounteringInvalidManager() throws Exception {
        Path tmp = Files.createTempFile("depth-invalid-manager", ".csv");
        try {
            // two roots: 1 (CEO) and 2 (invalid root) -> 2 and its child 3 invalidated
            String csv = ""
                    + "id,firstName,lastName,salary,managerId\n"
                    + "1,CEO,Top,200000,\n"
                    + "2,BadRoot,NoMgr,50000,\n"
                    + "3,Child,OfBad,40000,2\n";
            Files.write(tmp, csv.getBytes());

            EmployeeRepository repo = new EmployeeRepository();
            repo.importcsv(tmp.toString());

            // 2 and 3 should be invalid
            assertTrue(repo.getInvalidEmployeeIds().contains(2));
            assertTrue(repo.getInvalidEmployeeIds().contains(3));

            ReportingHeadLineInterface analyzer = new ReportingHeadLineImpl(repo);
            List<String> issues = analyzer.checkReportingHeadLines();

            // only valid employees (1) remain; no reporting issues expected
            assertTrue("No reporting issues expected after invalidation", issues.isEmpty());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Test
    public void longChainPerformanceAndCorrectness() throws Exception {
        Path tmp = Files.createTempFile("long-chain", ".csv");
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("id,firstName,lastName,salary,managerId\n");
            final int N = 500; // large but reasonable for unit test
            sb.append("1,CEO,Top,200000,\n");
            for (int i = 2; i <= N; i++) {
                sb.append(i).append(",E").append(i).append(",L").append(i).append(",50000,").append(i - 1).append("\n");
            }
            Files.write(tmp, sb.toString().getBytes());

            EmployeeRepository repo = new EmployeeRepository();
            repo.importcsv(tmp.toString());

            ReportingHeadLineInterface analyzer = new ReportingHeadLineImpl(repo);
            List<String> issues = analyzer.checkReportingHeadLines();

            // many employees will have depth > 4; ensure method completes and returns non-empty list
            assertFalse("Long chain should produce reporting issues and complete quickly", issues.isEmpty());
        } finally {
            Files.deleteIfExists(tmp);
        }
    }
}
