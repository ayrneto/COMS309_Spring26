package onetoone.Assignments;

import jakarta.persistence.*;
import onetoone.Users.User;

@Entity
@Table(name = "user_counsellor_assignment",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id"}))
public class UserCounsellorAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // a normal user gets 0/1 assigned counsellor (easy version)
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "counsellor_user_id", nullable = false)
    private User counsellor;

    public UserCounsellorAssignment() {}

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public User getCounsellor() { return counsellor; }
    public void setCounsellor(User counsellor) { this.counsellor = counsellor; }
}