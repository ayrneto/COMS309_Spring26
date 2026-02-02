package coms309;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
class WelcomeController {

    @GetMapping("/")
    public String welcome() {
        return "Hello shrey from backhand here"; // changed the helloworld statement

    }

    
    @GetMapping("/{name}")
    public String welcome(@PathVariable String name) {
        return "Hello and welcome to COMS 309: " + name;
    }

    @GetMapping("/status")
    public String status(){return "status ok";}

    @GetMapping("/course/{cname}/{cnumber}")
    public String course(@PathVariable String cname,@PathVariable String cnumber){return "Its the "+cname+cnumber ;}

}






