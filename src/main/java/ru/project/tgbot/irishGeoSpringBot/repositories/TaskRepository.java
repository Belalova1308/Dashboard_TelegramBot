package ru.project.tgbot.irishGeoSpringBot.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.project.tgbot.irishGeoSpringBot.models.Person;
import ru.project.tgbot.irishGeoSpringBot.models.Tag;
import ru.project.tgbot.irishGeoSpringBot.models.Task;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Integer> {

    //    @Query(value = "SELECT * from Task where tag_id =? and person_id=?", nativeQuery = true)
//    List<Task> findAllByTagId(int tagId, int person_id);
    @Query("SELECT t FROM Task t JOIN FETCH t.tag WHERE t.tag.id = :tagId AND t.person.id = :personId")
    List<Task> findAllTasksByTagAndPerson(@Param("tagId") int tagId, @Param("personId") int personId);

}
