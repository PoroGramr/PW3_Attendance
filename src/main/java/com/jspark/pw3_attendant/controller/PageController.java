package com.jspark.pw3_attendant.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PageController {

    /**
     * Serves the student's QR code display page.
     * The actual data fetching and QR generation is handled client-side by JavaScript
     * in the qr-display.html file.
     *
     * @param qrSecret The student's unique QR secret from the URL path.
     * @return A forward to the static HTML page.
     */
    @GetMapping("/s/{qrSecret}")
    public String showStudentQrPage(@PathVariable String qrSecret) {
        // We don't need to do anything with the qrSecret here.
        // The view (HTML+JS) will extract it from the URL and call the API.
        return "forward:/qr-display.html";
    }
}
