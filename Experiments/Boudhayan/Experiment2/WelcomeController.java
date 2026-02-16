package Experiment2;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
class WelcomeController {

    @GetMapping("/")
    public String welcome() {
        return "Hello this is Boudhayan, working on the backend side.";
    }

    @GetMapping("/welcome/{name}") // Added /welcome/ to avoid ambiguity
    public String welcome(@PathVariable String name) {
        return "Hello and welcome to COMS 309: " + name;
    }

    @GetMapping("/{name}/{year}")
    public String Student(@PathVariable String name, @PathVariable int year){ // Fixed 'yr' to 'year'
        return "Welcome to COMS 3090 " + name + ". This is the year " + year;
    }

    @GetMapping("/vote/{age}") // Fixed bracket and changed path to avoid conflict
    public String Votereligibilty(@PathVariable int age){ // Added @PathVariable
        if(age >= 18)
            return "You can vote";
        else
            return "You cannot vote";
    }

    @GetMapping("/{num1}/{num2}")
    public int Summation(@PathVariable int num1, @PathVariable int num2){
        return num1+num2;
    }
}
