import { test } from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { JSDOM } from "jsdom";
const html = await readFile(
  new URL("../src/ui/index.html", import.meta.url),
  "utf8",
);
const script = await readFile(
  new URL("../src/ui/app.js.txt", import.meta.url),
  "utf8",
);
const settle = () => new Promise((r) => setTimeout(r, 20));
async function setup(
  github = { total: 28, releases: 7, fetchedAt: 1793100000000, stale: false },
) {
  const dom = new JSDOM(html, {
    url: "https://veylo-operations.scrde.workers.dev/admin",
    runScripts: "outside-only",
  });
  const w = dom.window;
  const calls = [];
  const notes = [];
  w.HTMLElement.prototype.scrollIntoView = () => {};
  w.confirm = () => true;
  w.fetch = async (path, options = {}) => {
    const method = options.method ?? "GET";
    const body = options.body ? JSON.parse(options.body) : undefined;
    calls.push({ path, method, body });
    let data = [];
    if (path.endsWith("/overview"))
      data = {
        github,
        received: [{ source: "activity", received_at: 1793100000000 }],
        asOf: 1793100010000,
        today: 12,
        active30: 32,
        first90: 40,
        totalClicks: 51,
        openReports: 1,
        activity: [{ day: "2026-10-25", active: 12 }],
        versions: [{ version: "3.0.3", active: 12 }],
        clicks: [{ day: "2026-10-25", clicks: 51 }],
        controls: {
          analytics_enabled: 1,
          announcements_enabled: 1,
          reports_enabled: 1,
        },
      };
    if (path.endsWith("/announcements") && method === "GET") data = notes;
    if (path.endsWith("/announcements") && method === "POST") {
      const id = "39f93b6c-df8c-4b68-9c73-4baa459591fa";
      notes.push({ ...body, id, revision: 1 });
      data = { id };
    }
    if (path.includes("/announcements/") && method === "PUT") {
      Object.assign(notes[0], body);
      data = { ok: true };
    }
    if (path.endsWith("/unpublish")) {
      notes[0].status = "draft";
      data = { ok: true };
    }
    if (path.endsWith("/releases"))
      data = { items: [], held: [], fetchedAt: Date.now() };
    return { ok: true, json: async () => data };
  };
  w.eval(script);
  await settle();
  return { dom, w, calls, notes, document: w.document };
}
test("dashboard renders reported data and never treats announcement text as HTML", async () => {
  const { dom, w, document: d } = await setup();
  try {
    assert.match(d.getElementById("metrics").textContent, /12/);
    assert.match(
      d.getElementById("metrics").textContent,
      /GitHub APK downloads28/,
    );
    assert.match(
      d.getElementById("collection-status").textContent,
      /App collection: enabled/,
    );
    assert.match(
      d.getElementById("download-source").textContent,
      /Not unique people/,
    );
    w.location.hash = "announcements";
    await settle();
    const input = d.querySelector("input[name=title]");
    input.value = "<img src=x onerror=alert(1)>";
    input.dispatchEvent(new w.Event("input", { bubbles: true }));
    assert.equal(d.getElementById("preview-title").textContent, input.value);
    assert.equal(d.getElementById("preview-title").children.length, 0);
  } finally {
    dom.window.close();
  }
});
test("announcement create, edit and unpublish send complete payloads and preserve the ID", async () => {
  const { dom, w, document: d, calls, notes } = await setup();
  try {
    w.location.hash = "announcements";
    await settle();
    const f = d.getElementById("announcement-form");
    f.elements.title.value = "Maintenance";
    f.elements.body.value = "A service notice for users.";
    f.dispatchEvent(
      new w.SubmitEvent("submit", {
        bubbles: true,
        cancelable: true,
        submitter: f.querySelector("button[value=published]"),
      }),
    );
    await settle();
    const create = calls.find(
      (x) => x.method === "POST" && x.path.endsWith("/announcements"),
    );
    assert.equal(create.body.status, "published");
    assert.equal(create.body.minCode, 0);
    assert.equal(create.body.maxCode, 2147483647);
    assert.equal(create.body.onceOnly, true);
    assert.ok(create.body.endsAt > create.body.startsAt);
    [...d.querySelectorAll("#announcement-list button")]
      .find((x) => x.textContent === "Edit")
      .click();
    await settle();
    assert.equal(f.elements.id.value, notes[0].id);
    f.elements.title.value = "Changed";
    f.dispatchEvent(
      new w.SubmitEvent("submit", {
        bubbles: true,
        cancelable: true,
        submitter: f.querySelector("button[value=draft]"),
      }),
    );
    await settle();
    assert.ok(
      calls.some(
        (x) =>
          x.method === "PUT" &&
          x.path.endsWith("/" + notes[0].id) &&
          x.body.title === "Changed",
      ),
    );
    notes[0].status = "published";
    d.getElementById("refresh").click();
    await settle();
    [...d.querySelectorAll("#announcement-list button")]
      .find((x) => x.textContent === "Unpublish")
      .click();
    await settle();
    assert.ok(
      calls.some((x) => x.path.endsWith("/unpublish") && x.method === "POST"),
    );
  } finally {
    dom.window.close();
  }
});
test("controls submit booleans for every service toggle", async () => {
  const { dom, w, document: d, calls } = await setup();
  try {
    w.location.hash = "controls";
    await settle();
    const f = d.getElementById("controls-form");
    f.elements.analyticsEnabled.checked = false;
    f.elements.reportsEnabled.checked = false;
    f.dispatchEvent(
      new w.SubmitEvent("submit", {
        bubbles: true,
        cancelable: true,
        submitter: f.querySelector("button"),
      }),
    );
    await settle();
    assert.deepEqual(
      calls.find((x) => x.method === "PUT" && x.path.endsWith("/controls"))
        .body,
      {
        analyticsEnabled: false,
        announcementsEnabled: true,
        reportsEnabled: false,
      },
    );
  } finally {
    dom.window.close();
  }
});

test("unavailable and stale GitHub data are visibly different from zero", async () => {
  for (const [github, expected] of [
    [{ total: null, stale: true }, /unavailable/],
    [{ total: 0, releases: 0, fetchedAt: 1, stale: true }, /Cached data/],
  ]) {
    const { dom, document: d } = await setup(github);
    try {
      assert.match(d.getElementById("download-source").textContent, expected);
      assert.match(
        d.getElementById("metrics").textContent,
        github.total == null ? /Unavailable/ : /GitHub APK downloads0/,
      );
    } finally {
      dom.window.close();
    }
  }
});

test("website tracking falls back without blocking downloads and honors privacy signals", async () => {
  const website = await readFile(
    new URL("../../website/index.html", import.meta.url),
    "utf8",
  );
  for (const mode of ["queued", "full", "missing", "throws", "private"]) {
    const dom = new JSDOM(website, {
      url: "https://veylo-lra9.onrender.com/",
      runScripts: "outside-only",
    });
    const w = dom.window;
    let beacons = 0,
      fetches = 0;
    w.fetch = async () => {
      fetches++;
      return { ok: true };
    };
    if (mode !== "missing")
      w.navigator.sendBeacon = () => {
        beacons++;
        if (mode === "throws") throw new Error("queue failed");
        return mode === "queued";
      };
    if (mode === "private")
      Object.defineProperty(w.navigator, "doNotTrack", { value: "1" });
    try {
      const script = [...w.document.scripts].find((s) =>
        s.textContent.includes("/v1/download-click"),
      );
      assert.ok(script);
      w.eval(script.textContent);
      const link = w.document.querySelector(
        'a[href*="/releases/latest/download/"]',
      );
      link.addEventListener("click", (e) => e.preventDefault());
      link.click();
      await settle();
      assert.equal(
        fetches,
        ["full", "missing", "throws"].includes(mode) ? 1 : 0,
      );
      assert.equal(beacons, ["missing", "private"].includes(mode) ? 0 : 1);
    } finally {
      w.close();
    }
  }
});
