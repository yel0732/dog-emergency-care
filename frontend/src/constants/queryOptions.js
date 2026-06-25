import contract from "./query-options.contract.json";

function freezeOptions(options) {
  return Object.freeze(options.map((option) => Object.freeze({ ...option })));
}

export const QUERY_OPTION_CONTRACT = Object.freeze({
  defaults: Object.freeze({ ...contract.defaults }),
  directions: freezeOptions(contract.directions),
  videoSorts: freezeOptions(contract.videoSorts),
  caseSorts: freezeOptions(contract.caseSorts),
});

export const DIRECTION_OPTIONS = QUERY_OPTION_CONTRACT.directions;
export const VIDEO_SORT_OPTIONS = QUERY_OPTION_CONTRACT.videoSorts;
export const CASE_SORT_OPTIONS = QUERY_OPTION_CONTRACT.caseSorts;

export function optionValues(options) {
  return new Set(options.map((option) => option.value));
}
