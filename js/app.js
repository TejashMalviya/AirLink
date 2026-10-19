/* ==========================================================================
   AirLink - Interactive Application Controller & UI Logic
   Includes interactive phone mockup, web audio synth, jammer calc, FAQ
   ========================================================================== */

// --- 1. Synthesized Web Audio Effects (Zero External Files Needed) ---
class AudioController {
  constructor() {
    this.ctx = null;
    this.isMuted = true; // start muted by default for user comfort
  }

  init() {
    if (!this.ctx) {
      const AudioCtx = window.AudioContext || window.webkitAudioContext;
      if (AudioCtx) this.ctx = new AudioCtx();
    }
  }

  toggleMute() {
    this.init();
    if (this.ctx && this.ctx.state === 'suspended') {
      this.ctx.resume();
    }
    this.isMuted = !this.isMuted;
    const btn = document.getElementById('sound-toggle-btn');
    if (btn) {
      btn.innerHTML = this.isMuted
        ? '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 5L6 9H2v6h4l5 4V5z"/><line x1="23" y1="9" x2="17" y2="15"/><line x1="17" y1="9" x2="23" y2="15"/></svg>'
        : '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 5L6 9H2v6h4l5 4V5z"/><path d="M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"/></svg>';
      btn.title = this.isMuted ? "Enable Sound Effects" : "Mute Sound Effects";
    }
  }

  playPacketChirp() {
    if (this.isMuted || !this.ctx) return;
    try {
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(880, this.ctx.currentTime);
      osc.frequency.exponentialRampToValueAtTime(1760, this.ctx.currentTime + 0.08);

      gain.gain.setValueAtTime(0.04, this.ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, this.ctx.currentTime + 0.08);

      osc.connect(gain);
      gain.connect(this.ctx.destination);
      osc.start();
      osc.stop(this.ctx.currentTime + 0.08);
    } catch (e) { }
  }

  playJammerAlarm() {
    if (this.isMuted || !this.ctx) return;
    try {
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();
      osc.type = 'sawtooth';
      osc.frequency.setValueAtTime(320, this.ctx.currentTime);
      osc.frequency.linearRampToValueAtTime(220, this.ctx.currentTime + 0.25);

      gain.gain.setValueAtTime(0.08, this.ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, this.ctx.currentTime + 0.3);

      osc.connect(gain);
      gain.connect(this.ctx.destination);
      osc.start();
      osc.stop(this.ctx.currentTime + 0.3);
    } catch (e) { }
  }

  playClick() {
    if (this.isMuted || !this.ctx) return;
    try {
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();
      osc.type = 'triangle';
      osc.frequency.setValueAtTime(600, this.ctx.currentTime);
      gain.gain.setValueAtTime(0.03, this.ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, this.ctx.currentTime + 0.04);
      osc.connect(gain);
      gain.connect(this.ctx.destination);
      osc.start();
      osc.stop(this.ctx.currentTime + 0.04);
    } catch (e) { }
  }
}

window.AirLinkAudio = new AudioController();

// --- 2. Interactive Phone Mockup Tab Switcher ---
function initPhoneMockup() {
  const tabs = document.querySelectorAll('.screen-tab-btn');
  const views = document.querySelectorAll('.screen-view');

  tabs.forEach((tab) => {
    tab.addEventListener('click', () => {
      window.AirLinkAudio.playClick();
      const targetScreen = tab.getAttribute('data-screen');

      tabs.forEach((t) => t.classList.remove('active'));
      views.forEach((v) => v.classList.remove('active'));

      tab.classList.add('active');
      const activeView = document.getElementById(`view-${targetScreen}`);
      if (activeView) activeView.classList.add('active');
    });
  });

  // Simulated Chat Demo inside phone mockup
  const chatInput = document.getElementById('mock-chat-input');
  const chatSendBtn = document.getElementById('mock-chat-send');
  const chatMessagesList = document.getElementById('mock-chat-messages');

  function sendMockMessage() {
    if (!chatInput || !chatMessagesList) return;
    const text = chatInput.value.trim();
    if (!text) return;

    window.AirLinkAudio.playClick();

    // Append outgoing bubble
    const outBubble = document.createElement('div');
    outBubble.className = 'chat-bubble outgoing';
    outBubble.innerHTML = `<div>${escapeHtml(text)}</div>
      <div class="chat-meta"><span>Relayed • 0 towers</span> <span>✓✓</span></div>`;
    chatMessagesList.appendChild(outBubble);
    chatInput.value = '';
    chatMessagesList.scrollTop = chatMessagesList.scrollHeight;

    // Simulate multi-hop reply from peer
    setTimeout(() => {
      const replies = [
        "Received loud and clear via 2 peer hops! Zero cellular needed 🚀",
        "Signal hopping strong through the crowd hotspot!",
        "They blocked the cellular network here, but AirLink packet arrived in 14ms! 💪",
        "Encrypted Ed25519 payload verified. Mesh audio call ready?"
      ];
      const replyText = replies[Math.floor(Math.random() * replies.length)];

      const inBubble = document.createElement('div');
      inBubble.className = 'chat-bubble incoming';
      inBubble.innerHTML = `<div>${replyText}</div>
        <div class="chat-meta"><span>Elena (via Devin's Hotspot)</span> <span>1-hop</span></div>`;
      chatMessagesList.appendChild(inBubble);
      chatMessagesList.scrollTop = chatMessagesList.scrollHeight;
      window.AirLinkAudio.playPacketChirp();
    }, 1200);
  }

  if (chatSendBtn) chatSendBtn.addEventListener('click', sendMockMessage);
  if (chatInput) {
    chatInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') sendMockMessage();
    });
  }

  // Live Telemetry Ticker Simulation in Diagnostics View
  let packetsRelayed = 3940;
  let packetsSent = 1480;
  setInterval(() => {
    packetsRelayed += Math.floor(1 + Math.random() * 3);
    packetsSent += Math.floor(Math.random() * 2);

    const relEl = document.getElementById('mock-stat-relayed');
    const sentEl = document.getElementById('mock-stat-sent');
    if (relEl) relEl.innerText = packetsRelayed.toLocaleString();
    if (sentEl) sentEl.innerText = packetsSent.toLocaleString();
  }, 1800);
}

// --- 3. Interactive Jammer Calculator (Mockery Engine) ---
function initJammerCalculator() {
  const toggles = [
    document.getElementById('calc-toggle-cellular'),
    document.getElementById('calc-toggle-fiber'),
    document.getElementById('calc-toggle-sim'),
    document.getElementById('calc-toggle-jammer')
  ];

  const meter = document.getElementById('calc-score-meter');
  const verdict = document.getElementById('calc-verdict-title');
  const explanation = document.getElementById('calc-explanation-text');

  function recalculate() {
    window.AirLinkAudio.playClick();
    const isCellularKilled = toggles[0] ? toggles[0].checked : false;
    const isFiberCut = toggles[1] ? toggles[1].checked : false;
    const isSimRemoved = toggles[2] ? toggles[2].checked : false;
    const isRfJammed = toggles[3] ? toggles[3].checked : false;

    // AirLink ALWAYS stays 100% operational because it runs on peer-to-peer Wi-Fi Direct and BLE
    if (meter) meter.innerText = '100%';

    let roast = "";
    if (isCellularKilled && isFiberCut && isSimRemoved && isRfJammed) {
      verdict.innerText = "CLOWN STATUS ACHIEVED: AIRLINK STILL WORKING.";
      roast = "They spent $20,000 shutting down telecom towers, pulling fiber optics, confiscating SIMs, and running RF jammers. Meanwhile, AirLink is calmly chatting and calling over direct 5GHz device-to-device hops. Jammers literally achieved zero.";
    } else if (isRfJammed) {
      verdict.innerText = "JAMMER POWERLESS: LOCAL HOPS INTACT.";
      roast = "Cheap jammers only target cellular bands (B3/B40). AirLink dynamically hops across Wi-Fi Direct (5GHz/2.4GHz) and Bluetooth LE. Nice try, keep wasting your electricity.";
    } else if (isCellularKilled || isFiberCut) {
      verdict.innerText = "ZERO IMPACT: TOWERLESS MESH RUNNING.";
      roast = "Towers and ISPs are a 1990s legacy design. AirLink forms a living swarm network between people. As long as humans stand within 70m of each other, your call goes through.";
    } else {
      verdict.innerText = "100% OPERATIONAL BY DESIGN.";
      roast = "AirLink operates completely off-grid. No cloud login, no phone number verification, no telemetry, no single point of failure.";
    }

    if (explanation) explanation.innerText = roast;
  }

  toggles.forEach((t) => {
    if (t) t.addEventListener('change', recalculate);
  });
}

// --- 4. Sarcastic FAQ Accordion ---
function initFAQ() {
  const faqItems = document.querySelectorAll('.faq-item');
  faqItems.forEach((item) => {
    const question = item.querySelector('.faq-question');
    if (question) {
      question.addEventListener('click', () => {
        window.AirLinkAudio.playClick();
        const isOpen = item.classList.contains('open');
        faqItems.forEach((i) => i.classList.remove('open'));
        if (!isOpen) {
          item.classList.add('open');
        }
      });
    }
  });
}

// --- 5. Download Center & SHA-256 Copy ---
function initDownloadCenter() {
  const copyBtn = document.getElementById('btn-copy-checksum');
  if (copyBtn) {
    copyBtn.addEventListener('click', () => {
      const hash = "6ddba9501d2aae73ae3fa9c298e498d7683735554cd21af4abc2d763b9bf01ca";
      navigator.clipboard.writeText(hash).then(() => {
        const orig = copyBtn.innerText;
        copyBtn.innerText = "COPIED!";
        window.AirLinkAudio.playClick();
        setTimeout(() => { copyBtn.innerText = orig; }, 2000);
      });
    });
  }

  // Generate lightweight SVG QR code dynamically for the APK download
  const qrBox = document.getElementById('qr-canvas-holder');
  if (qrBox) {
    // Generate clean visual representation of QR matrix pointing to APK
    qrBox.innerHTML = generateCleanQR();
  }
}

// Generate sleek SVG QR code representation
function generateCleanQR() {
  return `<svg viewBox="0 0 100 100" width="100%" height="100%" fill="#070a0f">
    <!-- QR Finder Pattern Top-Left -->
    <rect x="5" y="5" width="28" height="28" fill="#070a0f" rx="3" />
    <rect x="8" y="8" width="22" height="22" fill="#ffffff" rx="2" />
    <rect x="12" y="12" width="14" height="14" fill="#070a0f" rx="1" />
    <rect x="15" y="15" width="8" height="8" fill="#10b981" />
    
    <!-- QR Finder Pattern Top-Right -->
    <rect x="67" y="5" width="28" height="28" fill="#070a0f" rx="3" />
    <rect x="70" y="8" width="22" height="22" fill="#ffffff" rx="2" />
    <rect x="74" y="12" width="14" height="14" fill="#070a0f" rx="1" />
    <rect x="77" y="15" width="8" height="8" fill="#10b981" />

    <!-- QR Finder Pattern Bottom-Left -->
    <rect x="5" y="67" width="28" height="28" fill="#070a0f" rx="3" />
    <rect x="8" y="70" width="22" height="22" fill="#ffffff" rx="2" />
    <rect x="12" y="74" width="14" height="14" fill="#070a0f" rx="1" />
    <rect x="15" y="77" width="8" height="8" fill="#10b981" />

    <!-- Data Blocks -->
    <rect x="38" y="8" width="6" height="6" fill="#070a0f" />
    <rect x="48" y="14" width="6" height="6" fill="#070a0f" />
    <rect x="40" y="22" width="6" height="6" fill="#070a0f" />
    <rect x="52" y="24" width="6" height="6" fill="#070a0f" />

    <rect x="10" y="40" width="6" height="6" fill="#070a0f" />
    <rect x="20" y="46" width="6" height="6" fill="#070a0f" />
    <rect x="28" y="38" width="6" height="6" fill="#070a0f" />

    <rect x="40" y="40" width="20" height="20" fill="#06b6d4" rx="4" />
    <!-- AirLink mini icon in center -->
    <circle cx="50" cy="50" r="6" fill="#ffffff" />
    <circle cx="50" cy="50" r="3" fill="#10b981" />

    <rect x="68" y="42" width="6" height="6" fill="#070a0f" />
    <rect x="78" y="48" width="6" height="6" fill="#070a0f" />
    <rect x="86" y="38" width="6" height="6" fill="#070a0f" />

    <rect x="40" y="68" width="6" height="6" fill="#070a0f" />
    <rect x="50" y="74" width="6" height="6" fill="#070a0f" />
    <rect x="44" y="84" width="6" height="6" fill="#070a0f" />
    <rect x="68" y="70" width="6" height="6" fill="#070a0f" />
    <rect x="78" y="78" width="6" height="6" fill="#070a0f" />
    <rect x="88" y="84" width="6" height="6" fill="#070a0f" />
  </svg>`;
}

// Utility for safe HTML strings
function escapeHtml(str) {
  return str.replace(/[&<>'"]/g, 
    tag => ({
      '&': '&amp;',
      '<': '&lt;',
      '>': '&gt;',
      "'": '&#39;',
      '"': '&quot;'
    }[tag] || tag)
  );
}

// --- 6. Mobile Navigation Toggle ---
function initMobileNav() {
  const toggle = document.querySelector('.mobile-nav-toggle');
  const navLinks = document.querySelector('.nav-links');
  if (toggle && navLinks) {
    toggle.addEventListener('click', () => {
      const isVisible = navLinks.style.display === 'flex';
      navLinks.style.display = isVisible ? 'none' : 'flex';
      if (!isVisible) {
        navLinks.style.flexDirection = 'column';
        navLinks.style.position = 'absolute';
        navLinks.style.top = '74px';
        navLinks.style.left = '0';
        navLinks.style.right = '0';
        navLinks.style.background = '#090c11';
        navLinks.style.padding = '24px';
        navLinks.style.borderBottom = '1px solid rgba(255,255,255,0.1)';
      }
    });
  }
}

// Initialize on DOM load
document.addEventListener('DOMContentLoaded', () => {
  initPhoneMockup();
  initJammerCalculator();
  initFAQ();
  initDownloadCenter();
  initMobileNav();

  const soundBtn = document.getElementById('sound-toggle-btn');
  if (soundBtn) {
    soundBtn.addEventListener('click', () => {
      window.AirLinkAudio.toggleMute();
    });
  }
});
