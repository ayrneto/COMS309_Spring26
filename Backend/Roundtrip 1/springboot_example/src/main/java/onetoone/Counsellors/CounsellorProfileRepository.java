package onetoone.Counsellors;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CounsellorProfileRepository extends JpaRepository<CounsellorProfile, Long> {
    Optional<CounsellorProfile> findByUser_Id(Long userId);
}