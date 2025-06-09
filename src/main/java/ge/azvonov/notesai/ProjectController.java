package ge.azvonov.notesai;

import ge.azvonov.notesai.db.ProjectRepository;
import ge.azvonov.notesai.db.UserRepository;
import ge.azvonov.notesai.AppUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/projects")
public class ProjectController {
    private final ProjectRepository repository;
    private final UserRepository userRepository;

    @Autowired
    public ProjectController(ProjectRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    private AppUser currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName()).orElseThrow();
    }

    @GetMapping
    public String list(Model model) {
        AppUser user = currentUser();
        model.addAttribute("projects", repository.findByUser(user));
        return "projects";
    }

    @PostMapping("/add")
    public String add(@RequestParam("name") String name) {
        if (StringUtils.hasText(name)) {
            Project p = new Project();
            p.setName(name);
            p.setUser(currentUser());
            repository.save(p);
        }
        return "redirect:/projects";
    }

    @PostMapping("/delete")
    public String delete(@RequestParam("id") Long id) {
        AppUser user = currentUser();
        repository.findById(id).filter(p -> p.getUser().equals(user)).ifPresent(repository::delete);
        return "redirect:/projects";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        AppUser user = currentUser();
        Project p = repository.findById(id).orElseThrow();
        if (!p.getUser().equals(user)) {
            return "redirect:/projects";
        }
        model.addAttribute("project", p);
        return "edit_project";
    }

    @PostMapping("/edit")
    public String edit(@RequestParam Long id, @RequestParam String name) {
        AppUser user = currentUser();
        Project p = repository.findById(id).orElseThrow();
        if (p.getUser().equals(user)) {
            p.setName(name);
            repository.save(p);
        }
        return "redirect:/projects";
    }
}
