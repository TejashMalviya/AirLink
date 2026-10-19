/* ==========================================================================
   AirLink — Two 3D Three.js Living Mesh Networks
   1. HERO SCENE: Light-themed mesh network (#f5f5f7) with concentric radar rings
   2. SANDBOX SCENE: Dark-themed interactive playground (#1d1d1f) with jammer simulation
   ========================================================================== */

(function () {
  'use strict';

  // Shared geometry instances for high performance and zero frame allocations
  const SHARED_PACKET_GEO = new THREE.SphereGeometry(1.3, 12, 12);

  // Global AirLink3D Controller
  window.AirLink3D = window.AirLink3D || {};

  /* ==========================================================================
     HERO SCENE (#hero-3d-canvas) — Light Theme (#f5f5f7)
     ========================================================================== */
  function initHeroScene() {
    const canvas = document.getElementById('hero-3d-canvas');
    if (!canvas) return;

    const container = canvas.parentElement || canvas;

    // 1. Scene & Light Background
    const scene = new THREE.Scene();
    scene.background = new THREE.Color(0xf5f5f7);
    scene.fog = new THREE.FogExp2(0xf5f5f7, 0.0032);

    // 2. Camera
    const camera = new THREE.PerspectiveCamera(
      42,
      (canvas.clientWidth || 800) / (canvas.clientHeight || 450),
      1,
      1000
    );
    camera.position.set(0, 12, 115);

    // 3. Renderer
    const renderer = new THREE.WebGLRenderer({
      canvas: canvas,
      antialias: true,
      alpha: false,
      powerPreference: 'high-performance'
    });
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
    renderer.setClearColor(0xf5f5f7, 1);

    // 4. Lighting for crisp light theme
    const ambientLight = new THREE.AmbientLight(0xffffff, 0.82);
    scene.add(ambientLight);

    const dirLight1 = new THREE.DirectionalLight(0xffffff, 0.65);
    dirLight1.position.set(60, 90, 70);
    scene.add(dirLight1);

    const dirLight2 = new THREE.DirectionalLight(0x06b6d4, 0.35);
    dirLight2.position.set(-60, -40, -40);
    scene.add(dirLight2);

    // 5. Hero Root Group (for rotation & parallax)
    const heroGroup = new THREE.Group();
    scene.add(heroGroup);

    // 6. 7 Nodes (spheres) in emerald (#10b981) and cyan (#06b6d4)
    const HERO_PEER_DEFS = [
      { x: 0,   y: 0,   z: 0,   color: 0x10b981, isCenter: true },
      { x: -34, y: 18,  z: -14, color: 0x06b6d4, isCenter: false },
      { x: 35,  y: 16,  z: 12,  color: 0x10b981, isCenter: false },
      { x: -28, y: -20, z: 16,  color: 0x06b6d4, isCenter: false },
      { x: 26,  y: -18, z: -18, color: 0x10b981, isCenter: false },
      { x: 50,  y: 2,   z: -8,  color: 0x06b6d4, isCenter: false },
      { x: -48, y: -4,  z: 8,   color: 0x10b981, isCenter: false }
    ];

    const heroNodes = [];

    HERO_PEER_DEFS.forEach((def, index) => {
      const nodeGroup = new THREE.Group();

      const radius = def.isCenter ? 4.2 : 3.2;
      const sphereGeo = new THREE.SphereGeometry(radius, 24, 24);
      const sphereMat = new THREE.MeshPhongMaterial({
        color: def.color,
        emissive: def.color,
        emissiveIntensity: 0.18,
        specular: 0xffffff,
        shininess: 70
      });
      const coreMesh = new THREE.Mesh(sphereGeo, sphereMat);
      nodeGroup.add(coreMesh);

      // Subtle translucent aura ring/shell
      const auraGeo = new THREE.SphereGeometry(radius * 1.35, 16, 16);
      const auraMat = new THREE.MeshBasicMaterial({
        color: def.color,
        wireframe: true,
        transparent: true,
        opacity: 0.12
      });
      const auraMesh = new THREE.Mesh(auraGeo, auraMat);
      nodeGroup.add(auraMesh);

      // Center node beacon equator ring
      if (def.isCenter) {
        const ringGeo = new THREE.RingGeometry(radius * 1.5, radius * 1.8, 36);
        const ringMat = new THREE.MeshBasicMaterial({
          color: 0x10b981,
          side: THREE.DoubleSide,
          transparent: true,
          opacity: 0.35
        });
        const equator = new THREE.Mesh(ringGeo, ringMat);
        equator.rotation.x = Math.PI / 2;
        nodeGroup.add(equator);
      }

      nodeGroup.position.set(def.x, def.y, def.z);
      heroGroup.add(nodeGroup);

      heroNodes.push({
        index: index,
        group: nodeGroup,
        core: coreMesh,
        basePos: new THREE.Vector3(def.x, def.y, def.z),
        isCenter: def.isCenter,
        color: def.color,
        pulseScale: 1.0,
        floatSpeed: {
          x: 0.7 + (index % 3) * 0.25,
          y: 0.9 + (index % 4) * 0.2,
          z: 0.6 + (index % 5) * 0.2
        },
        floatOffset: {
          x: index * 1.1,
          y: index * 1.7,
          z: index * 2.3
        },
        floatAmp: {
          x: def.isCenter ? 0.6 : 1.6,
          y: def.isCenter ? 1.0 : 2.4,
          z: def.isCenter ? 0.6 : 1.6
        }
      });
    });

    // 7. Concentric Radar Rings Expanding from Center Node
    const radarRings = [];
    const RADAR_RING_COUNT = 3;
    for (let i = 0; i < RADAR_RING_COUNT; i++) {
      const ringGeo = new THREE.RingGeometry(1, 1.45, 64);
      const ringMat = new THREE.MeshBasicMaterial({
        color: 0x10b981,
        side: THREE.DoubleSide,
        transparent: true,
        opacity: 0.4
      });
      const ringMesh = new THREE.Mesh(ringGeo, ringMat);
      ringMesh.rotation.x = Math.PI / 2;
      heroGroup.add(ringMesh);

      radarRings.push({
        mesh: ringMesh,
        mat: ringMat,
        phase: i / RADAR_RING_COUNT
      });
    }

    // 8. Dynamic Links Connecting Nearby Nodes (emerald, semi-transparent)
    const MAX_HERO_LINKS = 24;
    const heroLinkPositions = new Float32Array(MAX_HERO_LINKS * 6);
    const heroLinkGeo = new THREE.BufferGeometry();
    heroLinkGeo.setAttribute(
      'position',
      new THREE.BufferAttribute(heroLinkPositions, 3)
    );

    const heroLinkMat = new THREE.LineBasicMaterial({
      color: 0x10b981,
      transparent: true,
      opacity: 0.4,
      linewidth: 1.5
    });

    const heroLinkLines = new THREE.LineSegments(heroLinkGeo, heroLinkMat);
    heroGroup.add(heroLinkLines);

    let heroActivePairs = [];
    function computeHeroLinks() {
      heroActivePairs = [];
      const threshold = 62;
      for (let i = 0; i < heroNodes.length; i++) {
        for (let j = i + 1; j < heroNodes.length; j++) {
          const d = heroNodes[i].basePos.distanceTo(heroNodes[j].basePos);
          if (d <= threshold) {
            heroActivePairs.push([i, j]);
          }
        }
      }
      heroLinkGeo.setDrawRange(0, heroActivePairs.length * 2);
    }
    computeHeroLinks();

    function updateHeroLinkPositions() {
      const pos = heroLinkGeo.attributes.position.array;
      for (let k = 0; k < heroActivePairs.length; k++) {
        const [a, b] = heroActivePairs[k];
        const pA = heroNodes[a].group.position;
        const pB = heroNodes[b].group.position;

        const idx = k * 6;
        pos[idx + 0] = pA.x;
        pos[idx + 1] = pA.y;
        pos[idx + 2] = pA.z;
        pos[idx + 3] = pB.x;
        pos[idx + 4] = pB.y;
        pos[idx + 5] = pB.z;
      }
      heroLinkGeo.attributes.position.needsUpdate = true;
    }

    // 9. Luminous Data Packets Traveling Along Links with Parabolic Arc
    const heroPackets = [];
    const heroPacketMatEmerald = new THREE.MeshBasicMaterial({ color: 0x10b981 });
    const heroPacketMatCyan = new THREE.MeshBasicMaterial({ color: 0x06b6d4 });

    const heroCounterEl = document.getElementById('live-packet-counter');
    let heroPacketCounter = 1240;

    function spawnHeroPacket() {
      if (heroActivePairs.length === 0) return;
      const pair = heroActivePairs[Math.floor(Math.random() * heroActivePairs.length)];
      const reverse = Math.random() > 0.5;
      const fromIdx = reverse ? pair[1] : pair[0];
      const toIdx = reverse ? pair[0] : pair[1];

      const toNode = heroNodes[toIdx];
      const mat = toNode.color === 0x06b6d4 ? heroPacketMatCyan : heroPacketMatEmerald;
      const mesh = new THREE.Mesh(SHARED_PACKET_GEO, mat);
      heroGroup.add(mesh);

      heroPackets.push({
        mesh: mesh,
        fromIdx: fromIdx,
        toIdx: toIdx,
        progress: 0.0,
        speed: 0.55 + Math.random() * 0.25,
        arcHeight: 7 + Math.random() * 6
      });
    }

    // Spawn packets every 900ms
    setInterval(spawnHeroPacket, 900);

    // 10. Auto-rotation & Mouse Parallax
    let mouseX = 0;
    let mouseY = 0;
    let targetParallaxX = 0;
    let targetParallaxY = 0;
    let currentParallaxX = 0;
    let currentParallaxY = 0;
    let baseAutoRotation = 0;

    window.addEventListener('mousemove', (e) => {
      const nx = (e.clientX / window.innerWidth) * 2 - 1;
      const ny = (e.clientY / window.innerHeight) * 2 - 1;
      targetParallaxX = ny * 0.18;
      targetParallaxY = nx * 0.28;
    });

    // 11. Resize Handler
    function handleResize() {
      const w = container.clientWidth || canvas.clientWidth || 800;
      const h = container.clientHeight || canvas.clientHeight || 450;
      if (w > 0 && h > 0) {
        renderer.setSize(w, h, false);
        camera.aspect = w / h;
        camera.updateProjectionMatrix();
      }
    }
    window.addEventListener('resize', handleResize);
    if (window.ResizeObserver) {
      new ResizeObserver(handleResize).observe(container);
    }
    handleResize();

    // 12. Animation Loop (Smooth 60fps)
    const clock = new THREE.Clock();

    function animateHero() {
      requestAnimationFrame(animateHero);
      const delta = Math.min(clock.getDelta(), 0.1);
      const time = clock.getElapsedTime();

      // Organic Sine Floating for Nodes
      heroNodes.forEach(node => {
        node.group.position.x =
          node.basePos.x +
          Math.sin(time * node.floatSpeed.x + node.floatOffset.x) * node.floatAmp.x;
        node.group.position.y =
          node.basePos.y +
          Math.sin(time * node.floatSpeed.y + node.floatOffset.y) * node.floatAmp.y;
        node.group.position.z =
          node.basePos.z +
          Math.cos(time * node.floatSpeed.z + node.floatOffset.z) * node.floatAmp.z;

        // Node pulse bounce recovery
        if (node.pulseScale > 1.0) {
          node.pulseScale -= delta * 1.5;
          if (node.pulseScale < 1.0) node.pulseScale = 1.0;
          node.core.scale.setScalar(node.pulseScale);
        }
      });

      // Update link lines
      updateHeroLinkPositions();

      // Concentric Radar Rings Expanding from Center Node
      const centerPos = heroNodes[0] ? heroNodes[0].group.position : new THREE.Vector3();
      radarRings.forEach(r => {
        r.phase = (r.phase + delta * 0.32) % 1.0;
        const currentRadius = 4 + r.phase * 42;
        r.mesh.scale.set(currentRadius, currentRadius, 1);
        r.mesh.position.copy(centerPos);
        r.mat.opacity = Math.sin(r.phase * Math.PI) * 0.42;
      });

      // Update Packets with Parabolic Arc
      for (let i = heroPackets.length - 1; i >= 0; i--) {
        const p = heroPackets[i];
        p.progress += delta * p.speed;

        const pA = heroNodes[p.fromIdx].group.position;
        const pB = heroNodes[p.toIdx].group.position;
        const t = Math.min(p.progress, 1.0);

        // Linear lerp
        p.mesh.position.lerpVectors(pA, pB, t);

        // Parabolic arc displacement: peak at t = 0.5
        const arc = 4 * t * (1 - t) * p.arcHeight;
        p.mesh.position.y += arc;

        // Packet reached destination
        if (p.progress >= 1.0) {
          heroNodes[p.toIdx].pulseScale = 1.28;
          heroGroup.remove(p.mesh);
          heroPackets.splice(i, 1);

          heroPacketCounter++;
          if (heroCounterEl) {
            heroCounterEl.textContent = heroPacketCounter.toLocaleString();
          }
        }
      }

      // Parallax & Gentle Auto-rotation
      baseAutoRotation += delta * 0.08;
      currentParallaxX += (targetParallaxX - currentParallaxX) * 0.06;
      currentParallaxY += (targetParallaxY - currentParallaxY) * 0.06;

      heroGroup.rotation.x = currentParallaxX;
      heroGroup.rotation.y = baseAutoRotation + currentParallaxY;

      renderer.render(scene, camera);
    }

    animateHero();
  }

  /* ==========================================================================
     SANDBOX SCENE (.sandbox-viewport) — Dark Theme (#1d1d1f) Playground
     ========================================================================== */
  function initSandboxScene() {
    const viewport =
      document.querySelector('.sandbox-viewport') ||
      document.querySelector('.sandbox-canvas-viewport') ||
      document.getElementById('sandbox-canvas-target');

    if (!viewport) return;

    // Check if canvas already exists or create one behind HUD
    let canvas = viewport.querySelector('canvas');
    if (!canvas) {
      canvas = document.createElement('canvas');
      viewport.prepend(canvas);
    }

    // 1. Scene & Dark Background (#1d1d1f)
    const scene = new THREE.Scene();
    scene.background = new THREE.Color(0x1d1d1f);
    scene.fog = new THREE.FogExp2(0x1d1d1f, 0.003);

    // 2. Camera
    const camera = new THREE.PerspectiveCamera(
      45,
      (viewport.clientWidth || 800) / (viewport.clientHeight || 420),
      1,
      1000
    );
    camera.position.set(0, 16, 125);

    // 3. Renderer
    const renderer = new THREE.WebGLRenderer({
      canvas: canvas,
      antialias: true,
      alpha: false,
      powerPreference: 'high-performance'
    });
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
    renderer.setClearColor(0x1d1d1f, 1);

    // 4. Lighting for luminous dark theme
    const ambientLight = new THREE.AmbientLight(0xffffff, 0.35);
    scene.add(ambientLight);

    const greenPointLight = new THREE.PointLight(0x10b981, 2.5, 250);
    greenPointLight.position.set(-30, 40, 50);
    scene.add(greenPointLight);

    const cyanPointLight = new THREE.PointLight(0x06b6d4, 2.2, 250);
    cyanPointLight.position.set(40, -30, 40);
    scene.add(cyanPointLight);

    // 5. Sandbox Root Group
    const sandboxGroup = new THREE.Group();
    scene.add(sandboxGroup);

    // 6. Jammer State & Wireframe Jammer Sphere
    let isJammerActive = false;

    const jammerGroup = new THREE.Group();
    jammerGroup.position.set(0, 0, 0);

    const jammerGeo = new THREE.SphereGeometry(22, 22, 22);
    const jammerMat = new THREE.MeshBasicMaterial({
      color: 0xf43f5e,
      wireframe: true,
      transparent: true,
      opacity: 0.85
    });
    const jammerOuter = new THREE.Mesh(jammerGeo, jammerMat);
    jammerGroup.add(jammerOuter);

    const jammerCoreGeo = new THREE.SphereGeometry(10, 16, 16);
    const jammerCoreMat = new THREE.MeshBasicMaterial({
      color: 0xef4444,
      wireframe: true,
      transparent: true,
      opacity: 0.4
    });
    const jammerInner = new THREE.Mesh(jammerCoreGeo, jammerCoreMat);
    jammerGroup.add(jammerInner);

    jammerGroup.visible = false;
    sandboxGroup.add(jammerGroup);

    // 7. Sandbox Nodes
    const INITIAL_SANDBOX_NODES = [
      { x: 0,   y: 0,   z: 0,   color: 0x10b981, isCenter: true },
      { x: -35, y: 18,  z: -14, color: 0x06b6d4, isCenter: false },
      { x: 36,  y: 16,  z: 14,  color: 0x10b981, isCenter: false },
      { x: -26, y: -22, z: 16,  color: 0x06b6d4, isCenter: false },
      { x: 28,  y: -18, z: -18, color: 0x10b981, isCenter: false },
      { x: 50,  y: 0,   z: -8,  color: 0x06b6d4, isCenter: false },
      { x: -48, y: -4,  z: 6,   color: 0x10b981, isCenter: false }
    ];

    const sandboxNodes = [];

    function buildNodeObject(x, y, z, color, isCenter, index) {
      const group = new THREE.Group();
      const radius = isCenter ? 4.4 : 3.4;

      const sphereGeo = new THREE.SphereGeometry(radius, 24, 24);
      const sphereMat = new THREE.MeshStandardMaterial({
        color: color,
        emissive: color,
        emissiveIntensity: 0.45,
        roughness: 0.25,
        metalness: 0.15
      });
      const core = new THREE.Mesh(sphereGeo, sphereMat);
      group.add(core);

      const auraGeo = new THREE.SphereGeometry(radius * 1.35, 16, 16);
      const auraMat = new THREE.MeshBasicMaterial({
        color: color,
        wireframe: true,
        transparent: true,
        opacity: 0.22
      });
      const aura = new THREE.Mesh(auraGeo, auraMat);
      group.add(aura);

      group.position.set(x, y, z);
      sandboxGroup.add(group);

      return {
        index: index,
        group: group,
        core: core,
        basePos: new THREE.Vector3(x, y, z),
        color: color,
        isCenter: isCenter,
        pulseScale: 1.0,
        floatSpeed: {
          x: 0.7 + (index % 3) * 0.3,
          y: 0.9 + (index % 4) * 0.25,
          z: 0.6 + (index % 5) * 0.2
        },
        floatOffset: {
          x: index * 1.2,
          y: index * 1.6,
          z: index * 2.1
        },
        floatAmp: {
          x: isCenter ? 0.8 : 1.8,
          y: isCenter ? 1.2 : 2.6,
          z: isCenter ? 0.8 : 1.8
        }
      };
    }

    INITIAL_SANDBOX_NODES.forEach((def, i) => {
      sandboxNodes.push(buildNodeObject(def.x, def.y, def.z, def.color, def.isCenter, i));
    });

    // 8. Dynamic Link System with Rerouting support
    const MAX_SANDBOX_LINKS = 120;
    const sandboxLinkPositions = new Float32Array(MAX_SANDBOX_LINKS * 6);
    const sandboxLinkGeo = new THREE.BufferGeometry();
    sandboxLinkGeo.setAttribute(
      'position',
      new THREE.BufferAttribute(sandboxLinkPositions, 3)
    );

    const sandboxLinkMat = new THREE.LineBasicMaterial({
      color: 0x10b981,
      transparent: true,
      opacity: 0.5,
      linewidth: 1.8
    });

    const sandboxLinkLines = new THREE.LineSegments(sandboxLinkGeo, sandboxLinkMat);
    sandboxGroup.add(sandboxLinkLines);

    let sandboxActivePairs = [];

    function rebuildLinks() {
      sandboxActivePairs = [];
      const threshold = 66;
      const jammerRadiusSq = 20 * 20;

      for (let i = 0; i < sandboxNodes.length; i++) {
        for (let j = i + 1; j < sandboxNodes.length; j++) {
          const pA = sandboxNodes[i].basePos;
          const pB = sandboxNodes[j].basePos;
          const dist = pA.distanceTo(pB);

          if (dist <= threshold) {
            // When jammer is active, test if the link directly passes near the jammer center (0,0,0)
            if (isJammerActive) {
              const mid = new THREE.Vector3().addVectors(pA, pB).multiplyScalar(0.5);
              const passesThroughJammer = mid.lengthSq() < jammerRadiusSq;
              if (passesThroughJammer) {
                // Link is blocked by jammer — skip it to visually showcase dynamic rerouting!
                continue;
              }
            }
            sandboxActivePairs.push([i, j]);
          }
        }
      }

      // Guarantee each node has at least 1 connection
      for (let i = 0; i < sandboxNodes.length; i++) {
        const hasConnection = sandboxActivePairs.some(pair => pair[0] === i || pair[1] === i);
        if (!hasConnection) {
          let closestIdx = -1;
          let closestDist = Infinity;
          for (let j = 0; j < sandboxNodes.length; j++) {
            if (i === j) continue;
            const d = sandboxNodes[i].basePos.distanceTo(sandboxNodes[j].basePos);
            if (d < closestDist) {
              closestDist = d;
              closestIdx = j;
            }
          }
          if (closestIdx !== -1) {
            const pair = i < closestIdx ? [i, closestIdx] : [closestIdx, i];
            sandboxActivePairs.push(pair);
          }
        }
      }

      sandboxLinkGeo.setDrawRange(0, sandboxActivePairs.length * 2);
    }
    rebuildLinks();

    function updateSandboxLinkPositions() {
      const pos = sandboxLinkGeo.attributes.position.array;
      for (let k = 0; k < sandboxActivePairs.length; k++) {
        const [a, b] = sandboxActivePairs[k];
        const pA = sandboxNodes[a].group.position;
        const pB = sandboxNodes[b].group.position;

        const idx = k * 6;
        pos[idx + 0] = pA.x;
        pos[idx + 1] = pA.y;
        pos[idx + 2] = pA.z;
        pos[idx + 3] = pB.x;
        pos[idx + 4] = pB.y;
        pos[idx + 5] = pB.z;
      }
      sandboxLinkGeo.attributes.position.needsUpdate = true;
    }

    // 9. Luminous Data Packets Traveling Along Links with Parabolic Arc
    const sandboxPackets = [];
    const packetMatNormal = new THREE.MeshBasicMaterial({ color: 0x34d399 });
    const packetMatRerouted = new THREE.MeshBasicMaterial({ color: 0x38bdf8 });

    function spawnPacketAlongLink(pair, customSpeed) {
      if (!pair) return;
      const reverse = Math.random() > 0.5;
      const fromIdx = reverse ? pair[1] : pair[0];
      const toIdx = reverse ? pair[0] : pair[1];

      const mat = isJammerActive ? packetMatRerouted : packetMatNormal;
      const mesh = new THREE.Mesh(SHARED_PACKET_GEO, mat);
      sandboxGroup.add(mesh);

      sandboxPackets.push({
        mesh: mesh,
        fromIdx: fromIdx,
        toIdx: toIdx,
        progress: 0.0,
        speed: customSpeed || (0.6 + Math.random() * 0.3),
        arcHeight: 8 + Math.random() * 7
      });
    }

    function spawnSandboxPacket() {
      if (sandboxActivePairs.length === 0) return;
      const pair = sandboxActivePairs[Math.floor(Math.random() * sandboxActivePairs.length)];
      spawnPacketAlongLink(pair);
    }

    // Periodic packet transmission every 900ms
    setInterval(spawnSandboxPacket, 900);

    // 10. Mouse Drag & Touch Support to Rotate Scene with Inertia
    let isDragging = false;
    let lastX = 0;
    let lastY = 0;
    let targetRotX = 0.15;
    let targetRotY = 0;
    let currentRotX = 0.15;
    let currentRotY = 0;

    function onPointerStart(e) {
      isDragging = true;
      const clientX = e.touches ? e.touches[0].clientX : e.clientX;
      const clientY = e.touches ? e.touches[0].clientY : e.clientY;
      lastX = clientX;
      lastY = clientY;
    }

    function onPointerMove(e) {
      if (!isDragging) return;
      const clientX = e.touches ? e.touches[0].clientX : e.clientX;
      const clientY = e.touches ? e.touches[0].clientY : e.clientY;
      const dx = clientX - lastX;
      const dy = clientY - lastY;
      lastX = clientX;
      lastY = clientY;

      targetRotY += dx * 0.008;
      targetRotX += dy * 0.008;
      targetRotX = Math.max(-Math.PI / 2.5, Math.min(Math.PI / 2.5, targetRotX));
    }

    function onPointerEnd() {
      isDragging = false;
    }

    viewport.addEventListener('mousedown', onPointerStart);
    window.addEventListener('mousemove', onPointerMove);
    window.addEventListener('mouseup', onPointerEnd);

    viewport.addEventListener('touchstart', onPointerStart, { passive: true });
    window.addEventListener('touchmove', onPointerMove, { passive: true });
    window.addEventListener('touchend', onPointerEnd, { passive: true });

    // 11. HUD Element References
    const statusTextEl = document.getElementById('sandbox-status-text');
    const nodeCountEl = document.getElementById('sandbox-node-count');
    const jammerToggleBtn = document.getElementById('sandbox-jammer-toggle');

    function updateHudStatus(message, color) {
      if (statusTextEl) {
        statusTextEl.textContent = message;
        if (color) statusTextEl.style.color = color;
      }
    }

    let statusResetTimeout = null;
    function flashStatus(message, color, duration = 3000) {
      updateHudStatus(message, color);
      if (statusResetTimeout) clearTimeout(statusResetTimeout);
      statusResetTimeout = setTimeout(() => {
        if (isJammerActive) {
          updateHudStatus('⚡ JAMMER DETECTED (2.4GHz) • REROUTING VIA MESH • 100% ONLINE', '#38bdf8');
        } else {
          updateHudStatus('ALL RADIOS CLEAR • DIRECT PEER-TO-PEER', '#10b981');
        }
      }, duration);
    }

    // 12. Public API: window.AirLink3D methods
    window.AirLink3D.toggleJammer = function () {
      isJammerActive = !isJammerActive;
      jammerGroup.visible = isJammerActive;

      // When jammer is active, links turn cyan (#06b6d4) to show dynamic rerouting!
      if (isJammerActive) {
        sandboxLinkMat.color.setHex(0x06b6d4);
        sandboxLinkMat.opacity = 0.85;
        updateHudStatus('⚡ JAMMER DETECTED (2.4GHz) • REROUTING VIA MESH • 100% ONLINE', '#38bdf8');
        if (jammerToggleBtn) {
          jammerToggleBtn.classList.add('active');
          jammerToggleBtn.textContent = '❌ Stop Jammer Attack';
        }
      } else {
        sandboxLinkMat.color.setHex(0x10b981);
        sandboxLinkMat.opacity = 0.5;
        updateHudStatus('ALL RADIOS CLEAR • DIRECT PEER-TO-PEER', '#10b981');
        if (jammerToggleBtn) {
          jammerToggleBtn.classList.remove('active');
          jammerToggleBtn.textContent = '⚡ Simulate Jammer Attack';
        }
      }

      // Rebuild links around jammer
      rebuildLinks();

      // Fire a wave of packets to show the new rerouted paths
      window.AirLink3D.sendPulse();
    };

    window.AirLink3D.addNode = function () {
      const idx = sandboxNodes.length;
      // Position new node in 3D orbit around swarm
      const angle = Math.random() * Math.PI * 2;
      const radius = 38 + Math.random() * 24;
      const x = Math.cos(angle) * radius;
      const y = (Math.random() - 0.5) * 55;
      const z = Math.sin(angle) * radius;

      const color = idx % 2 === 0 ? 0x06b6d4 : 0x10b981;
      const newNode = buildNodeObject(x, y, z, color, false, idx);
      sandboxNodes.push(newNode);

      // Rebuild links with new node
      rebuildLinks();

      // Update node count in HUD
      if (nodeCountEl) {
        nodeCountEl.textContent = sandboxNodes.length;
      }

      flashStatus(`NEW HUMAN NODE JOINED • SWARM: ${sandboxNodes.length} RELAYS`, '#34d399', 3000);

      // Fire a packet to welcome the new node
      const matchingLinks = sandboxActivePairs.filter(p => p[0] === idx || p[1] === idx);
      if (matchingLinks.length > 0) {
        spawnPacketAlongLink(matchingLinks[0], 0.9);
      }
    };

    // Aliased for index.html / app.js compatibility
    window.AirLink3D.addHumanNode = window.AirLink3D.addNode;

    window.AirLink3D.sendPulse = function () {
      if (sandboxActivePairs.length === 0) return;

      flashStatus('📡 BROADCASTING VOICE PACKETS • 5-HOP BURST', '#38bdf8', 2200);

      // Fire burst of 5 packets along diverse links
      for (let i = 0; i < 5; i++) {
        setTimeout(() => {
          if (sandboxActivePairs.length === 0) return;
          const randomPair = sandboxActivePairs[Math.floor(Math.random() * sandboxActivePairs.length)];
          spawnPacketAlongLink(randomPair, 0.7 + Math.random() * 0.4);
        }, i * 90);
      }
    };

    // Aliased for index.html / app.js compatibility
    window.AirLink3D.broadcastVoicePing = window.AirLink3D.sendPulse;

    // 13. Resize Handler
    function handleResize() {
      const w = viewport.clientWidth || 800;
      const h = viewport.clientHeight || 420;
      if (w > 0 && h > 0) {
        renderer.setSize(w, h, false);
        camera.aspect = w / h;
        camera.updateProjectionMatrix();
      }
    }
    window.addEventListener('resize', handleResize);
    if (window.ResizeObserver) {
      new ResizeObserver(handleResize).observe(viewport);
    }
    handleResize();

    // 14. Animation Loop (Smooth 60fps)
    const clock = new THREE.Clock();

    function animateSandbox() {
      requestAnimationFrame(animateSandbox);
      const delta = Math.min(clock.getDelta(), 0.1);
      const time = clock.getElapsedTime();

      // Organic Sine Floating for Sandbox Nodes
      sandboxNodes.forEach(node => {
        node.group.position.x =
          node.basePos.x +
          Math.sin(time * node.floatSpeed.x + node.floatOffset.x) * node.floatAmp.x;
        node.group.position.y =
          node.basePos.y +
          Math.sin(time * node.floatSpeed.y + node.floatOffset.y) * node.floatAmp.y;
        node.group.position.z =
          node.basePos.z +
          Math.cos(time * node.floatSpeed.z + node.floatOffset.z) * node.floatAmp.z;

        if (node.pulseScale > 1.0) {
          node.pulseScale -= delta * 1.5;
          if (node.pulseScale < 1.0) node.pulseScale = 1.0;
          node.core.scale.setScalar(node.pulseScale);
        }
      });

      // Update link lines
      updateSandboxLinkPositions();

      // Jammer Interference Pulse & Rotation
      if (isJammerActive) {
        jammerGroup.rotation.y += delta * 1.2;
        jammerGroup.rotation.x += delta * 0.6;
        const pulse = 1.0 + Math.sin(time * 5.0) * 0.08;
        jammerGroup.scale.set(pulse, pulse, pulse);
      }

      // Update Packets with Parabolic Arc
      for (let i = sandboxPackets.length - 1; i >= 0; i--) {
        const p = sandboxPackets[i];
        p.progress += delta * p.speed;

        const pA = sandboxNodes[p.fromIdx].group.position;
        const pB = sandboxNodes[p.toIdx].group.position;
        const t = Math.min(p.progress, 1.0);

        // Linear lerp
        p.mesh.position.lerpVectors(pA, pB, t);

        // Parabolic arc displacement
        const arc = 4 * t * (1 - t) * p.arcHeight;
        p.mesh.position.y += arc;

        // Arrival
        if (p.progress >= 1.0) {
          sandboxNodes[p.toIdx].pulseScale = 1.25;
          sandboxGroup.remove(p.mesh);
          sandboxPackets.splice(i, 1);
        }
      }

      // Drag Orbit with Damping & Idle Drift
      if (!isDragging) {
        targetRotY += delta * 0.06;
      }
      currentRotX += (targetRotX - currentRotX) * 0.08;
      currentRotY += (targetRotY - currentRotY) * 0.08;

      sandboxGroup.rotation.x = currentRotX;
      sandboxGroup.rotation.y = currentRotY;

      renderer.render(scene, camera);
    }

    animateSandbox();
  }

  /* ==========================================================================
     Initialize Both Scenes
     ========================================================================== */
  function initAll() {
    initHeroScene();
    initSandboxScene();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initAll);
  } else {
    initAll();
  }
})();
