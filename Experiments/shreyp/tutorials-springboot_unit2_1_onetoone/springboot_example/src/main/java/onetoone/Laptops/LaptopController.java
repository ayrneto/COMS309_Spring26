package onetoone.Laptops;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import onetoone.Persons.Person;
import onetoone.Persons.PersonRepository;
import jakarta.validation.Valid;


/**
 * 
 * @author Vivek Bengre
 * 
 */ 

@RestController
public class LaptopController {

    @Autowired
    LaptopRepository laptopRepository;

    @Autowired
    PersonRepository personRepository;
    
    private String success = "{\"message\":\"success\"}";
    private String failure = "{\"message\":\"failure\"}";

    @GetMapping(path = "/laptops")
    List<Laptop> getAllLaptops(){
        return laptopRepository.findAll();
    }

    @GetMapping(path = "/laptops/{id}")
    Laptop getLaptopById(@PathVariable int id){
        return laptopRepository.findById(id);
    }

    @PostMapping(path = "/laptops")
    String createLaptop(@RequestBody Laptop Laptop){
        if (Laptop == null)
            return failure;
        laptopRepository.save(Laptop);
        return success;
    }


    @PutMapping(path = "/laptops/{id}")
    Laptop updateLaptop(@PathVariable int id, @Valid @RequestBody Laptop request){
                Laptop existing = laptopRepository.findById(id);

                if(existing == null) {
                        throw new RuntimeException("Laptop id does not exist");
                    }

                // Optional: enforce that request id matches path id if request includes id
                if (request.getId() != 0 && request.getId() != id) {
                        throw new RuntimeException("path variable id does not match request id");
                    }

                // Safe update: update fields individually
                existing.setCpuClock(request.getCpuClock());
                existing.setCpuCores(request.getCpuCores());
                existing.setRam(request.getRam());
                existing.setManufacturer(request.getManufacturer());
                existing.setCost(request.getCost());
                // Note: We don't update person relationship here - use PersonController link/unlink endpoints

                laptopRepository.save(existing);
                return laptopRepository.findById(id);
            }

//    @PutMapping(path = "/laptops/{id}")
//    Laptop updateLaptop(@PathVariable int id, @RequestBody Laptop request){
//        Laptop laptop = laptopRepository.findById(id);
//        if(laptop == null)
//            return null;
//        laptopRepository.save(request);
//        return laptopRepository.findById(id);
//    }

//    @DeleteMapping(path = "/laptops/{id}")
//    String deleteLaptop(@PathVariable int id){
//
//        // Check if there is an object depending on Person and then remove the dependency
//        Person person = personRepository.findByLaptop_Id(id);
//        person.setLaptop(null);
//        personRepository.save(person);
//
//        // delete the laptop if the changes have not been reflected by the above statement
//        laptopRepository.deleteById(id);
//        return success;
//    }


    @DeleteMapping(path = "/laptops/{id}")
    String deleteLaptop(@PathVariable int id){
                // Check if laptop exists
                Laptop laptop = laptopRepository.findById(id);
                if(laptop == null) {
                        return failure;
                    }

                // Check if there is a person with this laptop and remove the dependency
                Person person = personRepository.findByLaptop_Id(id);
                if(person != null) {
                        person.setLaptop(null);
                        personRepository.save(person);
                    }

                // Delete the laptop
                laptopRepository.deleteById(id);
                return success;
            }
}
