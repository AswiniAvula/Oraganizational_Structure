
**Organization Structure** is a small, focused Java application that inspects a company’s employee CSV and reports structural and salary issues. It enforces the board’s rules: managers must earn 20%–50% more than the average of their direct subordinates, and no employee should have more than 4 managers between them and the CEO. The tool reads a CSV, validates the data, and prints a clear console report.

**Features**
CSV import and validation: checks header, required fields, numeric formats, duplicate IDs, and non positive salaries.

Root handling: when multiple roots exist, selects a single CEO by highest salary then lowest id, marks other roots invalid, and cascades invalidation to their subtrees.

Manager existence checks: flags employees that reference missing managers.

Cycle detection: finds circular reporting and marks involved employees invalid.

Salary analysis: reports managers who earn too low or too high relative to the average salary of their direct reports, with exact amounts.

Reporting line analysis: identifies employees whose reporting chain to the CEO exceeds 4 managers and reports how many levels too long.

Console output only; no GUI.

Quick Start
**Requirements**

Java SE 8 or later

Maven

JUnit 4 for tests

Build

bash
mvn clean package
Run

bash
java -jar target/organizationStructure-1.0-SNAPSHOT.jar path/to/employees.csv
The program prints validation messages first, then salary issues, then reporting line issues.

Example CSV
Code
id,firstName,lastName,salary,managerId
1,CEO,Boss,100000,
2,Manager,A,80000,1
3,Manager,B,70000,2
4,Manager,C,60000,3
5,Manager,D,50000,4
6,Employee,E,40000,5
7,Employee,F,30000,6

Example Output

=== Salary Issues ===
Manager A (id=2) earns too low by 4000.00
Manager B (id=3) earns too low by 2000.00
=== Reporting Line Issues ===
Employee E (id=6) has reporting line long by 1
Employee F (id=7) has reporting line long by 2

**Assumptions and Notes**
CEO identification: CEO is any employee with empty managerId. If multiple roots exist, the CEO is chosen by highest salary, tie broken by lowest id. All other roots and their descendant subtrees are marked invalid and excluded from analysis.

Employee IDs must be unique integers. If duplicates are found, the first occurrence is kept and duplicates are reported and marked invalid.

Salary must be a positive number. Non positive salaries are reported and the employee is marked invalid.

ManagerId must reference an existing employee id. If not, the employee is marked invalid.

Reporting line length counts the number of managers between an employee and the CEO (direct manager equals 1). Any count greater than 4 is reported.

Money handling: salaries are treated with two decimal precision. The implementation uses precise decimal arithmetic to avoid floating point rounding issues.

When input ambiguity arises, the program favors conservative validation and reports the issue rather than guessing.

Tests
Run unit tests with:

bash
mvn test

**The test suite includes cases for:**

CSV parsing and header validation

Duplicate ids and invalid numeric formats

Multiple roots and CEO selection policy

Cycle detection and cascading invalidation

Salary boundary checks and reporting line length checks

**Project Structure**
Code
src/main/java    - application code
src/test/java    - JUnit tests
pom.xml          - Maven build file
src/test/resources - test CSV files