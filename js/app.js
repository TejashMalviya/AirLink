/* ==========================================================================
   AirLink — App Controller & Interactive Experiences
   - Web Audio Micro-Synth (Zero external files needed)
   - Phone Simulator (Chat + Walkie-Talkie Voice Call + Live Mini-Radar)
   - Jammer Blackout Stress-Test Calculator
   - Mesh Hop Range & Distance Calculator
   - Interactive FAQ Accordion
   - Camera QR Code Modal
   - Apple-style Scroll Reveal & Live Tickers
   ========================================================================== */

// --- 1. Synthesized Web Audio Effects (Zero Asset Dependency) ---
class AirLinkSoundEngine {
  constructor() {
    this.ctx = null;
    this.enabled = false; // Muted by default for respectful UX; 1-click toggle in nav
  }

  init() {
    if (!this.ctx) {
      const AudioCtx = window.AudioContext || window.webkitAudioContext;
      if (AudioCtx) this.ctx = new AudioCtx();
    }
  }

  toggle() {
    this.init();
    if (this.ctx && this.ctx.state === 'suspended') {
      this.ctx.resume();
    }
    this.enabled = !this.enabled;
    const btn = document.getElementById('sound-toggle-btn');
    if (btn) {
      if (this.enabled) {
        btn.classList.add('active');
        btn.innerHTML = `
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M11 5L6 9H2v6h4l5 4V5z"/><path d="M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"/></svg>
          <span class="sound-label">Sound: ON</span>
        `;
        this.playChirp();
      } else {
        btn.classList.remove('active');
        btn.innerHTML = `
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M11 5L6 9H2v6h4l5 4V5z"/><line x1="23" y1="9" x2="17" y2="15"/><line x1="17" y1="9" x2="23" y2="15"/></svg>
          <span class="sound-label">Sound: OFF</span>
        `;
      }
    }
  }

  playClick() {
    if (!this.enabled || !this.ctx) return;
    try {
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(800, this.ctx.currentTime);
      gain.gain.setValueAtTime(0.04, this.ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, this.ctx.currentTime + 0.04);
      osc.connect(gain);
      gain.connect(this.ctx.destination);
      osc.start();
      osc.stop(this.ctx.currentTime + 0.04);
    } catch (e) {}
  }

  playChirp() {
    if (!this.enabled || !this.ctx) return;
    try {
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();
      osc.type = 'triangle';
      osc.frequency.setValueAtTime(600, this.ctx.currentTime);
      osc.frequency.exponentialRampToValueAtTime(1400, this.ctx.currentTime + 0.08);
      gain.gain.setValueAtTime(0.05, this.ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, this.ctx.currentTime + 0.08);
      osc.connect(gain);
      gain.connect(this.ctx.destination);
      osc.start();
      osc.stop(this.ctx.currentTime + 0.08);
    } catch (e) {}
  }

  playCallTone(connected = false) {
    if (!this.enabled || !this.ctx) return;
    try {
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();
      osc.type = 'sine';
      if (connected) {
        osc.frequency.setValueAtTime(520, this.ctx.currentTime);
        osc.frequency.setValueAtTime(780, this.ctx.currentTime + 0.08);
      } else {
        osc.frequency.setValueAtTime(440, this.ctx.currentTime);
      }
      gain.gain.setValueAtTime(0.06, this.ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, this.ctx.currentTime + 0.18);
      osc.connect(gain);
      gain.connect(this.ctx.destination);
      osc.start();
      osc.stop(this.ctx.currentTime + 0.18);
    } catch (e) {}
  }
}

const SoundEngine = new AirLinkSoundEngine();

// --- 2. Master DOM Ready Initializer ---
document.addEventListener('DOMContentLoaded', () => {
  initScrollReveal();
  initSoundToggle();
  initPhoneSimulator();
  initJammerStressTest();
  initRangeCalculator();
  initFaqAccordion();
  initQrModal();
  initChecksumCopy();
  initStoryRadarCanvas();
  initLivePacketCounter();
  initSmoothScroll();
});

// --- Sound Button Hook ---
function initSoundToggle() {
  const btn = document.getElementById('sound-toggle-btn');
  if (btn) {
    btn.addEventListener('click', () => SoundEngine.toggle());
  }
}

// --- 3. Apple-Style Scroll Reveal Observer ---
function initScrollReveal() {
  const reveals = document.querySelectorAll('.reveal');
  if (!reveals.length) return;

  if ('IntersectionObserver' in window) {
    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('visible');
          observer.unobserve(entry.target);
        }
      });
    }, {
      threshold: 0.1,
      rootMargin: '0px 0px -40px 0px'
    });

    reveals.forEach(el => observer.observe(el));
  } else {
    reveals.forEach(el => el.classList.add('visible'));
  }
}

// --- 4. Interactive Phone Simulator (Chat + Walkie-Talkie Call + Radar) ---
function initPhoneSimulator() {
  // Mode Tabs Switcher
  const tabs = document.querySelectorAll('.mode-tab');
  const views = {
    chat: document.getElementById('view-chat'),
    call: document.getElementById('view-call'),
    radar: document.getElementById('view-radar')
  };

  tabs.forEach(tab => {
    tab.addEventListener('click', () => {
      SoundEngine.playClick();
      const mode = tab.getAttribute('data-mode');

      tabs.forEach(t => t.classList.remove('active'));
      tab.classList.add('active');

      Object.values(views).forEach(v => {
        if (v) v.classList.remove('active');
      });

      if (views[mode]) {
        views[mode].classList.add('active');
        if (mode === 'radar') drawMiniRadar();
      }
    });
  });

  // Mode 1: Chat Demo
  const chatInput = document.getElementById('phone-chat-input');
  const chatSend = document.getElementById('phone-chat-send');
  const chatStream = document.getElementById('phone-chat-stream');

  const peerReplies = [
    { text: "Received via 2 peer hops! Cellular tower outside is 100% dead, but AirLink is crystal clear.", route: "via Alex's hotspot · 1 hop" },
    { text: "Sub-20ms latency. No internet, no servers, jammers can't touch 5GHz Wi-Fi Direct.", route: "via Devin → Kiran · 2 hops" },
    { text: "Packets hopping hand to hand through the crowd. The human network never goes down.", route: "via Campus Relay · 2 hops" },
    { text: "Voice call quality is perfect. Want to start an offline walkie-talkie call?", route: "via Elena · direct 1 hop" }
  ];
  let replyIdx = 0;

  function sendChatMessage() {
    if (!chatInput || !chatStream) return;
    const text = chatInput.value.trim();
    if (!text) return;

    SoundEngine.playClick();

    const outBubble = document.createElement('div');
    outBubble.className = 'chat-bub out';
    outBubble.innerHTML = `
      ${escapeHtml(text)}
      <span class="route-tag">relayed via local mesh · 0 towers</span>
    `;
    chatStream.appendChild(outBubble);
    chatInput.value = '';
    chatStream.scrollTop = chatStream.scrollHeight;

    if (window.AirLink3D && typeof window.AirLink3D.sendPulse === 'function') {
      window.AirLink3D.sendPulse();
    }

    setTimeout(() => {
      const replyData = peerReplies[replyIdx % peerReplies.length];
      replyIdx++;

      const inBubble = document.createElement('div');
      inBubble.className = 'chat-bub in';
      inBubble.innerHTML = `
        ${escapeHtml(replyData.text)}
        <span class="route-tag">${escapeHtml(replyData.route)}</span>
      `;
      chatStream.appendChild(inBubble);
      chatStream.scrollTop = chatStream.scrollHeight;
      SoundEngine.playChirp();
    }, 850);
  }

  if (chatSend) chatSend.addEventListener('click', sendChatMessage);
  if (chatInput) {
    chatInput.addEventListener('keydown', (e) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        sendChatMessage();
      }
    });
  }

  // Mode 2: Real-Time Voice Call Simulation
  let isCalling = false;
  let callTimerInterval = null;
  let callSeconds = 0;
  let waveAnimInterval = null;

  const btnCallToggle = document.getElementById('btn-call-toggle');
  const callBtnText = document.getElementById('call-btn-text');
  const callStatus = document.getElementById('call-status-label');
  const callClock = document.getElementById('call-clock');
  const waveSticks = document.querySelectorAll('.wave-stick');

  if (btnCallToggle) {
    btnCallToggle.addEventListener('click', () => {
      isCalling = !isCalling;
      SoundEngine.playClick();

      if (isCalling) {
        // Start Call
        btnCallToggle.classList.add('hangup');
        if (callBtnText) callBtnText.textContent = "End Call";
        if (callStatus) callStatus.textContent = "Connecting P2P Audio...";
        SoundEngine.playCallTone(false);

        setTimeout(() => {
          if (!isCalling) return;
          if (callStatus) callStatus.textContent = "Connected · 2 Hops (Wi-Fi Direct)";
          SoundEngine.playCallTone(true);

          // Start Timer
          callSeconds = 0;
          clearInterval(callTimerInterval);
          callTimerInterval = setInterval(() => {
            callSeconds++;
            const m = String(Math.floor(callSeconds / 60)).padStart(2, '0');
            const s = String(callSeconds % 60).padStart(2, '0');
            if (callClock) callClock.textContent = `${m}:${s}`;
          }, 1000);

          // Animate Oscilloscope Waveform
          clearInterval(waveAnimInterval);
          waveAnimInterval = setInterval(() => {
            waveSticks.forEach(st => {
              const h = Math.floor(6 + Math.random() * 28);
              st.style.height = `${h}px`;
            });
          }, 120);
        }, 900);

      } else {
        // End Call
        btnCallToggle.classList.remove('hangup');
        if (callBtnText) callBtnText.textContent = "Start Test Call";
        if (callStatus) callStatus.textContent = "Call Ended · 0 Towers Used";
        clearInterval(callTimerInterval);
        clearInterval(waveAnimInterval);
        waveSticks.forEach(st => st.style.height = '12px');
        setTimeout(() => {
          if (!isCalling && callStatus) {
            callStatus.textContent = "Encrypted Relay via 2 Hops";
            if (callClock) callClock.textContent = "00:00";
          }
        }, 2200);
      }
    });
  }

  // Mode 3: Mini Radar Canvas Inside Phone
  function drawMiniRadar() {
    const canvas = document.getElementById('mini-radar-canvas');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const w = canvas.width;
    const h = canvas.height;
    const cx = w / 2;
    const cy = h / 2;

    ctx.fillStyle = '#141a24';
    ctx.fillRect(0, 0, w, h);

    // Rings
    ctx.strokeStyle = 'rgba(16, 185, 129, 0.25)';
    ctx.lineWidth = 1;
    [30, 60, 90].forEach(r => {
      ctx.beginPath();
      ctx.arc(cx, cy, r, 0, Math.PI * 2);
      ctx.stroke();
    });

    // Center
    ctx.fillStyle = '#ffffff';
    ctx.beginPath();
    ctx.arc(cx, cy, 4, 0, Math.PI * 2);
    ctx.fill();

    // Nearby Peers
    const miniPeers = [
      { x: cx - 42, y: cy - 35, name: "Alex", col: "#10b981" },
      { x: cx + 55, y: cy - 20, name: "Maya", col: "#06b6d4" },
      { x: cx + 30, y: cy + 50, name: "Elena", col: "#10b981" },
      { x: cx - 60, y: cy + 40, name: "Devin", col: "#06b6d4" }
    ];

    miniPeers.forEach(p => {
      ctx.strokeStyle = 'rgba(16, 185, 129, 0.3)';
      ctx.beginPath();
      ctx.moveTo(cx, cy);
      ctx.lineTo(p.x, p.y);
      ctx.stroke();

      ctx.fillStyle = p.col;
      ctx.beginPath();
      ctx.arc(p.x, p.y, 4, 0, Math.PI * 2);
      ctx.fill();

      ctx.font = '8px "JetBrains Mono", monospace';
      ctx.fillStyle = '#94a3b8';
      ctx.fillText(p.name, p.x + 6, p.y + 3);
    });
  }
}

// --- 5. Interactive Jammer Blackout Stress-Test ---
function initJammerStressTest() {
  const toggles = [
    document.getElementById('test-toggle-towers'),
    document.getElementById('test-toggle-fiber'),
    document.getElementById('test-toggle-sim'),
    document.getElementById('test-toggle-jammer')
  ];

  const barTrad = document.getElementById('bar-traditional');
  const valTrad = document.getElementById('val-traditional');
  const explanation = document.getElementById('stress-explanation-text');
  const badge = document.getElementById('stress-airlink-badge');

  function updateStressTest() {
    SoundEngine.playClick();
    const towersKilled = toggles[0] ? toggles[0].checked : false;
    const fiberCut = toggles[1] ? toggles[1].checked : false;
    const simConfiscated = toggles[2] ? toggles[2].checked : false;
    const jammerActive = toggles[3] ? toggles[3].checked : false;

    // Traditional apps fail if towers, fiber, or SIM are sabotaged
    const tradFailed = towersKilled || fiberCut || simConfiscated;

    if (tradFailed) {
      if (barTrad) barTrad.style.width = '0%';
      if (valTrad) {
        valTrad.textContent = '0% DEAD';
        valTrad.style.color = 'var(--red)';
      }
    } else {
      if (barTrad) barTrad.style.width = '100%';
      if (valTrad) {
        valTrad.textContent = '100% (Normal)';
        valTrad.style.color = 'var(--text-secondary)';
      }
    }

    // Dynamic Roast Text
    if (towersKilled && fiberCut && simConfiscated && jammerActive) {
      if (badge) badge.textContent = "100% IMMUNE · SWARM UNTOUCHED";
      if (explanation) {
        explanation.textContent = "All 4 sabotage mechanisms are active. While carrier-dependent apps suffer complete blackout, AirLink devices communicate over local 5GHz Wi-Fi Direct and BLE. The jammer wasted electricity; AirLink never relied on towers to begin with.";
      }
    } else if (jammerActive) {
      if (badge) badge.textContent = "100% IMMUNE (5GHz BYPASS)";
      if (explanation) {
        explanation.textContent = "Cellular jammers broadcast noise across carrier LTE frequencies (700MHz–2.6GHz). AirLink hops across 5.8 GHz & 2.4 GHz device-to-device Wi-Fi Direct, completely immune to standard radio denial attacks.";
      }
    } else if (simConfiscated) {
      if (badge) badge.textContent = "100% IMMUNE (CRYPTO ID)";
      if (explanation) {
        explanation.textContent = "Taking away SIM cards changes nothing for AirLink. Device identities are derived directly from cryptographic Ed25519 public keys. You cannot revoke what was never centralized.";
      }
    } else {
      if (badge) badge.textContent = "100% OPERATIONAL BY DESIGN";
      if (explanation) {
        explanation.textContent = "AirLink operates completely off-grid. Every smartphone running the app is an autonomous repeater node.";
      }
    }
  }

  toggles.forEach(t => {
    if (t) t.addEventListener('change', updateStressTest);
  });
}

// --- 6. Interactive Mesh Hop Range Calculator ---
function initRangeCalculator() {
  const slider = document.getElementById('range-slider');
  const countDisplay = document.getElementById('range-people-display');
  const distVal = document.getElementById('calc-distance-val');
  const hopsVal = document.getElementById('calc-hops-val');
  const latVal = document.getElementById('calc-latency-val');
  const coverageText = document.getElementById('calc-coverage-text');

  if (!slider) return;

  slider.addEventListener('input', () => {
    const people = parseInt(slider.value, 10);
    if (countDisplay) countDisplay.textContent = `${people} People`;

    let dist = 0.1;
    let hops = 1;
    let lat = 14;
    let desc = "";

    if (people <= 3) {
      dist = 0.1;
      hops = 1;
      lat = 12;
      desc = "Direct single-hop room or hall coverage";
    } else if (people <= 15) {
      dist = (people * 0.09).toFixed(1);
      hops = 2;
      lat = 16;
      desc = "Covers entire building or outdoor square";
    } else if (people <= 50) {
      dist = (people * 0.12).toFixed(1);
      hops = 4;
      lat = 22;
      desc = "Covers a full university campus or stadium";
    } else if (people <= 120) {
      dist = (people * 0.14).toFixed(1);
      hops = 5;
      lat = 26;
      desc = "Covers an entire neighborhood or valley";
    } else {
      dist = (people * 0.16).toFixed(1);
      hops = 7;
      lat = 30;
      desc = "Spans across an entire metropolitan city";
    }

    if (distVal) distVal.textContent = `${dist} km`;
    if (hopsVal) hopsVal.textContent = `${hops} Relays`;
    if (latVal) latVal.textContent = `~${lat} ms`;
    if (coverageText) coverageText.textContent = desc;
  });
}

// --- 7. FAQ Accordion Toggle ---
function initFaqAccordion() {
  const items = document.querySelectorAll('.faq-item');
  items.forEach(item => {
    const btn = item.querySelector('.faq-question');
    if (btn) {
      btn.addEventListener('click', () => {
        SoundEngine.playClick();
        const isOpen = item.classList.contains('active');
        items.forEach(i => i.classList.remove('active'));
        if (!isOpen) item.classList.add('active');
      });
    }
  });
}

// --- 8. QR Code Modal ---
function initQrModal() {
  const modal = document.getElementById('qr-modal');
  const btnOpen1 = document.getElementById('btn-open-qr-modal');
  const btnOpen2 = document.getElementById('btn-open-qr-modal-2');
  const btnClose = document.getElementById('btn-close-qr-modal');
  const canvasHolder = document.getElementById('qr-modal-canvas');

  function openModal() {
    SoundEngine.playClick();
    if (modal) modal.classList.add('active');
    if (canvasHolder && !canvasHolder.innerHTML.trim()) {
      canvasHolder.innerHTML = generateVectorQR();
    }
  }

  function closeModal() {
    SoundEngine.playClick();
    if (modal) modal.classList.remove('active');
  }

  if (btnOpen1) btnOpen1.addEventListener('click', openModal);
  if (btnOpen2) btnOpen2.addEventListener('click', openModal);
  if (btnClose) btnClose.addEventListener('click', closeModal);

  if (modal) {
    modal.addEventListener('click', (e) => {
      if (e.target === modal) closeModal();
    });
  }
}

// Generate sleek SVG QR code matrix pointing to AirLink APK
function generateVectorQR() {
  return `
    <svg viewBox="0 0 100 100" width="100%" height="100%" fill="#1d1d1f">
      <!-- Corners -->
      <rect x="6" y="6" width="26" height="26" rx="4" />
      <rect x="9" y="9" width="20" height="20" fill="#ffffff" rx="2" />
      <rect x="13" y="13" width="12" height="12" rx="1" />
      <rect x="16" y="16" width="6" height="6" fill="#10b981" />

      <rect x="68" y="6" width="26" height="26" rx="4" />
      <rect x="71" y="9" width="20" height="20" fill="#ffffff" rx="2" />
      <rect x="75" y="13" width="12" height="12" rx="1" />
      <rect x="78" y="16" width="6" height="6" fill="#10b981" />

      <rect x="6" y="68" width="26" height="26" rx="4" />
      <rect x="9" y="71" width="20" height="20" fill="#ffffff" rx="2" />
      <rect x="13" y="75" width="12" height="12" rx="1" />
      <rect x="16" y="78" width="6" height="6" fill="#10b981" />

      <!-- Center Logo Box -->
      <rect x="42" y="42" width="16" height="16" fill="#06b6d4" rx="4" />
      <circle cx="50" cy="50" r="4.5" fill="#ffffff" />
      <circle cx="50" cy="50" r="2" fill="#10b981" />

      <!-- Data Dots -->
      <rect x="40" y="8" width="6" height="6" />
      <rect x="52" y="16" width="6" height="6" />
      <rect x="38" y="24" width="6" height="6" />
      <rect x="12" y="44" width="6" height="6" />
      <rect x="24" y="52" width="6" height="6" />
      <rect x="68" y="44" width="6" height="6" />
      <rect x="80" y="52" width="6" height="6" />
      <rect x="42" y="68" width="6" height="6" />
      <rect x="54" y="76" width="6" height="6" />
      <rect x="68" y="72" width="6" height="6" />
      <rect x="80" y="80" width="6" height="6" />
    </svg>
  `;
}

// --- 9. SHA-256 Checksum Copy Button ---
function initChecksumCopy() {
  const btn = document.getElementById('copy-hash-btn');
  if (btn) {
    btn.addEventListener('click', () => {
      const hash = "6ddba9501d2aae73ae3fa9c298e498d7683735554cd21af4abc2d763b9bf01ca";
      navigator.clipboard.writeText(hash).then(() => {
        SoundEngine.playClick();
        const orig = btn.textContent;
        btn.textContent = "COPIED ✓";
        btn.style.background = "var(--emerald)";
        setTimeout(() => {
          btn.textContent = orig;
          btn.style.background = "var(--text-primary)";
        }, 2000);
      });
    });
  }
}

// --- 10. Live Radar Canvas in Story 1 ---
function initStoryRadarCanvas() {
  const container = document.getElementById('story-mesh-1');
  if (!container) return;

  const canvas = document.createElement('canvas');
  container.appendChild(canvas);
  const ctx = canvas.getContext('2d');

  function resize() {
    canvas.width = container.clientWidth || 380;
    canvas.height = container.clientHeight || 280;
  }
  resize();
  window.addEventListener('resize', resize);

  let angle = 0;
  const peers = [
    { x: 0.35, y: 0.38, name: "Alex", hops: 1, color: "#10b981" },
    { x: 0.68, y: 0.28, name: "Maya", hops: 1, color: "#06b6d4" },
    { x: 0.75, y: 0.68, name: "Devin", hops: 2, color: "#10b981" },
    { x: 0.25, y: 0.72, name: "Kiran", hops: 2, color: "#06b6d4" },
    { x: 0.82, y: 0.45, name: "Elena", hops: 3, color: "#34d399" }
  ];

  function draw() {
    const w = canvas.width;
    const h = canvas.height;
    const cx = w / 2;
    const cy = h / 2;
    const maxR = Math.min(w, h) * 0.44;

    ctx.fillStyle = '#1d1d1f';
    ctx.fillRect(0, 0, w, h);

    // Subtle Grid Rings
    ctx.strokeStyle = 'rgba(16, 185, 129, 0.18)';
    ctx.lineWidth = 1;
    for (let r = maxR * 0.33; r <= maxR; r += maxR * 0.33) {
      ctx.beginPath();
      ctx.arc(cx, cy, r, 0, Math.PI * 2);
      ctx.stroke();
    }

    // Crosshairs
    ctx.beginPath();
    ctx.moveTo(cx - maxR, cy);
    ctx.lineTo(cx + maxR, cy);
    ctx.moveTo(cx, cy - maxR);
    ctx.lineTo(cx, cy + maxR);
    ctx.stroke();

    // Radar Sweep Cone
    angle += 0.035;
    const grad = ctx.createRadialGradient(cx, cy, 0, cx, cy, maxR);
    grad.addColorStop(0, 'rgba(16, 185, 129, 0.0)');
    grad.addColorStop(1, 'rgba(16, 185, 129, 0.18)');

    ctx.save();
    ctx.beginPath();
    ctx.moveTo(cx, cy);
    ctx.arc(cx, cy, maxR, angle - 0.4, angle);
    ctx.closePath();
    ctx.fillStyle = grad;
    ctx.fill();

    // Sweep Leading Line
    ctx.beginPath();
    ctx.moveTo(cx, cy);
    ctx.lineTo(cx + Math.cos(angle) * maxR, cy + Math.sin(angle) * maxR);
    ctx.strokeStyle = '#34d399';
    ctx.lineWidth = 1.5;
    ctx.stroke();
    ctx.restore();

    // Center Node (You)
    ctx.fillStyle = '#ffffff';
    ctx.beginPath();
    ctx.arc(cx, cy, 5, 0, Math.PI * 2);
    ctx.fill();

    // Mesh Links between peers
    ctx.strokeStyle = 'rgba(16, 185, 129, 0.25)';
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(cx, cy);
    ctx.lineTo(w * peers[0].x, h * peers[0].y);
    ctx.lineTo(w * peers[1].x, h * peers[1].y);
    ctx.lineTo(w * peers[2].x, h * peers[2].y);
    ctx.moveTo(cx, cy);
    ctx.lineTo(w * peers[3].x, h * peers[3].y);
    ctx.lineTo(w * peers[4].x, h * peers[4].y);
    ctx.stroke();

    // Draw Peer Blips
    peers.forEach(p => {
      const px = w * p.x;
      const py = h * p.y;

      ctx.strokeStyle = p.color;
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.arc(px, py, 7 + Math.sin(angle * 2) * 2, 0, Math.PI * 2);
      ctx.stroke();

      ctx.fillStyle = p.color;
      ctx.beginPath();
      ctx.arc(px, py, 3.5, 0, Math.PI * 2);
      ctx.fill();

      ctx.font = '9px "JetBrains Mono", monospace';
      ctx.fillStyle = '#94a3b8';
      ctx.fillText(`${p.name} (${p.hops}h)`, px + 8, py + 3);
    });

    requestAnimationFrame(draw);
  }

  draw();
}

// --- 11. Live Packet Counter ---
function initLivePacketCounter() {
  const counterEl = document.getElementById('live-packet-counter');
  if (!counterEl) return;

  let currentCount = 1240;
  setInterval(() => {
    currentCount += Math.floor(1 + Math.random() * 3);
    counterEl.textContent = currentCount.toLocaleString();
  }, 1600);
}

// --- 12. Smooth Scroll Anchor Navigation ---
function initSmoothScroll() {
  document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function (e) {
      const targetId = this.getAttribute('href');
      if (targetId === '#') return;

      const targetEl = document.querySelector(targetId);
      if (targetEl) {
        e.preventDefault();
        targetEl.scrollIntoView({
          behavior: 'smooth',
          block: 'start'
        });
      }
    });
  });
}

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
