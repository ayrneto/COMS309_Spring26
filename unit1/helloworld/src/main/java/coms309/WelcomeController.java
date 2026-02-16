package coms309;

import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;


@RestController
class WelcomeController {




    @GetMapping("/")
    public String welcome() {
        return "Hello shrey from backend here"; // changed the helloworld statement

    }

    
    @GetMapping("/{name}")
    public String welcome(@PathVariable String name) {
        return "Hello and welcome to COMS 309: " + name;
    }

    @GetMapping("/status")
    public String status(){return "status ok";}

    @GetMapping("/course/{cname}/{cnumber}")
    public String course(@PathVariable String cname,@PathVariable String cnumber){return "Its the "+cname+cnumber ;}


    @GetMapping("/parity/{num}")
    public String checkParity(@PathVariable int num) {
        if (num % 2 == 0) {
            return num + " is an Even number";
        } else {
            return num + " is an Odd number";
        }
    }
    @GetMapping("/reverse")
    public String reverseWelcome() {

        // Get the original welcome message
        String origStg = welcome();

        // Reverse it
        String reversed = "";

        for (int i = origStg.length() - 1; i >= 0; i--) {
            reversed += origStg.charAt(i);
        }

        return reversed;
    }





}






