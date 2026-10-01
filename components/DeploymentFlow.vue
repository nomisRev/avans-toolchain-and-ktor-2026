<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ step: number }>()
const stage = computed(() => Math.min(4, Math.max(0, props.step)))
const captions = [
  'Build the application and package it as a container image.',
  'Publish the image. Docker Hub stores it under a tag.',
  'Click Deploy in Render. Render pulls the image and starts the service.',
  'Replace the manual click: Docker Hub sends a POST to Render’s deploy hook.',
  'On the next push, the hook triggers Render to pull and deploy the updated image.',
]
</script>

<template>
  <figure class="deployment-flow" :class="{ automated: stage >= 3 }">
    <svg viewBox="0 0 1240 470" role="img" :aria-label="captions[stage]">
      <defs>
        <marker id="deploy-arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="9" markerHeight="9" orient="auto-start-reverse">
          <path d="M 1 1 L 9 5 L 1 9" fill="none" stroke="context-stroke" stroke-width="1.5" />
        </marker>
      </defs>

      <!-- The hook carries a notification; the image follows the lower path. -->
      <g class="reveal hook" :class="{ visible: stage >= 3 }">
        <path d="M 610 175 V 85 Q 610 65 630 65 H 1040 Q 1060 65 1060 85 V 175" class="hook-line" marker-end="url(#deploy-arrow)" />
        <rect x="684" y="42" width="300" height="45" rx="8" class="label-background" />
        <text x="834" y="70" class="hook-label">POST · deploy hook</text>
        <text x="834" y="111" class="detail">Docker Hub webhook</text>
      </g>

      <g class="node build-node">
        <rect x="35" y="175" width="250" height="165" rx="18" />
        <text x="160" y="218" class="eyebrow">GRADLE / TOOLCHAIN</text>
        <text x="160" y="262" class="node-title">Build &amp; publish</text>
        <text x="160" y="305" class="detail mono">publishImage</text>
      </g>

      <g class="connection" :class="{ active: stage >= 1 }">
        <path d="M 298 250 H 472" marker-end="url(#deploy-arrow)" />
        <text x="385" y="223" class="edge-label">push image</text>
      </g>

      <g class="node registry-node" :class="{ ready: stage >= 1 }">
        <rect x="485" y="175" width="250" height="165" rx="18" />
        <text x="610" y="218" class="eyebrow">IMAGE REGISTRY</text>
        <text x="610" y="262" class="node-title">Docker Hub</text>
        <text x="610" y="305" class="detail mono">:demo-1</text>
      </g>

      <g class="connection" :class="{ active: stage >= 2 }">
        <path d="M 748 250 H 922" marker-end="url(#deploy-arrow)" />
        <text x="835" y="223" class="edge-label">image layers</text>
        <path d="M 922 291 H 748" class="request-line" marker-end="url(#deploy-arrow)" />
        <text x="835" y="325" class="detail">pull image</text>
      </g>

      <g class="node render-node" :class="{ ready: stage >= 2, deployed: stage >= 4 }">
        <rect x="935" y="175" width="250" height="165" rx="18" />
        <text x="1060" y="218" class="eyebrow">RUNNING SERVICE</text>
        <text x="1060" y="262" class="node-title">Render</text>
        <text x="1060" y="305" class="detail">{{ stage >= 4 ? 'Updated · healthy' : stage >= 2 ? 'Running · HTTPS' : 'Waiting for image' }}</text>
      </g>

      <g class="manual-trigger" :class="{ hidden: stage >= 3 }">
        <path d="M 1060 116 V 160" marker-end="url(#deploy-arrow)" />
        <text x="1060" y="97" class="detail">Click Deploy</text>
      </g>

      <g class="reveal" :class="{ visible: stage >= 3 }">
        <text x="610" y="390" class="detail">Same image tag</text>
        <text x="1060" y="390" class="detail">Automatic deploy</text>
      </g>

      <!-- One-shot particles replay on click, including when stepping backwards. -->
      <g v-if="stage > 0" :key="stage" class="moving-particles" aria-hidden="true">
        <rect v-if="stage === 1 || stage === 4" x="-8" y="-8" width="16" height="16" rx="3" class="image-particle">
          <animate attributeName="opacity" values="0;1;1;0" keyTimes="0;0.01;0.95;1" dur="0.7s" fill="freeze" />
          <animateMotion dur="0.7s" path="M 298 250 H 472" fill="freeze" />
        </rect>
        <circle v-if="stage >= 3" r="7" class="hook-particle">
          <animate attributeName="opacity" values="0;1;1;0" keyTimes="0;0.01;0.95;1" :begin="stage === 4 ? '0.75s' : '0s'" dur="0.8s" fill="freeze" />
          <animateMotion :begin="stage === 4 ? '0.75s' : '0s'" dur="0.8s" path="M 610 175 V 85 Q 610 65 630 65 H 1040 Q 1060 65 1060 85 V 175" fill="freeze" />
        </circle>
        <rect v-if="stage === 2 || stage === 4" x="-8" y="-8" width="16" height="16" rx="3" class="image-particle">
          <animate attributeName="opacity" values="0;1;1;0" keyTimes="0;0.01;0.95;1" :begin="stage === 4 ? '1.6s' : '0.2s'" dur="0.7s" fill="freeze" />
          <animateMotion :begin="stage === 4 ? '1.6s' : '0.2s'" dur="0.7s" path="M 748 250 H 922" fill="freeze" />
        </rect>
      </g>

      <text x="35" y="453" class="step-counter">{{ stage + 1 }} / 5</text>
      <text x="1185" y="453" class="mode-label">{{ stage >= 3 ? 'WEBHOOK TRIGGER' : 'MANUAL TRIGGER' }}</text>
    </svg>
    <figcaption aria-live="polite">{{ captions[stage] }}</figcaption>
  </figure>
</template>

<style scoped>
.deployment-flow { margin: 0; --ink: #272338; --surface: #fff; --muted: #736c85; --accent: #7954f6; --hook: #bd39b8; --faint: #dcd6e9; }
:global(html.dark) .deployment-flow { --ink: #f1ecff; --surface: #191622; --muted: #b8aecb; --accent: #b49aff; --hook: #f388ea; --faint: #514760; }
svg { display: block; width: 100%; overflow: visible; }
text { fill: var(--ink); text-anchor: middle; font-family: 'JetBrains Sans', sans-serif; }
.node rect { fill: var(--surface); stroke: var(--faint); stroke-width: 2.5; transition: stroke 450ms, fill 450ms; }
.build-node rect, .node.ready rect { stroke: var(--accent); }
.node.deployed rect { fill: color-mix(in srgb, var(--accent) 10%, var(--surface)); }
.node-title { font-size: 28px; font-weight: 700; }
.eyebrow { font-size: 14px; letter-spacing: 1.5px; fill: var(--muted); }
.detail { font-size: 20px; fill: var(--muted); }
.mono { font-family: 'JetBrains Mono', monospace; font-size: 19px; }
.edge-label { font-size: 21px; }
.connection { opacity: 0.24; transition: opacity 450ms; }
.connection.active { opacity: 1; }
.connection path { fill: none; stroke: var(--accent); stroke-width: 3; }
.connection .request-line { stroke: var(--muted); stroke-width: 2; }
.reveal { opacity: 0; transition: opacity 450ms; }
.reveal.visible { opacity: 1; }
.hook-line { fill: none; stroke: var(--hook); stroke-width: 3; stroke-dasharray: 8 7; }
.hook-label { fill: var(--hook); font-size: 23px; font-weight: 600; }
.label-background { fill: var(--surface); }
.manual-trigger { opacity: 1; transition: opacity 300ms, transform 450ms; }
.manual-trigger path { fill: none; stroke: var(--muted); stroke-width: 2; }
.manual-trigger.hidden { opacity: 0; transform: translateY(-18px); }
.image-particle { fill: var(--accent); opacity: 0; }
.hook-particle { fill: var(--hook); opacity: 0; }
.step-counter { text-anchor: start; font-size: 17px; fill: var(--muted); }
.mode-label { text-anchor: end; font-size: 15px; letter-spacing: 1.5px; fill: var(--muted); }
figcaption { margin-top: 0.5rem; min-height: 5rem; font-size: 1.8rem; line-height: 1.45; }
@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after { transition: none !important; }
  .moving-particles { display: none; }
}
</style>
