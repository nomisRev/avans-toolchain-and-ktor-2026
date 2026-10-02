<script setup>
import { computed } from 'vue'

const props = defineProps({
  /** Name of the plug-in drawn next to the handler on both rows; empty keeps the anonymous `Plug-in` cards. */
  plugin: { type: String, default: '' },
})

/** Splits a camel-case plug-in name into the words a card may wrap between. */
const pluginWords = computed(() => props.plugin.split(/(?=[A-Z])/))
</script>

<template>
  <div
    class="ktor-pipeline"
    role="img"
    :aria-label="plugin
      ? `A request enters Routing, passes through the installed plug-ins, the last of them ${plugin}, and reaches the handler; the response leaves the handler and passes back through ${plugin} and the other plug-ins.`
      : 'A request enters Routing, passes through the installed plug-ins and reaches the handler; the response leaves the handler and passes back through the plug-ins.'"
  >
    <!-- Request row: left to right -->
    <div class="pipeline-card pipeline-card--message">request</div>
    <div class="pipeline-arrow pipeline-arrow--right" aria-hidden="true"><i></i><svg viewBox="0 0 12 20"><path d="M2 2 L10 10 L2 18" /></svg></div>
    <div class="pipeline-card pipeline-card--plugin pipeline-card--routing">Routing</div>
    <div class="pipeline-arrow pipeline-arrow--right" aria-hidden="true"><i></i><svg viewBox="0 0 12 20"><path d="M2 2 L10 10 L2 18" /></svg></div>
    <div class="pipeline-card pipeline-card--plugin">Plug-in</div>
    <div class="pipeline-arrow pipeline-arrow--right" aria-hidden="true"><i></i><svg viewBox="0 0 12 20"><path d="M2 2 L10 10 L2 18" /></svg></div>
    <div class="pipeline-card pipeline-card--more">…</div>
    <div class="pipeline-arrow pipeline-arrow--right" aria-hidden="true"><i></i><svg viewBox="0 0 12 20"><path d="M2 2 L10 10 L2 18" /></svg></div>
    <div v-if="plugin" class="pipeline-card pipeline-card--plugin pipeline-card--named"><span><template v-for="(word, index) in pluginWords" :key="index"><wbr v-if="index > 0">{{ word }}</template></span></div>
    <div v-else class="pipeline-card pipeline-card--plugin">Plug-in</div>
    <div class="pipeline-arrow pipeline-arrow--right" aria-hidden="true"><i></i><svg viewBox="0 0 12 20"><path d="M2 2 L10 10 L2 18" /></svg></div>
    <div class="pipeline-card pipeline-card--handler">handler</div>

    <!-- Turn: the handler's result becomes the response -->
    <div class="pipeline-turn" aria-hidden="true"><i></i><svg viewBox="0 0 20 12"><path d="M2 2 L10 10 L18 2" /></svg></div>

    <!-- Response row: right to left -->
    <div class="pipeline-card pipeline-card--message pipeline-card--response">response</div>
    <div class="pipeline-arrow pipeline-arrow--left pipeline-arrow--long" aria-hidden="true"><svg viewBox="0 0 12 20"><path d="M10 2 L2 10 L10 18" /></svg><i></i></div>
    <div class="pipeline-card pipeline-card--plugin">Plug-in</div>
    <div class="pipeline-arrow pipeline-arrow--left" aria-hidden="true"><svg viewBox="0 0 12 20"><path d="M10 2 L2 10 L10 18" /></svg><i></i></div>
    <div class="pipeline-card pipeline-card--more">…</div>
    <div class="pipeline-arrow pipeline-arrow--left" aria-hidden="true"><svg viewBox="0 0 12 20"><path d="M10 2 L2 10 L10 18" /></svg><i></i></div>
    <div v-if="plugin" class="pipeline-card pipeline-card--plugin pipeline-card--named"><span><template v-for="(word, index) in pluginWords" :key="index"><wbr v-if="index > 0">{{ word }}</template></span></div>
    <div v-else class="pipeline-card pipeline-card--plugin">Plug-in</div>
    <div class="pipeline-arrow pipeline-arrow--left" aria-hidden="true"><svg viewBox="0 0 12 20"><path d="M10 2 L2 10 L10 18" /></svg><i></i></div>
    <div class="pipeline-card pipeline-card--handler">handler</div>
  </div>
</template>

<style scoped>
.ktor-pipeline {
  --pipeline-ink: var(--fundamentals-ink, #1f2023);
  --pipeline-purple: var(--fundamentals-purple, #7954f6);
  --pipeline-pink: var(--fundamentals-pink, #eb55e6);
  --pipeline-surface: rgb(255 255 255 / 90%);
  --pipeline-arrow: 2.6rem;
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr) var(--pipeline-arrow)) minmax(0, 1fr);
  grid-template-rows: 5.2rem 3.4rem 5.2rem;
  width: min(100%, 74rem);
  margin: 2.4rem auto 1rem;
  isolation: isolate;
}

.pipeline-card {
  display: flex;
  box-sizing: border-box;
  align-items: center;
  justify-content: center;
  border: 1px solid rgb(121 84 246 / 42%);
  border-radius: 1rem;
  background: var(--pipeline-surface);
  box-shadow: 0 0.55rem 1.45rem rgb(31 32 35 / 9%);
  color: var(--pipeline-ink);
  font-size: 1.55rem;
  font-weight: 800;
  letter-spacing: -0.035em;
}

.pipeline-card--message {
  background: linear-gradient(110deg, rgb(121 84 246 / 13%), rgb(235 85 230 / 10%));
  font-family: var(--slidev-font-mono);
  font-size: 1.35rem;
  font-weight: 700;
  letter-spacing: -0.02em;
}
.pipeline-card--plugin { color: var(--pipeline-purple); }
.pipeline-card--routing {
  border-color: rgb(235 85 230 / 48%);
  background: linear-gradient(135deg, rgb(235 85 230 / 11%), var(--pipeline-surface));
  color: #b731ae;
}
.pipeline-card--named {
  border-color: var(--pipeline-purple);
  background: linear-gradient(135deg, rgb(121 84 246 / 14%), var(--pipeline-surface));
  font-family: var(--slidev-font-mono);
  font-size: 1.1rem;
  font-weight: 700;
  line-height: 1.15;
  letter-spacing: -0.02em;
  text-align: center;
}
.pipeline-card--more {
  border-style: dashed;
  box-shadow: none;
  color: var(--pipeline-pink);
  font-size: 2rem;
}
.pipeline-card--handler {
  border-color: var(--pipeline-purple);
  background: var(--pipeline-purple);
  color: #fff;
  font-family: var(--slidev-font-mono);
  font-size: 1.35rem;
  font-weight: 700;
  letter-spacing: -0.02em;
}

/* Arrows: a line that fills its cell, a chevron at the pointing end. */
.pipeline-arrow {
  display: flex;
  align-items: center;
  padding: 0 0.3rem;
  color: var(--pipeline-purple);
}
.pipeline-arrow i {
  display: block;
  flex: 1 1 0;
  height: 3px;
  border-radius: 2px;
  background: currentColor;
  opacity: 0.85;
}
.pipeline-arrow svg {
  flex: 0 0 auto;
  width: 0.7rem;
  height: 1.15rem;
  fill: none;
  stroke: currentColor;
  stroke-width: 2.6;
  stroke-linecap: round;
  stroke-linejoin: round;
  opacity: 0.85;
}
.pipeline-arrow--right svg { margin-left: -2px; }
.pipeline-arrow--left { color: var(--pipeline-pink); }
.pipeline-arrow--left svg { margin-right: -2px; }
.pipeline-arrow--long { grid-column: 2 / span 3; }

/* The vertical turn under the handler, from the request row to the response row. */
.pipeline-turn {
  display: flex;
  flex-direction: column;
  grid-column: 11;
  align-items: center;
  padding: 0.35rem 0;
  color: var(--pipeline-pink);
}
.pipeline-turn i {
  display: block;
  flex: 1 1 0;
  width: 3px;
  border-radius: 2px;
  background: currentColor;
  opacity: 0.85;
}
.pipeline-turn svg {
  flex: 0 0 auto;
  width: 1.15rem;
  height: 0.7rem;
  margin-top: -2px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2.6;
  stroke-linecap: round;
  stroke-linejoin: round;
  opacity: 0.85;
}

/* Response row placement: response sits under request, then a long arrow
   back from the first plug-in; the handler stays under the handler. */
.pipeline-card--response { grid-column: 1; }

html.dark .ktor-pipeline { --pipeline-surface: rgb(35 34 42 / 92%); }
html.dark .pipeline-card { box-shadow: 0 0.55rem 1.45rem rgb(0 0 0 / 24%); }
html.dark .pipeline-card--named {
  border-color: var(--pipeline-purple);
  background: linear-gradient(135deg, rgb(121 84 246 / 14%), var(--pipeline-surface));
  font-family: var(--slidev-font-mono);
  font-size: 1.1rem;
  font-weight: 700;
  line-height: 1.15;
  letter-spacing: -0.02em;
  text-align: center;
}
.pipeline-card--more { box-shadow: none; }
html.dark .pipeline-card--routing { color: var(--pipeline-pink); }
</style>
