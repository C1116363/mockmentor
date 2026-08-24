/**
 * Formatting shared across features. No React, no fetch, no feature knowledge.
 *
 * formatPrice started life in planRules, which was wrong the moment projects
 * needed it too - Indian digit grouping is not a rule about plans.
 */

/** 2999 -> "2,999". Indian grouping: 100000 -> "1,00,000". */
export const formatPrice = (value) => Number(value).toLocaleString("en-IN");
