/* ==========================================================================
   AirLink — Immersive Three.js Living Mesh & Scroll Choreography
   Simple, attractive, scroll-driven, humanized mesh network experience.
   ========================================================================== */

(function () {
  let scene, camera, renderer;
  let particles, particleGeo, particleMat;
  let nodes = [];
  let links = [];
  let packets = [];
  let jammerSphere = null;
  let radarWaveRings = [];

  // Mouse & Scroll State
  let mouse = { x: 0, y: 0, targetX: 0, targetY: 0 };
  let scrollProgress = 0;
  let isJammerActive = false;

  const bgCanvas = document.getElementById('webgl-background-canvas');
  if (!bgCanvas) return;

  // Initial Human Node Set
  const INITIAL_PEERS = [
    { name: "You", x: 0, y: 0, z: 0, isLocal: true },
    { name: "Maya", x: -65, y: 25, z: -30, isLocal: false },
    { name: "Alex", x: 55, y: -20, z: -20, isLocal: false },
    { name: "Kiran", x: -40, y: -45, z: 25, isLocal: false },
    { name: "Devin", x: 75, y: 35, z: 30, isLocal: false },
    { name: "Elena", x: 120, y: -10, z: -40, isLocal: false },
    { name: "Campus Relay", x: -110, y: 15, z: 50, isLocal: false }
  ];

  function init() {
    // 1. Scene & Camera
    scene = new THREE.Scene();
    scene.fog = new THREE.FogExp2(0x05070a, 0.0018);

    camera = new THREE.PerspectiveCamera(45, window.innerWidth / window.innerHeight, 1, 2000);
    camera.position.set(0, 15, 180);

    // 2. Renderer
    renderer = new THREE.WebGLRenderer({
      canvas: bgCanvas,
      antialias: true,
      alpha: true
    });
    renderer.setSize(window.innerWidth, window.innerHeight);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));

    // 3. Ambient & Point Lighting
    const ambient = new THREE.AmbientLight(0xffffff, 0.9);
    scene.add(ambient);

    const pointLight = new THREE.PointLight(0x10b981, 2.5, 400);
    pointLight.position.set(0, 50, 80);
    scene.add(pointLight);

    const cyanLight = new THREE.PointLight(0x06b6d4, 2, 400);
    cyanLight.position.set(80, -40, 50);
    scene.add(cyanLight);

    // 4. Create Ambient Cosmic / Radio Dust Field
    createRadioDustField();

    // 5. Create Central Radar Waves
    createRadarWaves();

    // 6. Create Jammer Disturbance Sphere (Initially Dormant)
    createJammerSphere();

    // 7. Spawn Human Mesh Nodes
    INITIAL_PEERS.forEach(peer => spawnNode(peer.x, peer.y, peer.z, peer.name, peer.isLocal));

    // 8. Rebuild Links
    rebuildLinks();

    // 9. Event Listeners
    window.addEventListener('resize', onResize);
    window.addEventListener('mousemove', onMouseMove);
    window.addEventListener('scroll', onScroll, { passive: true });

    // Periodic Packet Transmission
    setInterval(spawnDataPacket, 800);

    // Render loop
    animate();
  }

  // --- Luminous Radio Dust ---
  function createRadioDustField() {
    const count = 1200;
    const pos = new Float32Array(count * 3);

    for (let i = 0; i < count * 3; i += 3) {
      pos[i] = (Math.random() - 0.5) * 800;
      pos[i + 1] = (Math.random() - 0.5) * 600;
      pos[i + 2] = (Math.random() - 0.5) * 800;
    }

    particleGeo = new THREE.BufferGeometry();
    particleGeo.setAttribute('position', new THREE.BufferAttribute(pos, 3));

    particleMat = new THREE.PointsMaterial({
      color: 0x34d399,
      size: 1.8,
      transparent: true,
      opacity: 0.45,
      blending: THREE.AdditiveBlending
    });

    particles = new THREE.Points(particleGeo, particleMat);
    scene.add(particles);
  }

  // --- Radar Beacon Waves from Local Device ---
  function createRadarWaves() {
    for (let i = 0; i < 3; i++) {
      const geo = new THREE.RingGeometry(8 + i * 20, 9 + i * 20, 64);
      const mat = new THREE.MeshBasicMaterial({
        color: 0x10b981,
        side: THREE.DoubleSide,
        transparent: true,
        opacity: 0.3 - i * 0.08
      });
      const ring = new THREE.Mesh(geo, mat);
      ring.rotation.x = Math.PI / 2;
      scene.add(ring);
      radarWaveRings.push({ mesh: ring, baseScale: 1 + i * 0.5 });
    }
  }

  // --- Jammer Interference Sphere ---
  function createJammerSphere() {
    const geo = new THREE.SphereGeometry(45, 32, 32);
    const mat = new THREE.MeshBasicMaterial({
      color: 0xf43f5e,
      wireframe: true,
      transparent: true,
      opacity: 0.0
    });
    jammerSphere = new THREE.Mesh(geo, mat);
    jammerSphere.position.set(0, 0, -20);
    scene.add(jammerSphere);
  }

  // --- Spawn Node ---
  function spawnNode(x, y, z, name, isLocal) {
    const group = new THREE.Group();

    // Core Sphere
    const geo = new THREE.SphereGeometry(isLocal ? 5 : 3.8, 24, 24);
    const mat = new THREE.MeshStandardMaterial({
      color: isLocal ? 0x10b981 : 0x06b6d4,
      emissive: isLocal ? 0x059669 : 0x0284c7,
      emissiveIntensity: 0.7,
      roughness: 0.3,
      metalness: 0.6
    });
    const sphere = new THREE.Mesh(geo, mat);
    group.add(sphere);

    // Glowing Orbit Ring
    const ringGeo = new THREE.RingGeometry(5.5, 6.2, 32);
    const ringMat = new THREE.MeshBasicMaterial({
      color: isLocal ? 0x34d399 : 0x38bdf8,
      side: THREE.DoubleSide,
      transparent: true,
      opacity: 0.5
    });
    const ring = new THREE.Mesh(ringGeo, ringMat);
    ring.rotation.x = Math.PI / 2;
    group.add(ring);

    group.position.set(x, y, z);
    scene.add(group);

    nodes.push({
      group: group,
      sphere: sphere,
      ring: ring,
      name: name,
      isLocal: isLocal,
      baseY: y,
      baseX: x,
      floatPhase: Math.random() * Math.PI * 2,
      velocity: new THREE.Vector3(
        (Math.random() - 0.5) * 0.1,
        (Math.random() - 0.5) * 0.1,
        (Math.random() - 0.5) * 0.1
      )
    });
  }

  // --- Dynamic Distance-based Links ---
  function rebuildLinks() {
    // Clear old link meshes
    links.forEach(l => scene.remove(l.line));
    links = [];

    const maxDist = 130;
    for (let i = 0; i < nodes.length; i++) {
      for (let j = i + 1; j < nodes.length; j++) {
        const d = nodes[i].group.position.distanceTo(nodes[j].group.position);
        if (d < maxDist) {
          const points = [nodes[i].group.position, nodes[j].group.position];
          const geo = new THREE.BufferGeometry().setFromPoints(points);
          const mat = new THREE.LineBasicMaterial({
            color: isJammerActive ? 0x06b6d4 : 0x10b981,
            transparent: true,
            opacity: Math.max(0.15, (1 - d / maxDist) * 0.5)
          });
          const line = new THREE.Line(geo, mat);
          scene.add(line);
          links.push({ line: line, from: i, to: j });
        }
      }
    }
  }

  // --- Packets Traveling Across Mesh ---
  function spawnDataPacket() {
    if (nodes.length < 2) return;

    // Pick random connected pair
    if (links.length === 0) return;
    const link = links[Math.floor(Math.random() * links.length)];

    const packetGeo = new THREE.SphereGeometry(1.6, 12, 12);
    const packetMat = new THREE.MeshBasicMaterial({
      color: isJammerActive ? 0x38bdf8 : 0x34d399,
      transparent: true,
      opacity: 0.95
    });
    const packetMesh = new THREE.Mesh(packetGeo, packetMat);
    scene.add(packetMesh);

    packets.push({
      mesh: packetMesh,
      fromIdx: link.from,
      toIdx: link.to,
      progress: 0,
      speed: 0.02 + Math.random() * 0.015
    });
  }

  function updatePackets() {
    for (let i = packets.length - 1; i >= 0; i--) {
      const p = packets[i];
      const fromNode = nodes[p.fromIdx];
      const toNode = nodes[p.toIdx];

      if (!fromNode || !toNode) {
        scene.remove(p.mesh);
        packets.splice(i, 1);
        continue;
      }

      p.progress += p.speed;
      p.mesh.position.lerpVectors(fromNode.group.position, toNode.group.position, p.progress);

      // Gentle parabolic arc
      p.mesh.position.y += Math.sin(p.progress * Math.PI) * 3;

      if (p.progress >= 1) {
        scene.remove(p.mesh);
        packets.splice(i, 1);
        toNode.ring.scale.set(1.5, 1.5, 1.5);
      }
    }
  }

  // --- Public Interactive Controls for Website ---
  window.AirLink3D = {
    toggleJammer: function () {
      isJammerActive = !isJammerActive;
      const btn = document.getElementById('sandbox-jammer-toggle');
      const badge = document.getElementById('sandbox-status-text');

      if (isJammerActive) {
        if (btn) btn.innerHTML = '⚡ Deactivate Jammer (Restore RF)';
        if (badge) badge.innerHTML = '<span style="color:#f43f5e;">JAMMER DETECTED • MESH REROUTED VIA 5GHZ PEERS</span>';
        jammerSphere.material.opacity = 0.25;

        // Change link colors to electric cyan
        links.forEach(l => {
          l.line.material.color.setHex(0x06b6d4);
          l.line.material.opacity = 0.65;
        });
      } else {
        if (btn) btn.innerHTML = '⚡ Simulate Jammer Attack';
        if (badge) badge.innerHTML = '<span style="color:#10b981;">ALL RADIOS CLEAR • DIRECT PEER-TO-PEER</span>';
        jammerSphere.material.opacity = 0.0;

        links.forEach(l => {
          l.line.material.color.setHex(0x10b981);
          l.line.material.opacity = 0.35;
        });
      }
    },

    addHumanNode: function () {
      const names = ["Jordan", "Sam", "Dorm Node", "Library Peer", "Cafeteria Hotspot", "Trekker 4"];
      const randomName = names[Math.floor(Math.random() * names.length)];
      const x = (Math.random() - 0.5) * 160;
      const y = (Math.random() - 0.5) * 80;
      const z = (Math.random() - 0.5) * 120;

      spawnNode(x, y, z, randomName, false);
      rebuildLinks();

      const countEl = document.getElementById('sandbox-node-count');
      if (countEl) countEl.innerText = nodes.length;

      // Blast a few packets
      for (let i = 0; i < 4; i++) {
        setTimeout(spawnDataPacket, i * 120);
      }
    },

    broadcastVoicePing: function () {
      for (let i = 0; i < 6; i++) {
        setTimeout(spawnDataPacket, i * 80);
      }
    }
  };

  // --- Event Handlers ---
  function onMouseMove(e) {
    mouse.targetX = (e.clientX / window.innerWidth - 0.5) * 2;
    mouse.targetY = (e.clientY / window.innerHeight - 0.5) * 2;
  }

  function onScroll() {
    const maxScroll = document.documentElement.scrollHeight - window.innerHeight;
    scrollProgress = Math.max(0, Math.min(1, window.scrollY / (maxScroll || 1)));
  }

  function onResize() {
    camera.aspect = window.innerWidth / window.innerHeight;
    camera.updateProjectionMatrix();
    renderer.setSize(window.innerWidth, window.innerHeight);
  }

  // --- Main Animation Loop ---
  function animate(timestamp) {
    requestAnimationFrame(animate);

    const time = timestamp ? timestamp * 0.001 : Date.now() * 0.001;

    // Smooth Mouse Damping
    mouse.x += (mouse.targetX - mouse.x) * 0.05;
    mouse.y += (mouse.targetY - mouse.y) * 0.05;

    // Scroll Choreography (Camera smoothly journeys through the 3D human mesh)
    const targetCamX = mouse.x * 25 + Math.sin(scrollProgress * Math.PI * 2) * 40;
    const targetCamY = 15 - mouse.y * 20 - scrollProgress * 50;
    const targetCamZ = 180 - Math.sin(scrollProgress * Math.PI) * 70;

    camera.position.x += (targetCamX - camera.position.x) * 0.05;
    camera.position.y += (targetCamY - camera.position.y) * 0.05;
    camera.position.z += (targetCamZ - camera.position.z) * 0.05;
    camera.lookAt(0, -scrollProgress * 20, 0);

    // Particle field gentle rotation
    if (particles) {
      particles.rotation.y = time * 0.02;
    }

    // Radar Wave Expansion
    radarWaveRings.forEach((r, idx) => {
      const scale = 1 + ((time * 0.5 + idx * 0.33) % 1) * 2.8;
      r.mesh.scale.set(scale, scale, 1);
      r.mesh.material.opacity = Math.max(0, 0.4 * (1 - ((time * 0.5 + idx * 0.33) % 1)));
    });

    // Jammer Sphere Pulsing
    if (jammerSphere && isJammerActive) {
      const pulse = 1 + Math.sin(time * 6) * 0.06;
      jammerSphere.scale.set(pulse, pulse, pulse);
      jammerSphere.rotation.y = time * 0.2;
    }

    // Node Gentle Floating Physics
    nodes.forEach(n => {
      n.group.position.y = n.baseY + Math.sin(time * 1.6 + n.floatPhase) * 3;
      n.group.position.x += n.velocity.x;
      n.group.position.z += n.velocity.z;

      // Soft boundary bounce
      if (Math.abs(n.group.position.x - n.baseX) > 15) n.velocity.x *= -1;
      if (n.ring.scale.x > 1) {
        n.ring.scale.multiplyScalar(0.96);
      }
    });

    // Update Connection Lines
    links.forEach(l => {
      const posAttr = l.line.geometry.attributes.position;
      const fromPos = nodes[l.from].group.position;
      const toPos = nodes[l.to].group.position;
      posAttr.setXYZ(0, fromPos.x, fromPos.y, fromPos.z);
      posAttr.setXYZ(1, toPos.x, toPos.y, toPos.z);
      posAttr.needsUpdate = true;
    });

    // Update Traveling Packets
    updatePackets();

    renderer.render(scene, camera);
  }

  // Auto-init
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
