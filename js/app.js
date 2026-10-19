/* ==========================================================================
   AirLink — Client Interactions, App Emulators & Live Tickers
   ========================================================================== */

document.addEventListener('DOMContentLoaded', () => {
  initScreenTabs();
  initChatDemo();
  initLiveTicker();
  initDownloadHelpers();
});

// --- 1. Minimalist Screen Tab Switcher ---
function initScreenTabs() {
  const cards = document.querySelectorAll('.feature-pill-card');
  const screens = document.querySelectorAll('.app-screen-layer');

  cards.forEach(card => {
    card.addEventListener('click', () => {
      const target = card.getAttribute('data-feature');

      cards.forEach(c => c.classList.remove('active'));
      screens.forEach(s => s.classList.remove('active'));

      card.classList.add('active');
      const activeScreen = document.getElementById(`screen-${target}`);
      if (activeScreen) activeScreen.classList.add('active');

      // Trigger small 3D broadcast ping when switching to demonstrate mesh reactivity
      if (window.AirLink3D && window.AirLink3D.broadcastVoicePing) {
        window.AirLink3D.broadcastVoicePing();
      }
    });
  });
}

// --- 2. Interactive Offline Chat Demo ---
function initChatDemo() {
  const input = document.getElementById('chat-text-input');
  const sendBtn = document.getElementById('chat-send-trigger');
  const msgContainer = document.getElementById('chat-stream-box');

  function sendMessage() {
    if (!input || !msgContainer) return;
    const text = input.value.trim();
    if (!text) return;

    // Append user message
    const bubble = document.createElement('div');
    bubble.className = 'bubble-msg me';
    bubble.textContent = text;
    msgContainer.appendChild(bubble);
    input.value = '';
    msgContainer.scrollTop = msgContainer.scrollHeight;

    // Simulate multi-hop mesh response from nearby human node
    setTimeout(() => {
      const responses = [
        "Voice & chat arriving through 2 peer hotspots. No towers needed!",
        "Cellular signal is jammed outside, but AirLink mesh is crystal clear.",
        "Packets hopping from phone to phone. The human network never goes down.",
        "Relay route verified. Encryption intact."
      ];
      const reply = responses[Math.floor(Math.random() * responses.length)];

      const incoming = document.createElement('div');
      incoming.className = 'bubble-msg them';
      incoming.textContent = reply;
      msgContainer.appendChild(incoming);
      msgContainer.scrollTop = msgContainer.scrollHeight;

      if (window.AirLink3D) window.AirLink3D.broadcastVoicePing();
    }, 900);
  }

  if (sendBtn) sendBtn.addEventListener('click', sendMessage);
  if (input) {
    input.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') sendMessage();
    });
  }
}

// --- 3. Live Ticker / Hop Physics Simulation ---
function initLiveTicker() {
  let packetsCounter = 1240;
  setInterval(() => {
    packetsCounter += Math.floor(1 + Math.random() * 3);
    const counterEl = document.getElementById('live-packet-count');
    if (counterEl) counterEl.innerText = packetsCounter.toLocaleString();
  }, 1600);
}

// --- 4. Download Helpers & QR Code ---
function initDownloadHelpers() {
  const copyBtn = document.getElementById('copy-hash-btn');
  if (copyBtn) {
    copyBtn.addEventListener('click', () => {
      const hash = "6ddba9501d2aae73ae3fa9c298e498d7683735554cd21af4abc2d763b9bf01ca";
      navigator.clipboard.writeText(hash).then(() => {
        const orig = copyBtn.innerText;
        copyBtn.innerText = "COPIED";
        setTimeout(() => { copyBtn.innerText = orig; }, 1800);
      });
    });
  }

  // Render clean high-res QR code into the holder
  const qrWrap = document.getElementById('qr-code-canvas');
  if (qrWrap) {
    qrWrap.innerHTML = `
      <svg viewBox="0 0 100 100" width="100%" height="100%" fill="#05070a">
        <rect x="6" y="6" width="26" height="26" fill="#05070a" rx="4" />
        <rect x="9" y="9" width="20" height="20" fill="#ffffff" rx="2" />
        <rect x="13" y="13" width="12" height="12" fill="#05070a" rx="1" />
        <rect x="16" y="16" width="6" height="6" fill="#10b981" />

        <rect x="68" y="6" width="26" height="26" fill="#05070a" rx="4" />
        <rect x="71" y="9" width="20" height="20" fill="#ffffff" rx="2" />
        <rect x="75" y="13" width="12" height="12" fill="#05070a" rx="1" />
        <rect x="78" y="16" width="6" height="6" fill="#10b981" />

        <rect x="6" y="68" width="26" height="26" fill="#05070a" rx="4" />
        <rect x="9" y="71" width="20" height="20" fill="#ffffff" rx="2" />
        <rect x="13" y="75" width="12" height="12" fill="#05070a" rx="1" />
        <rect x="16" y="78" width="6" height="6" fill="#10b981" />

        <rect x="42" y="10" width="8" height="8" fill="#05070a" />
        <rect x="52" y="18" width="6" height="6" fill="#05070a" />
        <rect x="38" y="24" width="6" height="6" fill="#05070a" />

        <rect x="42" y="42" width="16" height="16" fill="#06b6d4" rx="4" />
        <circle cx="50" cy="50" r="4.5" fill="#ffffff" />
        <circle cx="50" cy="50" r="2" fill="#10b981" />

        <rect x="12" y="44" width="8" height="8" fill="#05070a" />
        <rect x="22" y="50" width="6" height="6" fill="#05070a" />
        <rect x="68" y="44" width="8" height="8" fill="#05070a" />
        <rect x="78" y="52" width="6" height="6" fill="#05070a" />

        <rect x="42" y="68" width="8" height="8" fill="#05070a" />
        <rect x="54" y="76" width="6" height="6" fill="#05070a" />
        <rect x="68" y="72" width="8" height="8" fill="#05070a" />
        <rect x="80" y="80" width="6" height="6" fill="#05070a" />
      </svg>
    `;
  }
}
