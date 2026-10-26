import { Hono } from "hono";
import { bodyLimit } from "hono/body-limit";
import { cors } from "hono/cors";
import { secureHeaders } from "hono/secure-headers";
import { ZodError } from "zod";
import { adminGuard, type AppEnv } from "./security";
import { adminApi } from "./admin";
import { publicApi, downloads } from "./public";
import { adminHtml, adminScript, adminCss, favicon } from "./ui";
export const app = new Hono<AppEnv>();
app.use(
  "*",
  secureHeaders({
    contentSecurityPolicy: {
      defaultSrc: ["'none'"],
      scriptSrc: ["'self'"],
      styleSrc: ["'self'"],
      connectSrc: ["'self'"],
      imgSrc: ["'self'", "data:"],
      baseUri: ["'none'"],
      frameAncestors: ["'none'"],
      formAction: ["'self'"],
    },
    referrerPolicy: "no-referrer",
    permissionsPolicy: { camera: [], microphone: [], geolocation: [] },
  }),
);
app.use(
  "*",
  bodyLimit({
    maxSize: 8192,
    onError: (c) => c.json({ error: "Request too large." }, 413),
  }),
);
app.use(
  "/v1/*",
  cors({
    origin: "https://veylo-lra9.onrender.com",
    allowMethods: ["GET", "POST", "OPTIONS"],
    allowHeaders: ["Content-Type"],
    credentials: false,
  }),
);
app.use("/admin", adminGuard);
app.use("/admin/*", adminGuard);
app.route("/admin/api", adminApi);
app.get("/admin", (c) => c.html(adminHtml));
app.get("/admin/favicon.svg", (c) =>
  c.body(favicon, 200, { "Content-Type": "image/svg+xml" }),
);
app.get("/admin/app.js", (c) =>
  c.body(adminScript, 200, { "Content-Type": "text/javascript;charset=utf-8" }),
);
app.get("/admin/app.css", (c) =>
  c.body(adminCss, 200, { "Content-Type": "text/css;charset=utf-8" }),
);
app.route("/v1", publicApi);
app.route("/download", downloads);
app.get("/healthz", (c) => c.json({ ok: true }));
app.get("/", (c) => c.redirect("https://veylo-lra9.onrender.com", 302));
app.onError((e, c) =>
  e instanceof ZodError
    ? c.json({ error: e.issues.map((x) => x.message).join(" ") }, 400)
    : c.json({ error: "Service unavailable. Try again later." }, 503),
);
