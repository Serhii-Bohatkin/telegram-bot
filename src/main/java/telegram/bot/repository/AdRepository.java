package telegram.bot.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import telegram.bot.model.Ad;

public interface AdRepository extends JpaRepository<Ad, Long> {
}
