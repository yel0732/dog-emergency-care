export function formatNotice(message) {
  return String(message || "")
    .replace(/\s+/g, " ")
    .replace(/((?:요|다|니다)\.|[.!?。])\s*/g, "$1\n")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
}
