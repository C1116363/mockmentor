import { request } from "./http";

/** Backend: SlotController -> /api/slots */
export const slotApi = {
  /**
   * The hours a mentor has offered on one day.
   *
   * `sessionType` matters: a mentor may offer an hour for mock interviews but not
   * for mentoring discussions, so the two grids genuinely differ. Nothing here is
   * generated - an empty list means nobody put their hand up for that day.
   */
  forDate: (date, sessionType) =>
    request(`/slots?date=${encodeURIComponent(date)}`
      + (sessionType ? `&sessionType=${encodeURIComponent(sessionType)}` : "")),
};
