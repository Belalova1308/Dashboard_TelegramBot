package ru.project.tgbot.irishGeoSpringBot.services;

import com.vdurmont.emoji.EmojiParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.project.tgbot.irishGeoSpringBot.models.Tag;
import ru.project.tgbot.irishGeoSpringBot.models.Task;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;

import java.io.File;
import java.util.*;

@Component
public class TelegramBot extends TelegramLongPollingBot {
    private static final String START_MESSAGE = EmojiParser.parseToUnicode(""" 
            ,\sДобрый день! """ + "\n\n" + "Выберите действие");
    private static final String HELP_MESSAGE = EmojiParser.parseToUnicode("""
            Данный бот предназначен для отслеживания ваших задач.
            Доступен поиск по тегу
            1) Чтобы начать работу нажмите на /start
            2) Чтобы добавить/редактировать нажмите на соответствующие кнопки
            """);
    private final TaskService taskService;
    private final PeopleService peopleService;
    private final TagService tagService;
    private final WriteFile writeFile;
    @Value("${bot.name}")
    private String botName;

    @Value("${bot.token}")
    private String botToken;

    private Map<Long, String> userState = new HashMap<>();

    public TelegramBot(TaskService taskService, PeopleService peopleService, TagService tagService, WriteFile writeFile) {
        this.taskService = taskService;
        this.peopleService = peopleService;
        this.tagService = tagService;
        this.writeFile = writeFile;
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Long chatId = update.getMessage().getChatId();
            String messageText = update.getMessage().getText();

            switch (messageText) {
                case "/start":
                    peopleService.createPerson(chatId, update.getMessage().getChat().getFirstName()
                            + " " + update.getMessage().getChat().getLastName(), new Date());
                    sendMessageWithKeyboard(chatId, update.getMessage().getChat().getFirstName() + START_MESSAGE);
                    break;

                case "Показать все задачи":
                    List<Task> tasks = taskService.showAllTasks(chatId);
                    getAllTasks(chatId, tasks);
                    break;

                case "/help":
                    sendMessage(chatId, HELP_MESSAGE);
                    break;

                case "Добавить задачу":
                    userState.put(chatId, "ADDING_TASK");
                    sendMessage(chatId, "✏ Введите задачу, которую хотите добавить.");
                    break;

                case "Найти задачу по тегу":
                    executeMessage(showTagsKeyboard(chatId, tagService.getAllTags(chatId)));
                    break;

                case "Добавить тег":
                    userState.put(chatId, "ADD_TAG");
                    sendMessage(chatId, "✏ Введите тег, который хотите добавить");
                    break;

                case "Удалить тег":
                    userState.put(chatId, "DELETE_TAG");
                    sendMessage(chatId, "✏ Введите тег, который хотите удалить");
                    break;

                case "Файл со всеми задачами":
                    List<Task> tasksToFile = taskService.showAllTasks(chatId);
                    if (tasksToFile.isEmpty()){
                        sendMessage(chatId, "У вас пока нет задач");
                    } else {
                        writeFile.writeWordFile(chatId, tasksToFile);
                        sendDocument(chatId, "C:/sendToTg/document.docx");
                    }

                    break;

                default:
                    if (userState.containsKey(chatId)) {
                        handleStateBasedLogic(chatId, messageText);
                    } else {
                        sendMessage(chatId, "⚠ Неизвестная команда");
                    }
                    break;

            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            String callbackQueryId = update.getCallbackQuery().getId();

            if (callbackData.startsWith("update_")) {
                sendMessage(chatId, "✏ Измените задачу:");
                int taskId = Integer.parseInt(callbackData.split("_")[1]);
                userState.put(chatId, "CHANGE_" + taskId);

            }
            if (callbackData.startsWith("delete_")) {
                int taskId = Integer.parseInt(callbackData.split("_")[1]);
                taskService.deleteTask(taskId);
                sendMessage(chatId, "Задача успешно удалена!");
            }

            if (callbackData.startsWith("statusDone_")) {
                int taskId = Integer.parseInt(callbackData.split("_")[1]);
                taskService.changeStatus(taskId, "Сделано");
                sendMessage(chatId, "Статус изменен!");
            }

            if (callbackData.startsWith("statusNotDone_")) {
                int taskId = Integer.parseInt(callbackData.split("_")[1]);
                taskService.changeStatus(taskId, "Не сделано");
                sendMessage(chatId, "Статус изменен!");
            }


            if (callbackData.startsWith("tag_")) {
                int tagId = Integer.parseInt(callbackData.split("_")[1]);
                List<Task> tasks = taskService.showAllByTagName(chatId, tagId);
                getAllTasks(chatId, tasks);
            }
            if (callbackData.startsWith("attachTag_")) {
                int taskId = Integer.parseInt(callbackData.split("_")[1]);
                executeMessage(showTagsToAttachKeyboard(chatId, taskId, tagService.getAllTags(chatId)));
                userState.put(chatId, "ATTACHTAG_" + taskId);
                answerCallbackQuery(callbackQueryId);

            } else if (callbackData.startsWith("tagToAttach_")) {
                if (userState.get(chatId).startsWith("ATTACHTAG_")) {

                    int taskId = Integer.parseInt(userState.get(chatId).split("_")[1]);
                    String[] parts = callbackData.split("_");
                    String tagName = parts[2];

                    taskService.attachTag(chatId, taskId, tagName);
                    sendMessage(chatId, "Тег \"" + tagName + "\" назначен!");

                    userState.remove(chatId);
                }
                answerCallbackQuery(callbackQueryId);
            }

            answerCallbackQuery(callbackQueryId);
        }

    }


    private void handleStateBasedLogic(Long chatId, String messageText) {
        if (userState.get(chatId).equals("ADDING_TASK")) {

            taskService.addTask(chatId, messageText);
            sendMessage(chatId, "Задача \"" + (messageText) + "\" добавлена");
            userState.remove(chatId);

        } else if (userState.get(chatId).startsWith("ADD_TAG")) {

            if (tagService.searchTag(chatId, messageText)) {
                sendMessage(chatId, "⚠ Тег \"" + (messageText) + "\" уже существует\n"
                        + "Введите другое название тега");
            } else {
                tagService.addTag(chatId, messageText);
                sendMessage(chatId, "Тег \"" + (messageText) + "\" добавлен");
                userState.remove(chatId);
            }

        } else if (userState.get(chatId).startsWith("CHANGE_")) {

            String[] parts = userState.get(chatId).split("_");
            int taskId = Integer.parseInt(parts[1]);
            taskService.updateTask(taskId, messageText);
            sendMessage(chatId, "Задача изменена на " + messageText);
            userState.remove(chatId);

        } else if (userState.get(chatId).equals("DELETE_TAG")){
            if (tagService.searchTag(chatId, messageText)){
                tagService.deleteTag(chatId, messageText);
                sendMessage(chatId, "Тег \"" + (messageText) + "\" удален");
            } else {
                sendMessage(chatId, "Тег \"" + (messageText) + "\" не существует");
            }
        }
    }

    private void getAllTasks(Long chatId, List<Task> tasks) {

        if (tasks.isEmpty()) {
            sendMessage(chatId, "У вас пока нет задач");
        } else {
            sendMessage(chatId, """
                    📝 Ваши задачи:""");
            for (Task task : tasks) {
                String tagName = Optional.ofNullable(task.getTag())
                        .map(Tag::getName)
                        .orElse("Без тега");
                String message = """
                        📎 Задача:\s"""
                        + task.getTitle() +"\n" + """
                        🏁 Статус:\s""" + task.getStatus() + (!task.getStatus().equals("Сделано") ? " ❌" : " ✅"
                )
                        + """
                        
                        🔖 Тег:\s""" + tagName;
                try {
                    execute(showTaskKeyboard(chatId, message, task.getId()));
                } catch (TelegramApiException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }


    private void sendMessage(Long chatId, String messageToSend) {
        SendMessage message = new SendMessage();

        message.setChatId(String.valueOf(chatId));
        message.setText(messageToSend);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendMessageWithKeyboard(Long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("Показать все задачи");
        row.add("Добавить задачу");
        keyboardRows.add(row);

        row = new KeyboardRow();

        row.add("Найти задачу по тегу");
        row.add("Добавить тег");
        row.add("Удалить тег");
        keyboardRows.add(row);
        row = new KeyboardRow();
        row.add("Файл со всеми задачами");
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);

        executeMessage(message);
    }


    public SendMessage showTaskKeyboard(Long chatId, String taskInfo, int taskId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(taskInfo);
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        List<InlineKeyboardButton> rowInline1 = new ArrayList<>();
        InlineKeyboardButton updateButton = new InlineKeyboardButton();
        InlineKeyboardButton deleteButton = new InlineKeyboardButton();
        updateButton.setText("Изменить");
        updateButton.setCallbackData("update_" + taskId);
        deleteButton.setText("Удалить");
        deleteButton.setCallbackData("delete_" + taskId);
        rowInline1.add(updateButton);
        rowInline1.add(deleteButton);

        List<InlineKeyboardButton> rowInline2 = new ArrayList<>();
        InlineKeyboardButton doneButton = new InlineKeyboardButton();
        InlineKeyboardButton notDoneButton = new InlineKeyboardButton();
        doneButton.setText("Сделано");
        doneButton.setCallbackData("statusDone_" + taskId);
        notDoneButton.setText("Не сделано");
        notDoneButton.setCallbackData("statusNotDone_" + taskId);
        rowInline2.add(doneButton);
        rowInline2.add(notDoneButton);

        List<InlineKeyboardButton> rowInline3 = new ArrayList<>();
        InlineKeyboardButton attachTag = new InlineKeyboardButton();
        attachTag.setText("Назначить тег");
        attachTag.setCallbackData("attachTag_" + taskId);

        rowInline3.add(attachTag);
        rowsInline.add(rowInline1);
        rowsInline.add(rowInline2);
        rowsInline.add(rowInline3);
        inlineKeyboardMarkup.setKeyboard(rowsInline);
        message.setReplyMarkup(inlineKeyboardMarkup);

        return message;
    }

    public SendMessage showTagsKeyboard(Long chatId, List<Tag> tags) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        if (tags.isEmpty()) {
            message.setText("У вас пока нет тегов");
            return message;
        } else {
            message.setText("Ваши теги:");
            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

            List<InlineKeyboardButton> rowInline1 = new ArrayList<>();

            for (int i = 0; i < tags.size(); i++) {
                Tag tag = tags.get(i);

                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(tag.getName());
                button.setCallbackData("tag_" + tag.getId());
                rowInline1.add(button);

                if (rowInline1.size() == 2 || i == tags.size() - 1) {
                    rowsInline.add(rowInline1);
                    rowInline1 = new ArrayList<>();
                }

            }

            inlineKeyboardMarkup.setKeyboard(rowsInline);
            message.setReplyMarkup(inlineKeyboardMarkup);

            return message;
        }

    }

    public SendMessage showTagsToAttachKeyboard(Long chatId, int taskId, List<Tag> tags) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        if (tags.isEmpty()) {
            message.setText("У вас пока нет тегов");
            return message;
        } else {
            message.setText("Выберите тег для задачи: \""  + taskService.showByTaskId(taskId).getTitle() + "\"");
            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

            List<InlineKeyboardButton> rowInline1 = new ArrayList<>();

            for (int i = 0; i < tags.size(); i++) {
                Tag tag = tags.get(i);

                InlineKeyboardButton button1 = new InlineKeyboardButton();
                button1.setText(tag.getName());
                button1.setCallbackData("tagToAttach_" + taskId + "_" + tag.getName());
                rowInline1.add(button1);

                if (rowInline1.size() == 2 || i == tags.size() - 1) {
                    rowsInline.add(rowInline1);
                    rowInline1 = new ArrayList<>();
                }

            }

            inlineKeyboardMarkup.setKeyboard(rowsInline);
            message.setReplyMarkup(inlineKeyboardMarkup);

            return message;
        }

    }

    private void answerCallbackQuery(String callbackQueryId) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackQueryId);
        try {
            execute(answer);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendDocument(Long chatId, String filePath) {
        File file = new File(filePath);
        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(String.valueOf(chatId));
        sendDocument.setDocument(new InputFile(file));
        try {
            execute(sendDocument);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
