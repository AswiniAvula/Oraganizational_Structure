package com.organizationStructure.services;

import java.util.*;
public interface ReportingHeadLineInterface
{
    /*
    interface to check reporting line related checks such as circular reporting or missing manager
    To check if the reporting line of an employee is valid and does not creating a circular reporting structure
    */
    List<String> checkReportingHeadLines();
}
