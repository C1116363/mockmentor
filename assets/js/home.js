/* =========================\n   MOBILE MENU
========================= */
const menuButton =
    document.getElementById("menuButton");
const navLinks =
    document.getElementById("navLinks");
menuButton.addEventListener(
    "click",
    function() {
navLinks.classList.toggle(
    "show"
);
const expanded =
    navLinks.classList.contains(
        "show"
    );
menuButton.setAttribute(
    "aria-expanded",
    expanded
);
    }
);
navLinks
    .querySelectorAll("a")
    .forEach(link => {
link.addEventListener(
    "click",
    function() {
        navLinks.classList.remove(
            "show"
        );
        menuButton.setAttribute(
            "aria-expanded",
            "false"
        );
    }
);
    });
/* =========================
   HOW IT WORKS
========================= */
const steps =
    document.querySelectorAll(".step");
const stepDetail =
    document.getElementById(
"stepDetail"
    );
const stepDescriptions = [
    "Start by choosing the skills and interview areas that matter for your target role.",
    "Practice coding questions, mock interviews and real-world scenarios to build confidence.",
    "Use feedback from practice sessions and mentors to identify and improve your weak areas.",
    "Put everything together and approach your placement interviews with better preparation and confidence."
];
steps.forEach(step => {
    step.addEventListener(
"click",
function() {
    steps.forEach(item => {
        item.classList.remove(
            "active"
        );
    });
    step.classList.add(
        "active"
    );
    const index =
        Number(
            step.dataset.step
        );
    stepDetail.textContent =
        stepDescriptions[index];
}
    );
});
/* =========================
   INITIAL RENDER
========================= */

/* =========================
   PROJECT SHOWCASE
========================= */
const projectShowcase = {
    pdf: {
        kicker: "DOCUMENT PRODUCTIVITY",
        title: "PDF Utility Suite",
        description: "Build conversion, merge, split, compression and secure PDF workflows with a team.",
        stack: "Python · FastAPI · React",
        workflow: "Pick tickets · Open a pull request · Get reviewed",
        skills: "File processing · APIs · Testing"
    },
    hrms: {
        kicker: "BUSINESS APPLICATIONS",
        title: "HRMS Platform",
        description: "Create an employee platform for attendance, leave requests, departments and role-based access.",
        stack: "Python · FastAPI · React · PostgreSQL",
        workflow: "Plan a sprint · Build a feature · Demo your work",
        skills: "CRUD APIs · Authentication · UI state"
    },
    scheme: {
        kicker: "AI / RAG",
        title: "Scheme Finder AI",
        description: "Help people discover relevant government schemes using trusted documents and source-backed AI answers.",
        stack: "Python · FastAPI · React · RAG",
        workflow: "Prepare data · Test answers · Improve retrieval",
        skills: "Embeddings · Search · Prompt design"
    }
};
document.querySelectorAll(".project-tab").forEach(tab => {
    tab.addEventListener("click", () => {
        const project = projectShowcase[tab.dataset.project];
        if (!project) return;
        document.querySelectorAll(".project-tab").forEach(item => {
            item.classList.toggle("active", item === tab);
            item.setAttribute("aria-selected", String(item === tab));
        });
        ["Kicker", "Title", "Description", "Stack", "Workflow", "Skills"].forEach(key => {
            const target = document.getElementById("project" + key);
            const value = project[key.toLowerCase()];
            if (target && value) target.textContent = value;
        });
    });
});
