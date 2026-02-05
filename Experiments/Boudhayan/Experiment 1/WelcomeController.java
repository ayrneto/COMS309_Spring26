package "Experiment 1";

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

    @GetMapping("/student/{name}/{year}")
    public String Student(@PathVariable String name, @PathVariable int yr){
        return "Welcome to COMS 3090 "+name+". This is the year "+yr;
    }
}
