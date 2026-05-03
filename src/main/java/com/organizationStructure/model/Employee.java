package com.organizationStructure.model;

public class Employee {

    private final int id;
    private final String firstName;
    private final String lastName;
    private final double salary;
    private final Integer managerId;

    // Constructor
    public Employee(int id, String firstName, String lastName, double salary, Integer managerId) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.salary = salary;
        this.managerId = managerId;
    }


    //Getter methods for all fields
    public int getId()
    {
        return id;
    }
    public String getFirstName()
    {
        return firstName;
    }
    public String getLastName()
    {
        return lastName;
    }
    public double getSalary()
    {
        return salary;
    }
    public Integer getManagerId()
    {
        return managerId;
    }

    //toString for better readability
    @Override
    public String toString() {
        return String.format("%s %s (id=%d)", firstName, lastName, id);
    }
}
