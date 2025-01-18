package ru.project.tgbot.irishGeoSpringBot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.project.tgbot.irishGeoSpringBot.models.Person;

import java.util.List;
import java.util.Optional;

@Repository
public interface PeopleRepositories extends JpaRepository<Person, Integer> {
    @Query(value = "SELECT * from Person where telegram_id =?", nativeQuery = true)
    Person findByTelegramId(Long chatId);
}
