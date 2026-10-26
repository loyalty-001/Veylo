import { createRemoteJWKSet, jwtVerify } from "jose";
import type { MiddlewareHandler } from "hono";
export type AppEnv = { Bindings: Env; Variables: { actor: string } };
const issuer = "https://loyaltyz.cloudflareaccess.com";
const keys = createRemoteJWKSet(new URL(`${issuer}/cdn-cgi/access/certs`));
export const adminGuard: MiddlewareHandler<AppEnv> = async (c, next) => {
  c.header("Cache-Control", "no-store");
  if (c.env.ADMIN_READY !== "true" || c.env.ACCESS_AUD === "not-configured")
    return c.json(
      { error: "Admin access is locked until MFA protection is configured." },
      503,
    );
  const token = c.req.header("Cf-Access-Jwt-Assertion");
  if (!token)
    return c.json({ error: "Sign in through Cloudflare Access." }, 401);
  try {
    const { payload } = await jwtVerify(token, keys, {
      issuer,
      audience: c.env.ACCESS_AUD,
      algorithms: ["RS256"],
      requiredClaims: ["exp", "iat", "sub", "email"],
    });
    if (payload.email !== c.env.OWNER_EMAIL || payload.type !== "app")
      return c.json({ error: "Forbidden" }, 403);
    c.set("actor", String(payload.email));
  } catch {
    return c.json({ error: "Invalid or expired sign-in." }, 401);
  }
  if (
    !["GET", "HEAD"].includes(c.req.method) &&
    (c.req.header("Origin") !== c.env.ADMIN_ORIGIN ||
      c.req.header("Content-Type")?.split(";")[0] !== "application/json")
  )
    return c.json({ error: "Request origin or content type rejected." }, 403);
  await next();
};
export const publicLimit: MiddlewareHandler<AppEnv> = async (c, next) => {
  const ip = c.req.header("CF-Connecting-IP") || "local";
  const { success } = await c.env.INGEST_LIMITER.limit({ key: ip });
  if (!success)
    return c.json({ error: "Too many requests. Try again later." }, 429, {
      "Retry-After": "60",
    });
  await next();
};
