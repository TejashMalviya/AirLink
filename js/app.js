/* ==========================================================================
   AirLink — App Controller & Interactive Experiences
   Handles: Scroll Reveal, Phone Chat Demo, Live Radar Canvas, Live Tickers
   ========================================================================== */

document.addEventListener('DOMContentLoaded', () => {
  initScrollReveal();
  initPhoneChatDemo();
  initLivePacketCounter();
  initStoryRadarCanvas();
  initSmoothScroll();
});

// --- 1. Apple-Style Scroll Reveal Observer ---
function initScrollReveal() {
  const reveals = document.querySelectorAll('.reveal');
  if (!reveals.length) return;

  if ('IntersectionObserver' in window) {
    const observer = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('visible');
          // Once visible, keep it visible for smooth natural reading
          observer.unobserve(entry.target);
        }
      });
    }, {
      threshold: 0.12,
      rootMargin: '0px 0px -40px 0px'
    });

    reveals.forEach(el => observer.observe(el));
  } else {
    // Fallback for older browsers
    reveals.forEach(el => el.classList.add('visible'));
  }
}

// --- 2. Interactive Phone Chat Demo ---
function initPhoneChatDemo() {
  const input = document.getElementById('phone-chat-input');
  const sendBtn = document.getElementById('phone-chat-send');
  const stream = document.getElementById('phone-chat-stream');

  if (!input || !sendBtn || !stream) return;

  const peerReplies = [
    { text: "Received via 2 peer hops! Cellular tower outside is 100% dead, but AirLink is crystal clear.", route: "via Alex's hotspot · 1 hop" },
    { text: "Sub-20ms latency. No internet, no servers, jammers can't touch 5GHz Wi-Fi Direct.", route: "via Devin → Kiran · 2 hops" },
    { text: "Packets hopping hand to hand through the crowd. The human network never goes down.", route: "via Campus Relay · 2 hops" },
    { text: "Voice call quality is perfect. Want to start an offline audio call?", route: "via Elena · direct 1 hop" }
  ];

  let replyIndex = 0;

  function sendMessage() {
    const text = input.value.trim();
    if (!text) return;

    // 1. Append user message
    const outBubble = document.createElement('div');
    outBubble.className = 'chat-bub out';
    outBubble.innerHTML = `
      ${escapeHtml(text)}
      <span class="route-tag">relayed via local mesh · 0 towers</span>
    `;
    stream.appendChild(outBubble);
    input.value = '';
    stream.scrollTop = stream.scrollHeight;

    // Trigger visual pulse in sandbox if present
    if (window.AirLink3D && typeof window.AirLink3D.sendPulse === 'function') {
      window.AirLink3D.sendPulse();
    }

    // 2. Simulate multi-hop reply after 800ms
    setTimeout(() => {
      const replyData = peerReplies[replyIndex % peerReplies.length];
      replyIndex++;

      const inBubble = document.createElement('div');
      inBubble.className = 'chat-bub in';
      inBubble.innerHTML = `
        ${escapeHtml(replyData.text)}
        <span class="route-tag">${escapeHtml(replyData.route)}</span>
      `;
      stream.appendChild(inBubble);
      stream.scrollTop = stream.scrollHeight;
    }, 850);
  }

  sendBtn.addEventListener('click', sendMessage);
  input.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      sendMessage();
    }
  });
}

// --- 3. Live Packet Counter ---
function initLivePacketCounter() {
  const counterEl = document.getElementById('live-packet-counter');
  if (!counterEl) return;

  let currentCount = 1240;
  setInterval(() => {
    currentCount += Math.floor(1 + Math.random() * 3);
    counterEl.textContent = currentCount.toLocaleString();
  }, 1500);
}

// --- 4. Live Radar Canvas in Story 1 (Video-like live motion element) ---
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

    // Dark high-contrast radar display
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

      // Pulse ring
      ctx.strokeStyle = p.color;
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.arc(px, py, 7 + Math.sin(angle * 2) * 2, 0, Math.PI * 2);
      ctx.stroke();

      // Core dot
      ctx.fillStyle = p.color;
      ctx.beginPath();
      ctx.arc(px, py, 3.5, 0, Math.PI * 2);
      ctx.fill();

      // Label
      ctx.font = '9px "JetBrains Mono", monospace';
      ctx.fillStyle = '#94a3b8';
      ctx.fillText(`${p.name} (${p.hops}h)`, px + 8, py + 3);
    });

    requestAnimationFrame(draw);
  }

  draw();
}

// --- 5. Smooth Scroll for Navigation Links ---
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
