<script setup>
import { computed } from "vue";
import { formatNotice } from "../utils/messageFormat";

const props = defineProps({
  show: { type: Boolean, default: false },
  type: { type: String, default: "info" },
  message: { type: String, default: "" },
});

defineEmits(["close"]);

const icon = computed(() => (props.type === "success" ? "✓" : "!"));
const formattedMessage = computed(() => formatNotice(props.message));
</script>

<template>
  <Transition name="toast-pop">
    <div v-if="show" class="floating-toast" :class="type" role="status">
      <span aria-hidden="true">{{ icon }}</span>
      <p>{{ formattedMessage }}</p>
      <button type="button" aria-label="닫기" @click="$emit('close')">×</button>
    </div>
  </Transition>
</template>
