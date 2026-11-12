package com.gu.play.test

import play.api.http.{HeaderNames, HttpVerbs, Status}
import play.api.test.{DefaultAwaitTimeout, EssentialActionCaller, ResultExtractors, StubControllerComponentsFactory, Writeables}

// Helpers also mixes in PlayRunners, which eagerly loads Selenium's browser drivers.
object NonBrowserHelpers
    extends HeaderNames
    with HttpVerbs
    with Status
    with DefaultAwaitTimeout
    with ResultExtractors
    with Writeables
    with EssentialActionCaller
    with StubControllerComponentsFactory
