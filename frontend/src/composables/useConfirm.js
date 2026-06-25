import { reactive } from "vue";

const state = reactive({
  open: false,
  title: "확인",
  message: "",
  confirmText: "확인",
  cancelText: "취소",
  tone: "danger",
  resolver: null,
});

export function useConfirm() {
  function ask(options = {}) {
    if (state.resolver) state.resolver(false);
    Object.assign(state, {
      open: true,
      title: options.title || "확인",
      message: options.message || "",
      confirmText: options.confirmText || "확인",
      cancelText: options.cancelText || "취소",
      tone: options.tone || "danger",
      resolver: null,
    });
    return new Promise((resolve) => {
      state.resolver = resolve;
    });
  }

  function close(result) {
    const resolve = state.resolver;
    Object.assign(state, {
      open: false,
      resolver: null,
    });
    if (resolve) resolve(result);
  }

  return { confirmState: state, ask, close };
}
