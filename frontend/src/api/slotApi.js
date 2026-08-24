import { request } from "./http";

/** Backend: SlotController -> /api/slots */
export const slotApi = {
  /** date is "yyyy-MM-dd". Returns every 1-hour slot that day, with availability. */
  forDate: (date) => request(`/slots?date=${encodeURIComponent(date)}`),
};
