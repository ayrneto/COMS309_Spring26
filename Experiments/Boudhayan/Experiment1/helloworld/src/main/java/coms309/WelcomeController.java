package coms309;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WelcomeController {

    @GetMapping("/")
    public String welcome() {
        return "Hello this is Boudhayan, working on the backend side.";
    }

    @GetMapping("/welcome/{name}")//message
    public String welcomeByName(@PathVariable String name) {
        return "Hello and welcome to COMS 309: " + name;
    }

    @GetMapping("/student/{name}/{year}")//Name and year
    public String student(@PathVariable String name,
                          @PathVariable int year) {
        return "Welcome to COMS 3090 " + name + ". This is the year " + year;
    }

    @GetMapping("/vote/{age}") //check commit
    public String vote(@PathVariable int age) {
        return age >= 18 ? "You can vote" : "You cannot vote";
    }
}
