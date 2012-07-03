package model.tldr;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "T_PERSON")
public class Person {

    private int id;

    private Timestamp created;

    private String firstName;

    private String lastName;

    @GeneratedValue
    @Id
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setCreated(Timestamp param) {
        this.created = param;
    }

    @Version
    public Timestamp getCreated() {
        return created;
    }

    public void setFirstName(String param) {
        this.firstName = param;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setLastName(String param) {
        this.lastName = param;
    }

    public String getLastName() {
        return lastName;
    }

}