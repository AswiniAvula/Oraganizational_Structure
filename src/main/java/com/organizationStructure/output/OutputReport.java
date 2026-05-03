package com.organizationStructure.output;

import java.util.List;

public class OutputReport {


    // To validate CSV Data import related issues
    public void DataIssues(List<String> dataIssues) {
        if (dataIssues == null || dataIssues.isEmpty())
        {
            return;
        }
        System.out.println("=== Data Issues ===");
        dataIssues.forEach(System.out::println);
    }

    // To find Salary related issues,whether high or low
    public void SalaryIssues(List<String> salaryIssues)
    {
        System.out.println("=== Salary Issues ===");
        if (salaryIssues.isEmpty())
        {
            System.out.println("No salary issues found.");
        }
        else
        {
            salaryIssues.forEach(System.out::println);
        }
    }


    //To find Reporting line related issues such as circular reporting or missing manager
    public void ReportingLineIssues(List<String> reportingIssues) {
        System.out.println("=== Reporting Line Issues ===");
        if (reportingIssues.isEmpty())
        {
            System.out.println("No reporting line issues found.");
        }
        else {
            reportingIssues.forEach(System.out::println);
        }
    }
}
