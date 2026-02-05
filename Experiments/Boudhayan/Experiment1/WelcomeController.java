package Experiment1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
class WelcomeController {

    @GetMapping("/")
    public String welcome() {

        return "Hello this is Boudhayan, working on the backend side.";
    }

    @GetMapping("/{name}")
    public String welcome(@PathVariable String name) {
        return "Hello and welcome to COMS 309: " + name;
    }

    @GetMapping("/{name}/{year}")
    public String Student(@PathVariable String name, @PathVariable int yr){
        return "Welcome to COMS 3090 "+name+". This is the year "+yr;
    }

    @GetMapping("/{age}")
    public String Vote(@PathVariable int age){
        if(age>=18)
            return "You can vote";
        else
            return "You cannot vote";
    }
}
