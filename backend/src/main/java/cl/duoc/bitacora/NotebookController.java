package cl.duoc.bitacora;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/notebooks")
public class NotebookController {
    private final NotebookRepository repository;

    NotebookController(NotebookRepository repository) {
        this.repository = repository;
    }

    public record NotebookInput(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 10000) String purpose,
        @NotBlank @Size(max = 10000) String instructions,
        @Size(max = 10000) String questions,
        @NotBlank @Email @Size(max = 255) String teacherEmail,
        @NotBlank @Size(max = 255) String teacherName
    ) {}

    @GetMapping
    List<Notebook> list() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    Notebook get(@PathVariable Long id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Notebook create(@Valid @RequestBody NotebookInput input) {
        return repository.save(new Notebook(input));
    }

    @PutMapping("/{id}")
    Notebook update(@PathVariable Long id, @Valid @RequestBody NotebookInput input) {
        Notebook notebook = get(id);
        notebook.update(input);
        return repository.save(notebook);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id) {
        if (!repository.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        repository.deleteById(id);
    }
}
