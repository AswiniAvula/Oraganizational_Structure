package com.organizationStructure.services;

import java.util.*;
public interface SalaryInterface
{

    /*
    interface for salary related checks such as whether the salary is too high or low based on the role
    To check if the salary of employees is within a range
    returns string list of salary related issues
    */
    List<String> checkSalaries();
}
