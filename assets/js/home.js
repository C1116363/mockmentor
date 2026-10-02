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
