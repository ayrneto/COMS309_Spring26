package onetoone.Laptops;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;


import onetoone.Persons.Person;

/**
 * 
 * @author Vivek Bengre
 */ 

@Entity
public class Laptop {
    
    /*
     * The annotation @ID marks the field below as the primary key for the table created by springboot
     * The @GeneratedValue generates a value if not already present, The strategy in this case is to start from 1 and increment for each table
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Positive(message = "cpuClock must be > 0")
    private double cpuClock;

    @Min(value = 1, message = "cpuCores must be >= 1")
    private int cpuCores;

    @Min(value = 1, message = "ram must be >= 1")
    private int ram;

    @NotBlank(message = "manufacturer is required")
    private String manufacturer;

    @Min(value = 0, message = "cost must be >= 0")
    private int cost;

    /*
     * @OneToOne creates a relation between the current entity/table(Laptop) with the entity/table defined below it(Person)
     * @JsonIgnore is to assure that there is no infinite loop while returning either Person/laptop objects (laptop->Person->laptop->...)
     */
    @OneToOne(mappedBy = "laptop")

    private Person person;

//    @JsonIgnore
//    public Person getPerson(){
//        return Person;
//    }
    public Laptop( double cpuClock, int cpuCores, int ram, String manufacturer, int cost) {
        this.cpuClock = cpuClock;
        this.cpuCores = cpuCores;
        this.ram = ram;
        this.manufacturer = manufacturer;
        this.cost = cost;
    }

    public Laptop() {
    }

    // =============================== Getters and Setters for each field ================================== //

    public int getId(){
        return id;
    }

    public void setId(int id){
        this.id = id;
    }


    public double getCpuClock(){return cpuClock;}

    public void setCpuClock(double cpuClock){
        this.cpuClock = cpuClock;
    }

    public int getCpuCores(){return cpuCores;}

    public void setCpuCores(int cpuCores){
        this.cpuCores = cpuCores;
    }

    public String getManufacturer(){
        return manufacturer;
    }

    public void setManufacturer(String manufacturer){
        this.manufacturer = manufacturer;
    }

    public int getCost(){
        return cost;
    }

    public void setCost(int cost){
        this.cost = cost;
    }

    //public Person getPerson(){
        //return Person;
    //}

    @JsonIgnore
    public Person getPerson(){
        return person;
    }

    public void setPerson(Person person){
        this.person = person;
    }
//    public void setPerson(Person Person){
//        this.Person = Person;
//    }

    public int getRam(){
        return ram;
    }

    public void setRam(int ram){
        this.ram = ram;
    }

}
