package com.learn.interviewmentor.model;

/**
 * FILE -> uploaded to us and streamed back through an endpoint that checks who
 *         is asking. The bytes never sit in a public directory.
 * LINK -> just a URL (a YouTube playlist, a Drive folder). Nothing is stored.
 */
public enum MaterialKind {
    FILE,
    LINK
}
