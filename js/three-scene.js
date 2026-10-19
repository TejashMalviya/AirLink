/* ==========================================================================
   AirLink - Interactive Three.js 3D Mesh & Jammer Simulation
   Demonstrates multi-hop packet routing and jammers being rendered useless.
   ========================================================================== */

(function () {
  const container = document.getElementById('three-canvas-wrap');
  if (!container) return;

  let scene, camera, renderer, raycaster, mouse;
  let nodes = [];
  let links = [];
  let packets = [];
  let centralTower;
  let jammerWave;
  let isJammed = false;
  let animFrameId;
  let hoveredNode = null;
  let selectedNode = null;

  // Node peer profiles matching actual AirLink mesh devices
  const PEER_PROFILES = [
    { name: "You (Local Node)", id: "0x7F2A", x: -90, y: 10, z: -40, isHost: true },
    { name: "Alex (Pixel 8)", id: "0x1A4C", x: -40, y: 30, z: 20, isHost: false },
    { name: "Maya (Galaxy S24)", id: "0x89DE", x: -20, y: -25, z: -50, isHost: false },
    { name: "Devin (OnePlus 12)", id: "0x44B1", x: 30, y: 20, z: -30, isHost: false },
    { name: "Kiran (Nothing Phone 2)", id: "0xC038", x: 45, y: -20, z: 40, isHost: false },
    { name: "Target Peer (Elena)", id: "0x9E77", x: 95, y: 5, z: 10, isHost: false }
  ];

  // Adjacency connections for mesh hops
  const CONNECTIONS = [
    [0, 1], // You -> Alex (Direct Hotspot)
    [0, 2], // You -> Maya (Wi-Fi Direct)
    [1, 3], // Alex -> Devin (Relay hop 1)
    [2, 4], // Maya -> Kiran (Relay hop 1)
    [3, 5], // Devin -> Elena (Destination hop 2)
    [4, 5], // Kiran -> Elena (Alternative path)
    [1, 2], // Mesh cross-link
    [3, 4]  // Mesh cross-link
  ];

  function init() {
    const width = container.clientWidth;
    const height = container.clientHeight;

    // 1. Scene & Camera
    scene = new THREE.Scene();
    scene.fog = new THREE.FogExp2(0x070a0f, 0.0022);

    camera = new THREE.PerspectiveCamera(45, width / height, 1, 1000);
    camera.position.set(0, 70, 210);
    camera.lookAt(0, 0, 0);

    // 2. Renderer
    renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
    renderer.setSize(width, height);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    container.appendChild(renderer.domElement);

    raycaster = new THREE.Raycaster();
    mouse = new THREE.Vector2();

    // 3. Grid & Ambient Space
    const gridHelper = new THREE.GridHelper(300, 30, 0x10b981, 0x152336);
    gridHelper.position.y = -50;
    gridHelper.material.opacity = 0.25;
    gridHelper.material.transparent = true;
    scene.add(gridHelper);

    // 4. Lights
    const ambientLight = new THREE.AmbientLight(0xffffff, 0.8);
    scene.add(ambientLight);

    const pointLight = new THREE.PointLight(0x10b981, 2, 300);
    pointLight.position.set(0, 50, 50);
    scene.add(pointLight);

    // 5. Build Central "Obsolete" Cellular Tower
    createCellTower();

    // 6. Build AirLink Mesh Nodes
    createMeshNodes();

    // 7. Build Connection Links
    createMeshLinks();

    // 8. Event Listeners
    setupControls();
    window.addEventListener('resize', onWindowResize);

    // 9. Start Packet Animation Loop
    setInterval(spawnMeshPacket, 900);

    // Start render loop
    animate();
  }

  function createCellTower() {
    const towerGroup = new THREE.Group();

    // Tower Lattice Body
    const towerGeo = new THREE.CylinderGeometry(2, 8, 55, 6);
    const towerMat = new THREE.MeshBasicMaterial({
      color: 0x475569,
      wireframe: true,
      transparent: true,
      opacity: 0.6
    });
    const towerMesh = new THREE.Mesh(towerGeo, towerMat);
    towerMesh.position.y = -22;
    towerGroup.add(towerMesh);

    // Top Transmitter Beacon
    const beaconGeo = new THREE.SphereGeometry(3.5, 16, 16);
    const beaconMat = new THREE.MeshBasicMaterial({ color: 0x38bdf8 });
    const beacon = new THREE.Mesh(beaconGeo, beaconMat);
    beacon.position.y = 8;
    towerGroup.add(beacon);

    // Tower Rings (Cellular Waves)
    const ringGeo = new THREE.RingGeometry(6, 7, 32);
    const ringMat = new THREE.MeshBasicMaterial({
      color: 0x0284c7,
      side: THREE.DoubleSide,
      transparent: true,
      opacity: 0.4
    });
    const ring = new THREE.Mesh(ringGeo, ringMat);
    ring.rotation.x = Math.PI / 2;
    ring.position.y = 8;
    towerGroup.add(ring);

    towerGroup.position.set(0, 0, 0);
    centralTower = {
      group: towerGroup,
      beaconMat: beaconMat,
      towerMat: towerMat,
      ring: ring
    };
    scene.add(towerGroup);

    // Jammer Dome / Wave (Hidden initially)
    const jammerGeo = new THREE.SphereGeometry(65, 32, 32);
    const jammerMat = new THREE.MeshBasicMaterial({
      color: 0xef4444,
      wireframe: true,
      transparent: true,
      opacity: 0.0
    });
    jammerWave = new THREE.Mesh(jammerGeo, jammerMat);
    scene.add(jammerWave);
  }

  function createMeshNodes() {
    PEER_PROFILES.forEach((profile, idx) => {
      const nodeGroup = new THREE.Group();

      // Core Device Sphere
      const sphereGeo = new THREE.SphereGeometry(4.5, 24, 24);
      const sphereMat = new THREE.MeshStandardMaterial({
        color: profile.isHost ? 0x10b981 : 0x06b6d4,
        emissive: profile.isHost ? 0x059669 : 0x0284c7,
        emissiveIntensity: 0.6,
        roughness: 0.2,
        metalness: 0.8
      });
      const sphere = new THREE.Mesh(sphereGeo, sphereMat);
      sphere.userData = { profileIndex: idx, isNode: true };
      nodeGroup.add(sphere);

      // Radar Beacon Rings
      const ringGeo = new THREE.RingGeometry(6.5, 7.5, 32);
      const ringMat = new THREE.MeshBasicMaterial({
        color: profile.isHost ? 0x10b981 : 0x06b6d4,
        side: THREE.DoubleSide,
        transparent: true,
        opacity: 0.4
      });
      const ring = new THREE.Mesh(ringGeo, ringMat);
      ring.rotation.x = Math.PI / 2;
      nodeGroup.add(ring);

      nodeGroup.position.set(profile.x, profile.y, profile.z);
      scene.add(nodeGroup);

      nodes.push({
        group: nodeGroup,
        sphere: sphere,
        ring: ring,
        profile: profile,
        baseY: profile.y,
        floatPhase: Math.random() * Math.PI * 2
      });
    });
  }

  function createMeshLinks() {
    CONNECTIONS.forEach(([fromIdx, toIdx]) => {
      const fromPos = nodes[fromIdx].group.position;
      const toPos = nodes[toIdx].group.position;

      const points = [fromPos, toPos];
      const geometry = new THREE.BufferGeometry().setFromPoints(points);
      const material = new THREE.LineBasicMaterial({
        color: 0x10b981,
        transparent: true,
        opacity: 0.35,
        linewidth: 1
      });

      const line = new THREE.Line(geometry, material);
      scene.add(line);

      links.push({
        line: line,
        fromIdx: fromIdx,
        toIdx: toIdx
      });
    });
  }

  function spawnMeshPacket() {
    // Choose route: Host -> Peer -> Destination
    let route = [];
    if (isJammed) {
      // In jammed mode, routes deliberately avoid the central danger zone and take perimeter multi-hop
      route = Math.random() > 0.5 ? [0, 1, 3, 5] : [0, 2, 4, 5];
    } else {
      route = Math.random() > 0.5 ? [0, 1, 3, 5] : [0, 2, 4, 3, 5];
    }

    const packetGeo = new THREE.SphereGeometry(1.6, 12, 12);
    const packetMat = new THREE.MeshBasicMaterial({
      color: isJammed ? 0x38bdf8 : 0x34d399,
      transparent: true,
      opacity: 0.95
    });
    const packetMesh = new THREE.Mesh(packetGeo, packetMat);
    scene.add(packetMesh);

    packets.push({
      mesh: packetMesh,
      route: route,
      step: 0,
      progress: 0,
      speed: 0.035
    });

    // Web audio subtle packet chirp if enabled
    if (window.AirLinkAudio && window.AirLinkAudio.playSound) {
      window.AirLinkAudio.playPacketChirp();
    }
  }

  function updatePackets() {
    for (let i = packets.length - 1; i >= 0; i--) {
      const p = packets[i];
      const fromNode = nodes[p.route[p.step]];
      const toNode = nodes[p.route[p.step + 1]];

      if (!fromNode || !toNode) {
        scene.remove(p.mesh);
        packets.splice(i, 1);
        continue;
      }

      p.progress += p.speed;

      const fromPos = fromNode.group.position;
      const toPos = toNode.group.position;

      // Lerp position along hop link with slight arch
      p.mesh.position.lerpVectors(fromPos, toPos, p.progress);
      p.mesh.position.y += Math.sin(p.progress * Math.PI) * 4;

      if (p.progress >= 1) {
        p.progress = 0;
        p.step++;

        // Pulse receiving node ring
        toNode.ring.scale.set(1.4, 1.4, 1.4);

        if (p.step >= p.route.length - 1) {
          // Packet reached final destination
          scene.remove(p.mesh);
          packets.splice(i, 1);
          triggerDestinationDelivered();
        }
      }
    }
  }

  function triggerDestinationDelivered() {
    const el = document.getElementById('sim-ticker-msg');
    if (el) {
      const ms = Math.floor(12 + Math.random() * 8);
      el.innerHTML = `Packet delivered to <strong>Target Peer (Elena)</strong> via 3 mesh hops in <span class="ticker-ping">${ms}ms</span>. Zero cellular towers used.`;
    }
  }

  // Jammer Activation Toggle
  window.toggleAirLinkJammer = function () {
    isJammed = !isJammed;
    const btn = document.getElementById('btn-toggle-jammer');
    const badge = document.getElementById('sim-status-badge');
    const intelStatus = document.getElementById('intel-mesh-status');
    const intelTower = document.getElementById('intel-tower-status');
    const ticker = document.getElementById('sim-ticker-msg');

    if (isJammed) {
      // JAMMER IS ACTIVE
      if (btn) {
        btn.innerHTML = '⚡ DEACTIVATE JAMMER (RESTORE RF)';
        btn.classList.add('btn-secondary');
        btn.classList.remove('btn-danger');
      }
      if (badge) {
        badge.className = 'sim-hud-badge badge-jammed';
        badge.innerHTML = '<span class="status-beacon" style="background:#ef4444;box-shadow:0 0 8px #ef4444;"></span> JAMMER ACTIVE: CELL TOWER DEAD (AIRLINK 100% ROUTING)';
      }
      if (intelStatus) intelStatus.innerHTML = '<span style="color:#10b981;">ACTIVE • 40MBPS MULTI-HOP</span>';
      if (intelTower) intelTower.innerHTML = '<span style="color:#ef4444;font-weight:700;">OFFLINE (0 CARRIER BARS)</span>';

      // Visual reaction in Three.js
      centralTower.beaconMat.color.setHex(0xef4444);
      centralTower.towerMat.color.setHex(0xef4444);
      centralTower.ring.material.color.setHex(0xef4444);
      jammerWave.material.opacity = 0.22;

      // Glow links to cyan/electric emerald
      links.forEach(l => {
        l.line.material.color.setHex(0x06b6d4);
        l.line.material.opacity = 0.65;
      });

      if (ticker) {
        ticker.innerHTML = `<span style="color:#f87171;">⚠️ 5G/4G Cell Jammer Triggered!</span> Standard phones disconnected. <strong>AirLink self-healed in 4ms via peer Wi-Fi Direct.</strong>`;
      }

      if (window.AirLinkAudio) {
        window.AirLinkAudio.playJammerAlarm();
      }
    } else {
      // JAMMER IS OFF
      if (btn) {
        btn.innerHTML = '⚡ TRIGGER CELLULAR JAMMER';
        btn.classList.remove('btn-secondary');
        btn.classList.add('btn-danger');
      }
      if (badge) {
        badge.className = 'sim-hud-badge badge-normal';
        badge.innerHTML = '<span class="status-beacon"></span> CELL TOWER: IDLE • AIRLINK MESH: ZERO DEPENDENCY';
      }
      if (intelStatus) intelStatus.innerHTML = '<span style="color:#10b981;">STANDBY • READY TO HOP</span>';
      if (intelTower) intelTower.innerHTML = '<span style="color:#94a3b8;">NORMAL (UNUSED BY AIRLINK)</span>';

      centralTower.beaconMat.color.setHex(0x38bdf8);
      centralTower.towerMat.color.setHex(0x475569);
      centralTower.ring.material.color.setHex(0x0284c7);
      jammerWave.material.opacity = 0.0;

      links.forEach(l => {
        l.line.material.color.setHex(0x10b981);
        l.line.material.opacity = 0.35;
      });

      if (ticker) {
        ticker.innerHTML = `Jammer deactivated. AirLink continues operating regardless because it never relied on towers.`;
      }
    }
  };

  // Trigger manual test packet
  window.sendAirLinkTestPacket = function () {
    for (let i = 0; i < 3; i++) {
      setTimeout(spawnMeshPacket, i * 150);
    }
  };

  function setupControls() {
    let isDragging = false;
    let prevMouseX = 0;
    let prevMouseY = 0;

    container.addEventListener('mousedown', (e) => {
      isDragging = true;
      prevMouseX = e.clientX;
      prevMouseY = e.clientY;
    });

    window.addEventListener('mouseup', () => {
      isDragging = false;
    });

    container.addEventListener('mousemove', (e) => {
      const rect = container.getBoundingClientRect();
      mouse.x = ((e.clientX - rect.left) / container.clientWidth) * 2 - 1;
      mouse.y = -((e.clientY - rect.top) / container.clientHeight) * 2 + 1;

      if (isDragging) {
        const deltaX = e.clientX - prevMouseX;
        const deltaY = e.clientY - prevMouseY;

        scene.rotation.y += deltaX * 0.005;
        camera.position.y = Math.max(20, Math.min(130, camera.position.y - deltaY * 0.3));
        camera.lookAt(0, 0, 0);

        prevMouseX = e.clientX;
        prevMouseY = e.clientY;
      }
    });

    // Touch support for mobile
    let touchStartX = 0;
    container.addEventListener('touchstart', (e) => {
      if (e.touches.length === 1) {
        touchStartX = e.touches[0].clientX;
      }
    });

    container.addEventListener('touchmove', (e) => {
      if (e.touches.length === 1) {
        const deltaX = e.touches[0].clientX - touchStartX;
        scene.rotation.y += deltaX * 0.004;
        touchStartX = e.touches[0].clientX;
      }
    });

    // Raycast on click
    container.addEventListener('click', () => {
      raycaster.setFromCamera(mouse, camera);
      const intersects = raycaster.intersectObjects(scene.children, true);

      for (let hit of intersects) {
        if (hit.object.userData && hit.object.userData.isNode) {
          const profile = PEER_PROFILES[hit.object.userData.profileIndex];
          displayNodeIntel(profile);
          sendAirLinkTestPacket();
          break;
        }
      }
    });
  }

  function displayNodeIntel(profile) {
    const intelNode = document.getElementById('intel-selected-node');
    const intelHop = document.getElementById('intel-hop-type');
    if (intelNode) intelNode.innerText = `${profile.name} (${profile.id})`;
    if (intelHop) intelHop.innerText = profile.isHost ? "Root (0 hops)" : (Math.random() > 0.5 ? "1-Hop Wi-Fi Direct" : "2-Hop Mesh Relay");
  }

  function onWindowResize() {
    if (!container || !renderer || !camera) return;
    const width = container.clientWidth;
    const height = container.clientHeight;
    camera.aspect = width / height;
    camera.updateProjectionMatrix();
    renderer.setSize(width, height);
  }

  function animate(timestamp) {
    animFrameId = requestAnimationFrame(animate);

    const time = timestamp ? timestamp * 0.001 : Date.now() * 0.001;

    // Gentle auto-rotation
    scene.rotation.y += 0.0015;

    // Node floating animation
    nodes.forEach((node) => {
      node.group.position.y = node.baseY + Math.sin(time * 1.5 + node.floatPhase) * 2;
      // Ring recovery
      if (node.ring.scale.x > 1) {
        node.ring.scale.multiplyScalar(0.96);
      }
    });

    // Central tower ring expansion
    if (centralTower && centralTower.ring) {
      centralTower.ring.scale.set(
        1 + Math.sin(time * 3) * 0.15,
        1 + Math.sin(time * 3) * 0.15,
        1
      );
    }

    // Jammer wave expansion
    if (isJammed && jammerWave) {
      jammerWave.rotation.y += 0.01;
      const pulse = 1 + Math.sin(time * 6) * 0.08;
      jammerWave.scale.set(pulse, pulse, pulse);
    }

    // Update connection lines to follow floating nodes
    links.forEach(l => {
      const fromPos = nodes[l.fromIdx].group.position;
      const toPos = nodes[l.toIdx].group.position;
      const posAttr = l.line.geometry.attributes.position;
      posAttr.setXYZ(0, fromPos.x, fromPos.y, fromPos.z);
      posAttr.setXYZ(1, toPos.x, toPos.y, toPos.z);
      posAttr.needsUpdate = true;
    });

    // Update traveling packets
    updatePackets();

    renderer.render(scene, camera);
  }

  // Initialize once DOM is ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
