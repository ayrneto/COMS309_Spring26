/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.owner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;


import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;



/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 * @Modified By Tanmay Ghosh
 * @Modified By Vivek Bengre
 */
@RestController
class OwnerController {

    @Autowired
    OwnerRepository ownersRepository;

    private final Logger logger = LoggerFactory.getLogger(OwnerController.class);

//    @RequestMapping(method = RequestMethod.POST, path = "/owners/new")
//    public String saveOwner(@RequestBody Owners owner) {
//        ownersRepository.save(owner);
//        return "New Owner "+ owner.getFirstName() + " Saved";
//    }

// changed from return string to return owners json methos -sp
    @RequestMapping(method = RequestMethod.POST, path = "/owners")
    public Owners createOwner(@RequestBody Owners owner) {
        return ownersRepository.save(owner);
    }

    // function just to create dummy data
    @RequestMapping(method = RequestMethod.GET, path = "/owner/create")
    public List<Owners> createDummyData() {
        Owners o1 = new Owners(1, "shrey", "patel", "404 Not found", "random numbers");
        Owners o2 = new Owners(2, "leo", "messi", "FC Barcelona", "I wished I had");
        Owners o3 = new Owners(3, "Nix", "gg", "sleeps in cyride", "515-345-41213");
        Owners o4 = new Owners(4, "Chad", "chad jr", "dining", "420-420-4200");
        ownersRepository.save(o1);
        ownersRepository.save(o2);
        ownersRepository.save(o3);
        ownersRepository.save(o4);
        return List.of(o1, o2, o3, o4);
    }

    @RequestMapping(method = RequestMethod.GET, path = "/owners")
    public List<Owners> getAllOwners() {
        logger.info("Entered into Controller Layer");
        List<Owners> results = ownersRepository.findAll();
        logger.info("Number of Records Fetched:" + results.size());
        return results;
    }

//    @RequestMapping(method = RequestMethod.GET, path = "/owners/{ownerId}")
//    public Optional<Owners> findOwnerById(@PathVariable("ownerId") int id) {
//        logger.info("Entered into Controller Layer");
//        Optional<Owners> results = ownersRepository.findById(id);
//        return results;
//    }

// Added 404 error if id not exist -sp
    @RequestMapping(method = RequestMethod.GET, path = "/owners/{ownerId}")
    public Owners findOwnerById(@PathVariable("ownerId") int id) {
        logger.info("Entered into Controller Layer");
        return ownersRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Owner " + id + " not found"
                ));
    }


// update owner endpoint with 404 error instead of null -sp
    @RequestMapping(method = RequestMethod.PUT, path = "/owners/{ownerId}")
    public Owners updateOwner(@PathVariable("ownerId") int id, @RequestBody Owners updated) {

        Owners existing = ownersRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Owner " + id + " not found"
        ));
//        if (existingOpt.isEmpty()) {
//            return null; // Step 3 will replace with proper 404 error handling
//        }
//
//        Owners existing = existingOpt.get();
        existing.setFirstName(updated.getFirstName());
        existing.setLastName(updated.getLastName());
        existing.setAddress(updated.getAddress());
        existing.setTelephone(updated.getTelephone());

        return ownersRepository.save(existing);
    }


    // used map to give json rather than plain text
    @RequestMapping(method = RequestMethod.DELETE, path = "/owners/{ownerId}")
    public Map<String, Object> deleteOwner(@PathVariable("ownerId") int id) {
        if (!ownersRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Owner " + id + " not found");
        }
        ownersRepository.deleteById(id);
        return Map.of(
                "message", "Deleted owner " + id,
                "ownerId", id
                );
    }


    @RequestMapping(method = RequestMethod.GET, path = "/owners/search")
    public List<Owners> searchOwners(@RequestParam String lastName) {
        return ownersRepository.findByLastNameContainingIgnoreCase(lastName);
    }



}
