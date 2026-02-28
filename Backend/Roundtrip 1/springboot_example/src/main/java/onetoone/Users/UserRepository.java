package onetoone.Users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 
 * @author Boudhayan Chakraborty
 * Database access layer.
 *
 * What it does:
 *
 * Extends JpaRepository<User, Integer>.
 *
 * Provides CRUD operations automatically.
 * 
 */ 

public interface UserRepository extends JpaRepository<User, Long> {
    User findByEmail(String email);
}
