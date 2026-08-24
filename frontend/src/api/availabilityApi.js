import { request, requestEnvelope } from "./http";

/**
 * Backend: MentorAvailabilityController -> /api/mentor/availability
 *
 * This is where the slot grid comes from. Students see the union of what mentors
 * declare here - nothing is generated - so an hour nobody offers is an hour
 * nobody can book.
 */
export const availabilityApi = {
  /**
   * Declare a day's hours. Bulk, because a mentor thinks "Tuesday afternoon",
   * not "3 PM, then 4 PM".
   *
   * Partial success is normal: hours already booked, outside 9-9, or inside the
   * 24-hour notice period are skipped, and the response `message` names each one
   * and why. Re-sending an hour you already offered updates it.
   */
  declare: ({ date, hours, forInterviews, forMentoring, note }) =>
    requestEnvelope("/mentor/availability", {
      method: "POST",
      body: JSON.stringify({ date, hours, forInterviews, forMentoring, note }),
    }),

  mine: () => request("/mentor/availability"),

  /** Only while nobody is booked into it and it is still >24h away. */
  withdraw: (id) => request(`/mentor/availability/${id}`, { method: "DELETE" }),
};
