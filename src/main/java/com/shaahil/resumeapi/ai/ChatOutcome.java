package com.shaahil.resumeapi.ai;

/** How one chat request ended. */
public enum ChatOutcome {
    /** The model answered. */
    ANSWERED,
    /** The model call failed and the fallback reply was sent. */
    FALLBACK,
    /** The daily model-call cap was reached and the polite reply was sent. */
    DAILY_CAP,
    /** A greeting got the fixed welcome, with no model call. */
    WELCOME,
    /** The question was below the similarity floor and got the fixed decline, with no model call. */
    DECLINED,
    /** The visitor was over the rate limit and got 429. */
    RATE_LIMITED
}
