import { CONTRIBUTE_APP_URL } from './config.js';

export function initAppLinks() {
  const dialog = document.querySelector('#app-dialog');
  dialog.querySelector('button').addEventListener('click', () => dialog.close());
  let destination;
  try {
    const url = new URL(CONTRIBUTE_APP_URL);
    if (url.protocol === 'https:') destination = url.href;
  } catch { /* Enrollment stays unavailable until a valid destination is configured. */ }

  document.querySelectorAll('[data-open-app]').forEach(button => {
    button.addEventListener('click', () => {
      if (destination) window.location.assign(destination);
      else dialog.showModal();
    });
  });
}
