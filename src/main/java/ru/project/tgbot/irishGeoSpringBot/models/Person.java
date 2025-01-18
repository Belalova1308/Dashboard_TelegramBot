package ru.project.tgbot.irishGeoSpringBot.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;


@Getter
@Setter
@Entity
@Table(name = "person")
public class Person {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "name")
    private String name;

    @Column(name = "registered_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date registered_at;

    @Column(name = "telegram_id")
    private Long telegram_id;

    @OneToMany(mappedBy = "person", fetch = FetchType.EAGER)
    private List<Task> tasks;

    @OneToMany(mappedBy = "person", fetch = FetchType.EAGER)
    private List<Tag> tags;

    public Person() {
    }

    public Person(Long telegram_id) {
        this.telegram_id = telegram_id;
    }

    public Person(String name, Date registered_at, Long telegram_id) {
        this.name = name;
        this.registered_at = registered_at;
        this.telegram_id = telegram_id;
    }
}
