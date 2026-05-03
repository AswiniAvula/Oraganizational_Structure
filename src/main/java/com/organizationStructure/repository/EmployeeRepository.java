package com.organizationStructure.repository;

import com.organizationStructure.model.Employee;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class EmployeeRepository {
    private final Map<Integer, Employee> emp = new LinkedHashMap<>();
    private final List<String> Errors = new ArrayList<>();
    private final Set<Integer> invalidEmpIds = new HashSet<>();



    public void importcsv(String filePath) throws IOException {
        emp.clear();
        Errors.clear();
        invalidEmpIds.clear();

        File f = new File(filePath);
        if (!f.exists() || !f.canRead())
        {
            Errors.add("CSV file not found or cannot be read: " + filePath);
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String header = br.readLine();
            if (header == null) {
                Errors.add("CSV file is empty or missing header.");
                return;
            }
            HeaderValidation(header);

            String line;
            int row = 1;
            while ((line = br.readLine()) != null)
            {
                row++;
                if (line.trim().isEmpty())
                {
                    Errors.add("Row " + row + " is empty.");
                    continue;
                }

                String[] parts = line.split(",", -1);
                if (parts.length < 4)
                {
                    Errors.add("Row " + row + " is missing required fields 'id,firstName,lastName,salary'.");
                    continue;
                }

                String idStr = parts[0].trim();
                String firstName = parts[1].trim();
                String lastName = parts[2].trim();
                String salaryStr = parts[3].trim();
                String managerStr = parts.length > 4 ? parts[4].trim() : "";

                if (idStr.isEmpty())
                {
                    Errors.add("Row " + row + " has empty 'id' field.");
                    continue;
                }
                if (firstName.isEmpty())
                {
                    Errors.add("Row " + row + " has empty 'firstName' field.");
                    continue;
                }
                if (lastName.isEmpty())
                {
                    Errors.add("Row " + row + " has empty 'lastName' field.");
                    continue;
                }
                if (salaryStr.isEmpty())
                {
                    Errors.add("Row " + row + " has empty 'salary' field.");
                    continue;
                }

                try {
                    int id = Integer.parseInt(idStr);
                    if (emp.containsKey(id))
                    {
                        Errors.add("Duplicate employee id detected: " + id + " at row " + row);
                        invalidEmpIds.add(id);
                        continue;
                    }

                    double salary = Double.parseDouble(salaryStr);
                    if (salary <= 0)
                    {
                        Errors.add("Row " + row + " has invalid salary value (must be > 0).");
                        invalidEmpIds.add(id);

                    }

                    Integer managerId = managerStr.isEmpty() ? null : Integer.parseInt(managerStr);
                    Employee e = new Employee(id, firstName, lastName, salary, managerId);
                    emp.put(id, e);
                }
                catch (NumberFormatException ex) {
                    Errors.add("Row " + row + " has invalid number format in 'id', 'salary', or 'managerId'.");
                }
            }
        }
        // Post-importcsv validations
        RootsValidation();               // handle multiple roots and  invalidation
        ManagerExistence();    // mark employees  missing managers if managerid is null
        CheckCircularReferences();  // detect cycles and mark involved employees invalid
    }

    private void HeaderValidation(String header)
    {
        String[] columns = header.split(",", -1);
        List<String> expected = Arrays.asList("id", "firstName", "lastName", "salary");
        for (int i = 0; i < expected.size(); i++)
        {
            if (columns.length <= i || !columns[i].trim().equalsIgnoreCase(expected.get(i)))
            {
                Errors.add("CSV header is invalid. Expected at least: id,firstName,lastName,salary");
                return;
            }
        }
    }

    // If multiple rootuser (managerId == null) are present, pick one CEO (highest salary, then lowest id) and mark other roots invalid to their subordinate,so they are excluded from analysis.
    private void RootsValidation()
    {
        List<Employee> roots = emp.values().stream()
                .filter(e -> e.getManagerId() == null)
                .collect(Collectors.toList());

        if (roots.size() <= 1) return;

        Employee ceo = roots.stream()
                .max(Comparator.comparingDouble(Employee::getSalary)
                        .thenComparingInt(Employee::getId))
                .orElse(null);

        for (Employee r : roots)
        {
            if (ceo != null && r.getId() == ceo.getId()) continue;
            Errors.add(String.format("Employee %d %s %s has missing managerId but is not the CEO; marked invalid for analysis.",
                    r.getId(), r.getFirstName(), r.getLastName()));
            markInvalid(r.getId());
        }
    }


     // Mark an employee id invalid and cascade to all descendants (subordinates, recursively).
    private void markInvalid(int rootId)
    {
        if (invalidEmpIds.contains(rootId)) return;
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(rootId);
        while (!stack.isEmpty()) {
            int id = stack.pop();
            if (!invalidEmpIds.add(id)) continue;
            for (Employee sub : getSubordinates(id))
            {
                Errors.add(String.format("Employee %d %s %s marked invalid because manager %d is invalid.",
                        sub.getId(), sub.getFirstName(), sub.getLastName(), id));
                stack.push(sub.getId());
            }
        }
    }

    //validate that all managerIds reference existing employee ids
    private void ManagerExistence()
    {
        for (Employee e : emp.values())
        {
            Integer m = e.getManagerId();
            if (m != null && !emp.containsKey(m))
            {
                Errors.add("Employee " + e.getId() + " references managerId " + m + " which does not exist.");
                invalidEmpIds.add(e.getId());
            }
        }
    }

    //Finding circular reportingheadlines by DFS
    private void CheckCircularReferences()
    {
        final Map<Integer, Color> color = new HashMap<>();
        for (Integer id : emp.keySet()) color.put(id, Color.WHITE);

        for (Integer id : emp.keySet())
        {
            if (color.get(id) == Color.WHITE)
            {
                Deque<Integer> stack = new ArrayDeque<>();
                dfsDetection(id, color, stack);
            }
        }
    }

    //DFS logic to find
    private void dfsDetection(Integer startId, Map<Integer, Color> color, Deque<Integer> stack)
    {
        color.put(startId, Color.GRAY);
        stack.push(startId);

        Employee cur = emp.get(startId);
        Integer mid = cur == null ? null : cur.getManagerId();
        if (mid != null && emp.containsKey(mid))
        {
            Color mColor = color.get(mid);
            if (mColor == Color.WHITE)
            {
                dfsDetection(mid, color, stack);
            }
            else if (mColor == Color.GRAY)
            {
                List<Integer> cycle = new ArrayList<>();
                for (Integer node : stack)
                {
                    cycle.add(node);
                    if (node.equals(mid)) break;
                }
                Collections.reverse(cycle);
                String cycleStr = cycle.stream().map(Object::toString).collect(Collectors.joining(" -> "));
                Errors.add("Circular reporting detected involving employees: " + cycleStr);
                for (Integer nodeId : cycle)
                {
                    markInvalid(nodeId);
                }
            }
        }
        stack.pop();
        color.put(startId, Color.BLACK);
    }


    private enum Color { WHITE, GRAY, BLACK }


    public Employee get(int id)
    {
        return emp.get(id);
    }


    public Collection<Employee> getAll()
    {
        return emp.values();
    }


    public void addEmployee(Employee e)
    {
        emp.put(e.getId(), e);
    }

    //Get Employee/subordinate of manager
    public List<Employee> getSubordinates(int managerId)
    {
        return emp.values().stream()
                .filter(emp -> emp.getManagerId() != null && emp.getManagerId().equals(managerId))
                .collect(Collectors.toList());
    }


    public List<String> getErrors()
    {
        return Collections.unmodifiableList(Errors);
    }


    public Set<Integer> getInvalidEmployeeIds()
    {
        return Collections.unmodifiableSet(invalidEmpIds);
    }


    public boolean isValidForAnalysis(int id)
    {
        return emp.containsKey(id) && !invalidEmpIds.contains(id);
    }

}
