package onetoone.Assignments;

import jakarta.persistence.*;
import onetoone.Users.User;
import io.swagger.v3.oas.annotations.media.Schema;



@Entity
@Table(name = "user_counsellor_assignment",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id"}))
public class UserCounsellorAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Assignment ID", example = "1")
    private Long id;

    // a normal user gets 0/1 assigned counsellor (easy version)
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @Schema(description = "The user who is assigned a counsellor")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "counsellor_user_id", nullable = false)
    @Schema(description = "The assigned counsellor")
    private User counsellor;

    public UserCounsellorAssignment() {}

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public User getCounsellor() { return counsellor; }
    public void setCounsellor(User counsellor) { this.counsellor = counsellor; }
}