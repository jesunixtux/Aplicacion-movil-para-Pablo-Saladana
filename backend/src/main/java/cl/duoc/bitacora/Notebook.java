package cl.duoc.bitacora;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Notebook {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String title;
    @Column(columnDefinition = "TEXT")
    public String purpose;
    @Column(columnDefinition = "TEXT")
    public String instructions;
    @Column(columnDefinition = "TEXT")
    public String questions;
    public String teacherEmail;
    public String teacherName;
    public long createdAt;

    protected Notebook() {}

    Notebook(NotebookController.NotebookInput input) {
        update(input);
        createdAt = System.currentTimeMillis();
    }

    void update(NotebookController.NotebookInput input) {
        title = input.title().trim();
        purpose = input.purpose().trim();
        instructions = input.instructions().trim();
        questions = input.questions() == null ? "" : input.questions().trim();
        teacherEmail = input.teacherEmail().trim();
        teacherName = input.teacherName().trim();
    }
}
