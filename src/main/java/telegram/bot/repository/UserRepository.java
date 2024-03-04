package telegram.bot.repository;

import org.springframework.data.repository.CrudRepository;
import telegram.bot.model.User;

public interface UserRepository extends CrudRepository<User, Long> {
}
