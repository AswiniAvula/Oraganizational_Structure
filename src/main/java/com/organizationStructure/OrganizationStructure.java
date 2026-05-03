package com.organizationStructure;

import com.organizationStructure.output.OutputReport;
import com.organizationStructure.repository.EmployeeRepository;
import com.organizationStructure.services.*;
import java.util.List;

public class OrganizationStructure {
    public static void main(String[] args) throws Exception {

        //Passing csv file path as argument
        String csv = args[0];

        //CSV import data validation
        EmployeeRepository repo = new EmployeeRepository();
        repo.importcsv(csv);

        //logic for salary and reportingheads line
        SalaryInterface salary = new SalaryImpl(repo);
        ReportingHeadLineInterface reportingheadLine = new ReportingHeadLineImpl(repo);

        List<String> salaryIssues = salary.checkSalaries();
        List<String> reportingheadIssues = reportingheadLine.checkReportingHeadLines();

        //Output generation
        OutputReport op = new OutputReport();
        op.DataIssues(repo.getErrors());
        op.SalaryIssues(salaryIssues);
        op.ReportingLineIssues(reportingheadIssues);
    }
}
