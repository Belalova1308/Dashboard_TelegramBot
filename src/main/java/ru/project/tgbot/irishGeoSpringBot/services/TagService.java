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
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TagService {

    private final TagRepository tagRepository;
    private final PeopleRepositories peopleRepositories;
    private final TaskRepository taskRepository;

    public TagService(TagRepository tagRepository, PeopleRepositories peopleRepositories, TaskRepository taskRepository) {
        this.tagRepository = tagRepository;
        this.peopleRepositories = peopleRepositories;
        this.taskRepository = taskRepository;
    }

    public List<Tag> getAllTags(Long chatId) {
        Person person = peopleRepositories.findByTelegramId(chatId);
        return person.getTags();
    }

    @Transactional(readOnly = false)
    public void addTag(Long chatId, String tagName) {
        Person person = peopleRepositories.findByTelegramId(chatId);
        Tag tag = new Tag(tagName, person);
        tagRepository.save(tag);
    }

    public Boolean searchTag(Long chatId, String name) {
        Person person = peopleRepositories.findByTelegramId(chatId);
        return tagRepository.findPersonTag(person.getId(), name) != null;
    }

    @Transactional(readOnly = false)
    public void deleteTag(Long chatId, String tagName) {
        Person person = peopleRepositories.findByTelegramId(chatId);
        Tag tag = tagRepository.findPersonTag(person.getId(), tagName);
        tag.getTasks().forEach(p-> p.setTag(null));
        tagRepository.deleteById(tag.getId());
    }


}
