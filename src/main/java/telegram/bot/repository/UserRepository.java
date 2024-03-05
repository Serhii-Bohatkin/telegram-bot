package telegram.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import telegram.bot.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
