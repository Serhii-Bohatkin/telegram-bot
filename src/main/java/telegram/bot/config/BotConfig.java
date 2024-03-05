package telegram.bot.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@Getter
@EnableScheduling
public class BotConfig {
    @Value("${bot.username}")
    private String botName;
    @Value("${bot.token}")
    private String botToken;
    @Value("${bot.owner}")
    private Long botOwner;
}
