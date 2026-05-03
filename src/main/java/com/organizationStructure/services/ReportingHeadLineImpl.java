package com.organizationStructure.services;

import com.organizationStructure.model.Employee;
import com.organizationStructure.repository.EmployeeRepository;
import java.util.*;
import java.util.stream.Collectors;

public class ReportingHeadLineImpl implements ReportingHeadLineInterface
{
    private final EmployeeRepository repo;

    public ReportingHeadLineImpl(EmployeeRepository repo)
    {
        this.repo=repo;
    }

    //logic for calculating reporting line of employees by checking the depth of the reporting structure and make sure it does not exceed 4 levels
    private int depthcheck(Employee e) {
        int depth = 0;
        Employee current = e;
        while (current != null && current.getManagerId() != null)
        {
            depth++;
            current = repo.get(current.getManagerId());
        }
        return depth;
    }


    //chceking reportinghead line
    @Override
    public List<String> checkReportingHeadLines()
    {
        return repo.getAll().stream()
                .map(e -> {
                    int deep = depthcheck(e);
                    if (deep > 4) {
                        return e + " has reporting line long by " + (deep - 4);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
