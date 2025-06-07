package ge.azvonov.notesai;

import ge.azvonov.notesai.db.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/projects")
public class ProjectController {
    private final ProjectRepository repository;

    @Autowired
    public ProjectController(ProjectRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("projects", repository.findAll());
        return "projects";
    }

    @PostMapping("/add")
    public String add(@RequestParam("name") String name) {
        if (StringUtils.hasText(name)) {
            Project p = new Project();
            p.setName(name);
            repository.save(p);
        }
        return "redirect:/projects";
    }

    @PostMapping("/delete")
    public String delete(@RequestParam("id") Long id) {
        repository.deleteById(id);
        return "redirect:/projects";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("project", repository.findById(id).orElseThrow());
        return "edit_project";
    }

    @PostMapping("/edit")
    public String edit(@RequestParam Long id, @RequestParam String name) {
        Project p = repository.findById(id).orElseThrow();
        p.setName(name);
        repository.save(p);
        return "redirect:/projects";
    }
}
