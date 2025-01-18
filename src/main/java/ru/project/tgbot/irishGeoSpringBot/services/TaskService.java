package ru.project.tgbot.irishGeoSpringBot.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.project.tgbot.irishGeoSpringBot.models.Person;
import ru.project.tgbot.irishGeoSpringBot.models.Tag;
import ru.project.tgbot.irishGeoSpringBot.models.Task;
import ru.project.tgbot.irishGeoSpringBot.repositories.PeopleRepositories;
import ru.project.tgbot.irishGeoSpringBot.repositories.TagRepository;
import ru.project.tgbot.irishGeoSpringBot.repositories.TaskRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TaskService {

    private final PeopleRepositories peopleRepositories;
    private final TaskRepository taskRepository;
    private final TagService tagService;
    private final TagRepository tagRepository;


    public TaskService(PeopleRepositories peopleRepositories, TaskRepository taskRepository, TagService tagService, TagRepository tagRepository) {
        this.peopleRepositories = peopleRepositories;
        this.taskRepository = taskRepository;
        this.tagService = tagService;
        this.tagRepository = tagRepository;
    }

    public List<Task> showAllTasks(Long id) {
        Person person = peopleRepositories.findByTelegramId(id);
        return person.getTasks();

    }

    @Transactional(readOnly = false)
    public void addTask(Long chatId, String message) {

        Person person = peopleRepositories.findByTelegramId(chatId);
        Task task = new Task("Не сделано", message, person);
        task.setPerson(person);
        task.setTitle(message);
        task.setStatus("Не сделано");
        taskRepository.save(task);
    }

    @Transactional(readOnly = false)
    public void updateTask(int taskId, String description) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Book not found"));
        task.setTitle(description);
        taskRepository.save(task);
    }

    @Transactional(readOnly = false)
    public void changeStatus(int taskId, String status) {
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));
        task.setStatus(status);
        taskRepository.save(task);
    }

    @Transactional(readOnly = false)
    public void deleteTask(int id) {
        taskRepository.deleteById(id);
    }

    @Transactional(readOnly = false)
    public void attachTag(Long chatId, int taskId, String tagName) {
        Person person = peopleRepositories.findByTelegramId(chatId);
        Tag tag = tagRepository.findPersonTag(person.getId(), tagName);
        if (tag != null) {
            Task task = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("Task not found"));
            task.setTag(tag);
            taskRepository.save(task);
        }
    }

    public List<Task> showAllByTagName(Long chatId, int tagId) {
        Person person = peopleRepositories.findByTelegramId(chatId);
        return taskRepository.findAllTasksByTagAndPerson(tagId, person.getId());
    }

    public Task showByTaskId(int taskId){
        return taskRepository.findById(taskId).orElse(null);
    }

    public Map<Tag, List<Task>> showTasksGroupedByTag(Long chatId) {
        Person person = peopleRepositories.findByTelegramId(chatId);
        List<Task> tasks = person.getTasks();

        Map<Tag, List<Task>> groupedTasks = tasks.stream()
                .collect(Collectors.groupingBy(Task::getTag));

        return groupedTasks;
    }
}
