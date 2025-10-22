package com.cadetex

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureOpenAPI() {
    routing {
        get("/swagger") {
            call.respondText(
                """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Cadetex API - Swagger UI</title>
                    <link rel="stylesheet" type="text/css" href="https://unpkg.com/swagger-ui-dist@4.15.5/swagger-ui.css" />
                    <style>
                        html { box-sizing: border-box; overflow: -moz-scrollbars-vertical; overflow-y: scroll; }
                        *, *:before, *:after { box-sizing: inherit; }
                        body { margin:0; background: #fafafa; }
                    </style>
                </head>
                <body>
                    <div id="swagger-ui"></div>
                    <script src="https://unpkg.com/swagger-ui-dist@4.15.5/swagger-ui-bundle.js"></script>
                    <script src="https://unpkg.com/swagger-ui-dist@4.15.5/swagger-ui-standalone-preset.js"></script>
                    <script>
                        window.onload = function() {
                            const ui = SwaggerUIBundle({
                                url: '/openapi/documentation.yaml',
                                dom_id: '#swagger-ui',
                                presets: [
                                    SwaggerUIBundle.presets.apis,
                                    SwaggerUIStandalonePreset
                                ],
                                layout: "StandaloneLayout",
                                deepLinking: true,
                                showExtensions: true,
                                showCommonExtensions: true
                            });
                        };
                    </script>
                </body>
                </html>
                """.trimIndent(),
                ContentType.Text.Html
            )
        }
        
        get("/openapi/documentation.yaml") {
            call.respondText(
                javaClass.getResource("/openapi/documentation.yaml")?.readText() ?: "",
                ContentType.Text.Plain
            )
        }
    }
}
