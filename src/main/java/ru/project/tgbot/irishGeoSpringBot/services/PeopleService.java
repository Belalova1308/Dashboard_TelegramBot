package ru.project.tgbot.irishGeoSpringBot.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.project.tgbot.irishGeoSpringBot.models.Person;
import ru.project.tgbot.irishGeoSpringBot.models.Task;
import ru.project.tgbot.irishGeoSpringBot.repositories.PeopleRepositories;

import java.util.Date;
import java.util.List;

@Service
public class PeopleService {

    private final PeopleRepositories peopleRepositories;

    public PeopleService(PeopleRepositories peopleRepositories) {
        this.peopleRepositories = peopleRepositories;
    }

    @Transactional(readOnly = false)
    public void createPerson(Long chatId, String name, Date registered_at){
        if (peopleRepositories.findByTelegramId(chatId)==null){
            Person newPerson = new Person(name, registered_at, chatId);
            peopleRepositories.save(newPerson);
        }
    }

//    public String showAllTasks(Long id){
//        Person person = peopleRepositories.findByTelegramId(id);
//        List<Task> tasks = person.getTasks();
//        StringBuilder endLine = new StringBuilder();
//        int number = 1;
//        for (Task task : tasks) {
//            endLine.append(number).append(". ");
//            endLine.append(task.getTitle());
//            endLine.append("\n");
//            ++number;
//        }
//        return endLine.toString();
//    }


}
