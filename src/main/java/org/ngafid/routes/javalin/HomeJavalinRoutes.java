package org.ngafid.routes.javalin;

import io.javalin.http.Context;
import org.ngafid.routes.MustacheHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HomeJavalinRoutes {
    private static final String homeTemplateFileName = "home.html";

    private static class Message {
        String type;
        String message;

        Message(String type, String message) {
            this.type = type;
            this.message = message;
        }
    }

    public static void getHome(Context ctx) throws IOException {
        Map<String, Object> scopes = new HashMap<String, Object>();
        if (ctx.queryParam("access_denied") != null) {
            scopes.put("access_denied", new Message("danger",
                    "You attempted to load a page you did not have access to " +
                            "or attempted to access a page while not logged in."));
        } else if (ctx.queryParam("logout_success") != null) {
            scopes.put("logout_success", new Message("success", "You have been successfully logged out."));
        }

        ctx.result(MustacheHandler.handle(homeTemplateFileName, scopes));
    }
}
