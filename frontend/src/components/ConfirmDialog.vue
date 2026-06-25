<script setup>
import { onBeforeUnmount, onMounted } from "vue";
import { useConfirm } from "../composables/useConfirm";

const { confirmState, close } = useConfirm();

function onKeydown(event) {
  if (!confirmState.open) return;
  if (event.key === "Escape") close(false);
}

onMounted(() => window.addEventListener("keydown", onKeydown));
onBeforeUnmount(() => window.removeEventListener("keydown", onKeydown));
</script>

<template>
  <Teleport to="body">
    <Transition name="confirm-pop">
      <div v-if="confirmState.open" class="app-confirm-backdrop" @click.self="close(false)">
        <section class="app-confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="app-confirm-title">
          <div class="app-confirm-icon" :class="confirmState.tone" aria-hidden="true">!</div>
          <div class="app-confirm-copy">
            <h2 id="app-confirm-title">{{ confirmState.title }}</h2>
            <p>{{ confirmState.message }}</p>
          </div>
          <div class="app-confirm-actions">
            <button type="button" class="confirm-cancel" @click="close(false)">
              {{ confirmState.cancelText }}
            </button>
            <button type="button" class="confirm-submit" :class="confirmState.tone" autofocus @click="close(true)">
              {{ confirmState.confirmText }}
            </button>
          </div>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>
