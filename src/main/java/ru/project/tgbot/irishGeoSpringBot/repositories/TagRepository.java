package ru.project.tgbot.irishGeoSpringBot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.project.tgbot.irishGeoSpringBot.models.Person;
import ru.project.tgbot.irishGeoSpringBot.models.Tag;

import java.util.List;

@Repository
public interface TagRepository extends JpaRepository<Tag, Integer> {
        Tag findByName(String name);

        @Query(value = "SELECT * from Tag where person_id =? and name=?", nativeQuery = true)
        Tag findPersonTag(int personId, String tagName);

}
