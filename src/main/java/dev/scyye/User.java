package dev.scyye;

@Entity
@Table(name = "users")
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String username;

	@Column(nullable = false)
	private String passwordHash; // store hashed password (BCrypt)

	public User(long id, String username, String passwordHash) {
	}

	// Constructors, getters, setters
}

