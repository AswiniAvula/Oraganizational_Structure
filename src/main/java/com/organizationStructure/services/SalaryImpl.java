package com.organizationStructure.services;

import com.organizationStructure.model.Employee;
import com.organizationStructure.repository.EmployeeRepository;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SalaryImpl implements SalaryInterface {

    private final EmployeeRepository repo;
    private static final double EPS = 1e-6;

    public SalaryImpl(EmployeeRepository repo)
    {
        this.repo = repo;
    }

    //logic for calculating salaries of managers by comparing with avg salary of suboordinates
    @Override
    public List<String> checkSalaries() {
        return repo.getAll().stream()
                .filter(manager -> repo.isValidForAnalysis(manager.getId()))
                .flatMap(manager ->
                {
                    List<Employee> subs = repo.getSubordinates(manager.getId()).stream()
                            .filter(s -> repo.isValidForAnalysis(s.getId()))
                            .collect(Collectors.toList());
                    if (subs.isEmpty()) {
                        return Stream.empty();
                    }

                    //double avg = subs.stream().mapToDouble(Employee::getSalary).average().orElse(0.0);
                   double sum = 0.0;
                    int count = 0;
                    for (Employee e : subs) {
                        sum =sum + e.getSalary();
                        count++;
                    }
                    double avg = (count == 0) ? 0.0 : sum / count;

                    double min = avg * 1.2;
                    double max = avg * 1.5;

                    double mgrSalary = manager.getSalary();

                    if (mgrSalary + EPS < min) {
                        String msg = String.format("%s earns too low by %.2f", manager, (min - mgrSalary));
                        return Stream.of(msg);
                    } else if (mgrSalary - EPS > max) {
                        String msg = String.format("%s earns too high by %.2f", manager, (mgrSalary - max));
                        return Stream.of(msg);
                    } else {
                        return Stream.empty();
                    }
                })
                .collect(Collectors.toList());
    }
}
