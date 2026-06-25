export async function resyncAfterMutationFailure({
  message,
  reload,
  refreshDetail,
  onDetailMissing,
  setMessage,
}) {
  try {
    if (typeof reload === "function") await reload();
    if (typeof refreshDetail === "function") await refreshDetail();
  } catch (error) {
    if (typeof onDetailMissing === "function") onDetailMissing(error);
  } finally {
    if (typeof setMessage === "function") setMessage(message);
  }
}

export function createMutationResync({
  reload,
  refreshDetail,
  isDetailMissing,
  onDetailMissing,
  setMessage,
}) {
  return async function recover(message, context) {
    await resyncAfterMutationFailure({
      message,
      reload: () => reload?.(context),
      refreshDetail: refreshDetail ? () => refreshDetail(context) : undefined,
      onDetailMissing: (error) => {
        if (!isDetailMissing || isDetailMissing(error, context)) {
          onDetailMissing?.(error, context);
        }
      },
      setMessage,
    });
  };
}
