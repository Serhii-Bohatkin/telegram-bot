package telegram.bot.service;

import com.vdurmont.emoji.EmojiParser;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import telegram.bot.config.BotConfig;
import telegram.bot.model.User;
import telegram.bot.repository.UserRepository;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {
    private static final String HELP_TEXT = """
            This bot is created to demonstrate Spring capabilities.\n\n
            You can execute commands from the main menu on the left or by typing a command:\n\n
            Type /start to see a welcome message\n\n
            Type /mydata to see data stored about yourself\n\n
            Type /help to see this message again
            """;
    private static final String YES_BUTTON = "YES_BUTTON";
    public static final String NO_BUTTON = "NO_BUTTON";
    public static final String ERROR_TEXT = "Error occurred: ";
    private final BotConfig botConfig;
    @Autowired
    private UserRepository userRepository;

    public TelegramBot(BotConfig botConfig) {
        this.botConfig = botConfig;
        List<BotCommand> commandOfList = new ArrayList<>();
        commandOfList.add(new BotCommand("/start", "get a welcome message"));
        commandOfList.add(new BotCommand("/mydata", "get your data stored"));
        commandOfList.add(new BotCommand("/deletedata", "delete my data "));
        commandOfList.add(new BotCommand("/help", "info how to use this bot"));
        commandOfList.add(new BotCommand("/settings", "set your preferences"));
        try {
            this.execute(new SetMyCommands(commandOfList, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getBotToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            if (messageText.contains("/send") && botConfig.getBotOwner().equals(chatId)) {
                String textToSend =
                        EmojiParser.parseToUnicode(messageText.substring(messageText.indexOf(" ")));
                Iterable<User> users = userRepository.findAll();
                for (User user : users) {
                    prepareAndSendMessage(user.getChatId(), textToSend);
                }
            } else {
                switch (messageText) {
                    case "/start":
                        registerUser(update.getMessage());
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                        break;
                    case "/help":
                        prepareAndSendMessage(chatId, HELP_TEXT);
                        break;
                    case "/register":
                        register(chatId);
                        break;
                    default:
                        prepareAndSendMessage(chatId, "Command not recognized");
                }
            }

        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            Long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (YES_BUTTON.equals(callbackData)) {
                String text = "You press YES button";
                executeMessageText(chatId, text, messageId);
            } else if (NO_BUTTON.equals(callbackData)) {
                String text = "You press NO button";
                executeMessageText(chatId, text, messageId);
            }
        }
    }

    private void register(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Do you really want to register?");

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        InlineKeyboardButton yesButton = new InlineKeyboardButton();

        yesButton.setText("Yes");
        yesButton.setCallbackData(YES_BUTTON);

        InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText("No");
        noButton.setCallbackData(NO_BUTTON);

        rowInLine.add(yesButton);
        rowInLine.add(noButton);

        rowsInLine.add(rowInLine);
        inlineKeyboardMarkup.setKeyboard(rowsInLine);
        message.setReplyMarkup(inlineKeyboardMarkup);
        executeMessage(message);
    }

    private void registerUser(Message message) {
        if (userRepository.findById(message.getChatId()).isEmpty()) {
            Long chatId = message.getChatId();
            Chat chat = message.getChat();

            User user = new User();
            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
            userRepository.save(user);
            log.info("User saved: " + user);
        }
    }

    private void startCommandReceived(Long chatId, String firstName) {
        String answer = EmojiParser.parseToUnicode(
                "Hi, " + firstName + ", nice to meet you!" + " :blush:");
        log.info("Replied to user " + firstName);
        sendMessage(chatId, answer);
    }

    private void sendMessage(Long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add("weather");
        row.add("get random joke");
        keyboardRows.add(row);

        row = new KeyboardRow();
        row.add("register");
        row.add("check my data");
        row.add("delete my data");
        keyboardRows.add(row);

        ReplyKeyboardMarkup keyboardMarkup = ReplyKeyboardMarkup.builder()
                .resizeKeyboard(true)
                .keyboard(keyboardRows)
                .build();
        message.setReplyMarkup(keyboardMarkup);

        executeMessage(message);
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void executeMessageText(Long chatId, String text, Integer messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setMessageId(messageId);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void prepareAndSendMessage(Long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        executeMessage(message);
    }
}
