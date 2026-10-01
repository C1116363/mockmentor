/* =========================
   MENTORS DATA
========================= */
const MENTORS = [
    {
name: "Chetan Dhumal",
role: "SDE at Leegality",
expertise:
    "Java, Spring Boot, React, MySQL",
topics: [
    "Java",
    "Full Stack"
],
bio:
    "Builds and ships full-stack products using Java, Spring Boot, React and MySQL."
    },
    {
name: "Rutuja Khose",
role:
    "Assistant Professor at Deogiri College",
expertise:
    "AI, ML, Python, SQL",
topics: [
    "AI / ML",
    "Python",
    "SQL"
],
bio:
    "Teaches AI and machine learning with a strong focus on fundamentals and practical interview preparation."
    },
    {
name: "Abhijeet Dhumal",
role:
    "Software Engineer at Electropneumatics",
expertise:
    "C, C++, Embedded C, IoT",
topics: [
    "Embedded",
    "IoT"
],
bio:
    "Works with embedded systems and IoT, helping candidates understand low-level programming and system fundamentals."
    }
];
/* =========================
   GET MENTOR CATEGORIES
========================= */
function getMentorCategories(mentor) {
    const text = (
mentor.name +
" " +
mentor.role +
" " +
mentor.expertise +
" " +
mentor.topics.join(" ")
    ).toLowerCase();
    const categories = [];
    if (
text.includes("java") ||
text.includes("spring")
    ) {
categories.push("Java");
    }
    if (
text.includes("react") ||
text.includes("frontend") ||
text.includes("full stack")
    ) {
categories.push("Frontend");
    }
    if (
text.includes("spring") ||
text.includes("backend") ||
text.includes("mysql") ||
text.includes("sql")
    ) {
categories.push("Backend");
    }
    if (
text.includes("career") ||
text.includes("interview") ||
text.includes("placement")
    ) {
categories.push("Career");
    }
    if (
text.includes("ai") ||
text.includes("ml") ||
text.includes("machine learning")
    ) {
categories.push("AI / ML");
    }
    if (
text.includes("embedded") ||
text.includes("iot") ||
text.includes("c++") ||
text.includes("embedded c")
    ) {
categories.push("Embedded");
    }
    return categories;
}
/* =========================
   CREATE MENTOR CARD
========================= */
function createMentorCard(mentor) {
    const initials = mentor.name
.split(" ")
.map(word => word[0])
.join("")
.slice(0, 2)
.toUpperCase();
    const tags = mentor.topics
.map(topic => {
    return `
        <span class="mentor-tag">
            ${topic}
        </span>
    `;
})
.join("");
    return `
<article class="mentor-card">
    <div class="mentor-top">
        <div class="mentor-avatar">
            ${initials}
        </div>
        <div>
            <h3>
                ${mentor.name}
            </h3>
            <div class="mentor-role">
                ${mentor.role}
            </div>
        </div>
    </div>
    <div class="mentor-expertise">
        ${mentor.expertise}
    </div>
    <div class="mentor-tags">
        ${tags}
    </div>
    <p class="mentor-bio">
        ${mentor.bio}
    </p>
</article>
    `;
}
/* =========================
   RENDER MENTORS
========================= */
function renderMentors(filter = "All") {
    const mentorGrid =
document.getElementById("mentorGrid");
    let mentors = MENTORS;
    if (filter !== "All") {
mentors = MENTORS.filter(mentor => {
    const categories =
        getMentorCategories(mentor);
    return categories.includes(filter);
});
    }
    if (mentors.length === 0) {
mentorGrid.innerHTML = `
    <div
        style="
            grid-column: 1 / -1;
            padding: 30px;
            text-align: center;
            color: #667085;
            background: white;
            border: 1px solid #e7eaf0;
            border-radius: 16px;
        "
    >
        No mentors available in this category yet.
    </div>
`;
return;
    }
    mentorGrid.innerHTML =
mentors
    .map(createMentorCard)
    .join("");
}
/* =========================
   MENTOR FILTER
========================= */
document
    .getElementById("mentorFilters")
    .addEventListener(
"click",
function(event) {
    const button =
        event.target.closest(
            ".filter-button"
        );
    if (!button) {
        return;
    }
    document
        .querySelectorAll(
            ".filter-button"
        )
        .forEach(btn => {
            btn.classList.remove(
                "active"
            );
        });
    button.classList.add("active");
    const filter =
        button.dataset.filter;
    renderMentors(filter);
}
    );
/* =========================
   MOBILE MENU
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
renderMentors("All");
