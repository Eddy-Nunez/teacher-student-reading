package com.scholastic.portal.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the React (Vite) SPA from the same origin as the API in production.
 *
 * The built frontend lives under {@code classpath:/static/} (baked into the jar at build time),
 * so one process serves both the UI and /api — no CORS, and the cookie/CSRF auth works on a
 * single origin. This controller forwards client-side routes (e.g. /student/assignments/1) to
 * index.html so React Router handles them; it must not capture /api or real static assets —
 * those are handled by higher-precedence controller/resource mappings (paths with no dot).
 */
@Controller
public class SpaForwardController {

    @GetMapping(value = {
            "/",
            "/{p1:[^\\.]*}",
            "/{p1:[^\\.]*}/{p2:[^\\.]*}",
            "/{p1:[^\\.]*}/{p2:[^\\.]*}/{p3:[^\\.]*}"
    })
    public String forward() {
        return "forward:/index.html";
    }
}