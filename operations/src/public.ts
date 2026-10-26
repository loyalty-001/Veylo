import { Hono } from "hono";
import { z } from "zod";
import {
  eventSchema,
  reportSchema,
  wireAnnouncement,
  type Announcement,
  type Controls,
} from "./schema";
import { releases } from "./releases";
import { publicLimit, type AppEnv } from "./security";
export const publicApi = new Hono<AppEnv>();
publicApi.use("*", publicLimit);
publicApi.get("/config", async (c) => {
  const code = z.coerce
    .number()
    .int()
    .min(0)
    .max(2147483647)
    .parse(c.req.query("code") ?? 0);
  const controls = await c.env.DB.prepare(
    "SELECT * FROM controls WHERE id=1",
  ).first<Controls>();
  const now = Date.now();
  const notes = controls?.announcements_enabled
    ? await c.env.DB.prepare(
        "SELECT * FROM announcements WHERE status='published' AND starts_at<=? AND ends_at>? AND min_code<=? AND max_code>=? ORDER BY updated_at DESC LIMIT 10",
      )
        .bind(now, now, code, code)
        .all<Announcement>()
    : { results: [] };
  const held = await c.env.DB.prepare(
    "SELECT tag FROM held_releases ORDER BY held_at DESC LIMIT 100",
  ).all<{ tag: string }>();
  c.header("Cache-Control", "no-store");
  return c.json({
    serverTime: now,
    analyticsEnabled: !!controls?.analytics_enabled,
    reportsEnabled: !!controls?.reports_enabled,
    heldReleases: held.results.map((x) => x.tag),
    announcements: notes.results.map(wireAnnouncement),
  });
});
publicApi.post("/events", async (c) => {
  const e = eventSchema.parse(await c.req.json());
  const enabled = await c.env.DB.prepare(
    "SELECT analytics_enabled FROM controls WHERE id=1",
  ).first<{ analytics_enabled: number }>();
  if (!enabled?.analytics_enabled) return c.body(null, 204);
  const day = new Date().toISOString().slice(0, 10);
  if (e.category === "launch" || e.category === "first_launch") {
    const statements = [
      c.env.DB.prepare(
        "INSERT INTO daily_installs(day,install_id,version,code,platform) VALUES(?,?,?,?,?) ON CONFLICT(day,install_id) DO UPDATE SET version=excluded.version,code=excluded.code,platform=excluded.platform",
      ).bind(day, e.installId, e.version, e.code, e.platform),
    ];
    if (e.category === "first_launch")
      statements.push(
        c.env.DB.prepare(
          "INSERT OR IGNORE INTO first_launches(install_id,day,version) VALUES(?,?,?)",
        ).bind(e.installId, day, e.version),
      );
    await c.env.DB.batch(statements);
  } else
    await c.env.DB.prepare(
      "INSERT INTO health_counts(day,version,category,count) VALUES(?,?,?,1) ON CONFLICT(day,version,category) DO UPDATE SET count=count+1",
    )
      .bind(day, e.version, e.category)
      .run();
  return c.body(null, 204);
});
publicApi.post("/forget", async (c) => {
  const { installId } = z
    .object({ installId: z.uuid() })
    .strict()
    .parse(await c.req.json());
  await c.env.DB.batch([
    c.env.DB.prepare("DELETE FROM daily_installs WHERE install_id=?").bind(
      installId,
    ),
    c.env.DB.prepare("DELETE FROM first_launches WHERE install_id=?").bind(
      installId,
    ),
  ]);
  return c.body(null, 204);
});
publicApi.post("/reports", async (c) => {
  const r = reportSchema.parse(await c.req.json());
  const enabled = await c.env.DB.prepare(
    "SELECT reports_enabled FROM controls WHERE id=1",
  ).first<{ reports_enabled: number }>();
  if (!enabled?.reports_enabled)
    return c.json({ error: "Reports are temporarily paused." }, 503);
  const id = crypto.randomUUID();
  await c.env.DB.prepare(
    "INSERT INTO reports(id,category,message,version,code,platform,created_at) VALUES(?,?,?,?,?,?,?)",
  )
    .bind(id, r.category, r.message, r.version, r.code, r.platform, Date.now())
    .run();
  return c.json({ id }, 201);
});
export const downloads = new Hono<AppEnv>();
const variants: Record<string, string> = {
  universal: "Veylo.apk",
  "arm64-v8a": "veylo-arm64-v8a.apk",
  "armeabi-v7a": "veylo-armeabi-v7a.apk",
  x86: "veylo-x86.apk",
  x86_64: "veylo-x86_64.apk",
};
downloads.get("/:variant", (c) => {
  const name = variants[c.req.param("variant")];
  if (!name) return c.notFound();
  c.header("Cache-Control", "no-store");
  return c.redirect(
    `https://github.com/loyalty-001/Veylo/releases/latest/download/${name}`,
    302,
  );
});
publicApi.post("/download-click", async (c) => {
  const { variant } = z
    .object({
      variant: z.enum([
        "universal",
        "arm64-v8a",
        "armeabi-v7a",
        "x86",
        "x86_64",
      ]),
    })
    .strict()
    .parse(await c.req.json());
  await c.env.DB.prepare(
    "INSERT INTO download_clicks(day,variant,count) VALUES(?,?,1) ON CONFLICT(day,variant) DO UPDATE SET count=count+1",
  )
    .bind(new Date().toISOString().slice(0, 10), variant)
    .run();
  return c.body(null, 204);
});
export async function cleanup(db: D1Database) {
  const day = new Date(Date.now() - 30 * 86400000).toISOString().slice(0, 10);
  const firstDay = new Date(Date.now() - 90 * 86400000)
    .toISOString()
    .slice(0, 10);
  await db.batch([
    db.prepare("DELETE FROM daily_installs WHERE day<?").bind(day),
    db.prepare("DELETE FROM first_launches WHERE day<?").bind(firstDay),
    db
      .prepare("DELETE FROM reports WHERE created_at<?")
      .bind(Date.now() - 90 * 86400000),
    db
      .prepare("DELETE FROM audit_log WHERE at<?")
      .bind(Date.now() - 180 * 86400000),
  ]);
  await releases(db);
}
