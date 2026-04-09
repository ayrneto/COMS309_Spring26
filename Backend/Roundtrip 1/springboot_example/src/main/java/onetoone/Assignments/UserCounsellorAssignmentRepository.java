package onetoone.Assignments;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserCounsellorAssignmentRepository extends JpaRepository<UserCounsellorAssignment, Long> {
    Optional<UserCounsellorAssignment> findByUserId(int userId);

    void deleteByUser_Id(Long userId);
    void deleteByCounsellor_Id(Long counsellorId);

}