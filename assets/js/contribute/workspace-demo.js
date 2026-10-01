import { tickets, team } from './demo-data.js';

const stages = [
  { label: 'Pending', action: 'Pick ticket' },
  { label: 'Picked', action: 'Submit work' },
  { label: 'In review', action: 'Preview approval' },
  { label: 'Done', action: 'Reviewed ✓' },
];

export function initWorkspaceDemo() {
  let view = 'frontend';
  const progress = new Map();
  const content = document.querySelector('#workspace-content');
  const message = document.querySelector('#demo-message');
  const tabs = [...document.querySelectorAll('[data-view]')];

  function render() {
    tabs.forEach(tab => tab.setAttribute('aria-pressed', String(tab.dataset.view === view)));
    if (view === 'team') {
      content.innerHTML = team.map(person => `
        <div class="team-person">
          <span class="avatar ${person.color}" aria-hidden="true">${person.initials}</span>
          <div><strong>${person.name}</strong><small>${person.role}</small></div>
          <span><i class="status-dot ${person.status.toLowerCase()}"></i>${person.status}</span>
        </div>`).join('');
      return;
    }
    content.innerHTML = `<div class="board-meta"><span>${view === 'frontend' ? 'Frontend' : 'Backend'} repository · 2 sample tickets</span><span><i class="status-dot"></i>Your sprint</span></div>` + tickets[view].map(ticket => {
      const stageIndex = progress.get(ticket.id) || 0;
      const stage = stages[stageIndex];
      return `<article class="ticket">
        <div class="ticket-top"><span>${ticket.id}</span><span class="ticket-status ${stageIndex === 3 ? 'done' : ''}">${stage.label}</span></div>
        <h3>${ticket.title}</h3>
        <div class="ticket-bottom"><span>${ticket.skill}</span><button type="button" class="ticket-action" data-ticket="${ticket.id}" aria-label="${stage.action}: ${ticket.title}" ${stageIndex === 3 ? 'disabled' : ''}>${stage.action}</button></div>
      </article>`;
    }).join('');
  }

  function changeView(next) {
    view = next;
    message.textContent = next === 'team'
      ? 'Sample teammates. In your project, this directory shows actual members and their availability.'
      : 'Try picking a sample ticket. This preview does not change a real project.';
    render();
  }

  tabs.forEach(tab => tab.addEventListener('click', () => changeView(tab.dataset.view)));
  content.addEventListener('click', event => {
    const button = event.target.closest('[data-ticket]');
    if (!button) return;
    const id = button.dataset.ticket;
    const next = Math.min((progress.get(id) || 0) + 1, 3);
    progress.set(id, next);
    render();
    message.textContent = [
      '',
      'Ticket picked! In the real workspace, you would now work in the project repository.',
      'Sample work submitted. A Tech Lead reviews real contributions; this demo lets you preview their approval.',
      'Review approved in this demo. In your project, only an authorized reviewer can approve your work.',
    ][next];
    const nextButton = content.querySelector(`[data-ticket="${id}"]`);
    if (!nextButton.disabled) nextButton.focus();
    else document.querySelector('#reset-demo').focus();
  });
  document.querySelector('#reset-demo').addEventListener('click', () => {
    progress.clear();
    changeView('frontend');
    message.textContent = 'Demo reset. Pick any sample ticket to start again.';
  });
  document.querySelectorAll('[data-preview-track]').forEach(button => {
    button.addEventListener('click', () => {
      changeView(button.dataset.previewTrack);
      document.querySelector('#workspace-preview').scrollIntoView({ block: 'center' });
      tabs.find(tab => tab.dataset.view === view).focus({ preventScroll: true });
    });
  });
  render();
}
