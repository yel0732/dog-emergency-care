<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from "vue";

const props = defineProps({
  modelValue: { type: [String, Number], default: "" },
  options: { type: Array, default: () => [] }, // ["최신순"] 또는 [{value, label}]
  ariaLabel: { type: String, default: "선택" },
  placeholder: { type: String, default: "선택" },
});
const emit = defineEmits(["update:modelValue", "change"]);

const open = ref(false);
const root = ref(null);

const normalized = computed(() =>
  props.options.map((o) => (typeof o === "object" ? o : { value: o, label: String(o) }))
);
const selectedLabel = computed(() => {
  const found = normalized.value.find((o) => String(o.value) === String(props.modelValue));
  return found ? found.label : props.placeholder;
});

function toggle() {
  open.value = !open.value;
}
function select(option) {
  emit("update:modelValue", option.value);
  emit("change", option.value);
  open.value = false;
}
function onClickOutside(e) {
  if (root.value && !root.value.contains(e.target)) open.value = false;
}
function onKeydown(e) {
  if (e.key === "Escape") open.value = false;
}

onMounted(() => {
  document.addEventListener("click", onClickOutside);
  document.addEventListener("keydown", onKeydown);
});
onBeforeUnmount(() => {
  document.removeEventListener("click", onClickOutside);
  document.removeEventListener("keydown", onKeydown);
});
</script>

<template>
  <div ref="root" class="csel" :class="{ open }" @click.stop>
    <button
      type="button"
      class="csel-trigger"
      :aria-label="ariaLabel"
      :aria-expanded="open"
      @click.prevent.stop="toggle"
    >
      <span class="csel-value">{{ selectedLabel }}</span>
      <span class="csel-arrow" aria-hidden="true">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"/></svg>
      </span>
    </button>
    <transition name="csel-pop">
      <ul v-if="open" class="csel-menu" role="listbox">
        <li
          v-for="option in normalized"
          :key="option.value"
          class="csel-option"
          :class="{ selected: String(option.value) === String(modelValue) }"
          role="option"
          :aria-selected="String(option.value) === String(modelValue)"
          @mousedown.prevent.stop="select(option)"
          @click.prevent.stop
        >
          {{ option.label }}
        </li>
      </ul>
    </transition>
  </div>
</template>
