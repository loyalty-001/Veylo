import {Hono} from 'hono';
import {z} from 'zod';
import {announcementSchema,controlsSchema,holdSchema,wireAnnouncement,type Announcement} from './schema';
import {releases} from './releases';
import type {AppEnv} from './security';
export const adminApi=new Hono<AppEnv>();
function audit(db:D1Database,actor:string,action:string,target:string){return db.prepare('INSERT INTO audit_log(id,at,actor,action,target) VALUES(?,?,?,?,?)').bind(crypto.randomUUID(),Date.now(),actor,action,target);}
adminApi.get('/overview',async c=>{
 const db=c.env.DB;const today=new Date().toISOString().slice(0,10);
 const results=await db.batch<Record<string, string | number>>([
  db.prepare('SELECT COUNT(*) total FROM daily_installs WHERE day=?').bind(today),
  db.prepare('SELECT COUNT(DISTINCT install_id) total FROM daily_installs'),
  db.prepare('SELECT COUNT(*) total FROM first_launches'),
  db.prepare("SELECT COUNT(*) total FROM reports WHERE status='open'"),
  db.prepare('SELECT day,COUNT(*) active FROM daily_installs GROUP BY day ORDER BY day DESC LIMIT 30'),
  db.prepare('SELECT version,COUNT(*) active FROM daily_installs WHERE day=? GROUP BY version ORDER BY active DESC').bind(today),
  db.prepare('SELECT day,SUM(count) clicks FROM download_clicks GROUP BY day ORDER BY day DESC LIMIT 30'),
  db.prepare('SELECT COALESCE(SUM(count),0) total FROM download_clicks'),
  db.prepare('SELECT * FROM controls WHERE id=1'),
 ]);
 return c.json({today:results[0].results[0]?.total??0,active30:results[1].results[0]?.total??0,first90:results[2].results[0]?.total??0,openReports:results[3].results[0]?.total??0,activity:results[4].results,versions:results[5].results,clicks:results[6].results,totalClicks:results[7].results[0]?.total??0,controls:results[8].results[0],asOf:Date.now()});
});
adminApi.get('/health',async c=>c.json((await c.env.DB.prepare('SELECT day,version,category,count FROM health_counts ORDER BY day DESC LIMIT 300').all()).results));
adminApi.get('/announcements',async c=>c.json((await c.env.DB.prepare('SELECT * FROM announcements ORDER BY updated_at DESC LIMIT 100').all<Announcement>()).results.map(x=>({...wireAnnouncement(x),status:x.status,updatedAt:x.updated_at}))));
adminApi.post('/announcements',async c=>{
 const a=announcementSchema.parse(await c.req.json());const id=crypto.randomUUID();const db=c.env.DB;
 await db.batch([db.prepare('INSERT INTO announcements(id,title,body,kind,status,starts_at,ends_at,min_code,max_code,action_label,action_url,once_only,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)').bind(id,a.title,a.body,a.kind,a.status,a.startsAt,a.endsAt,a.minCode,a.maxCode,a.actionLabel,a.actionUrl,Number(a.onceOnly),Date.now()),audit(db,c.get('actor'),'announcement.create',id)]);
 return c.json({id},201);
});
adminApi.put('/announcements/:id',async c=>{
 const id=z.uuid().parse(c.req.param('id'));const a=announcementSchema.parse(await c.req.json());const db=c.env.DB;
 if(!await db.prepare('SELECT id FROM announcements WHERE id=?').bind(id).first())return c.notFound();
 await db.batch([db.prepare('UPDATE announcements SET revision=revision+1,title=?,body=?,kind=?,status=?,starts_at=?,ends_at=?,min_code=?,max_code=?,action_label=?,action_url=?,once_only=?,updated_at=? WHERE id=?').bind(a.title,a.body,a.kind,a.status,a.startsAt,a.endsAt,a.minCode,a.maxCode,a.actionLabel,a.actionUrl,Number(a.onceOnly),Date.now(),id),audit(db,c.get('actor'),'announcement.update',id)]);
 return c.json({id});
});
adminApi.post('/announcements/:id/unpublish',async c=>{
 const id=z.uuid().parse(c.req.param('id'));const db=c.env.DB;
 await db.batch([db.prepare("UPDATE announcements SET status='draft',updated_at=? WHERE id=?").bind(Date.now(),id),audit(db,c.get('actor'),'announcement.unpublish',id)]);return c.json({ok:true});
});
adminApi.get('/reports',async c=>c.json((await c.env.DB.prepare('SELECT * FROM reports ORDER BY created_at DESC LIMIT 200').all()).results));
adminApi.post('/reports/:id/resolve',async c=>{
 const id=z.uuid().parse(c.req.param('id'));const db=c.env.DB;
 await db.batch([db.prepare("UPDATE reports SET status='resolved' WHERE id=?").bind(id),audit(db,c.get('actor'),'report.resolve',id)]);return c.json({ok:true});
});
adminApi.get('/releases',async c=>{
 const items=await releases(c.env.DB);const held=(await c.env.DB.prepare('SELECT tag FROM held_releases').all<{tag:string}>()).results.map(x=>x.tag);const cached=await c.env.DB.prepare('SELECT fetched_at FROM release_cache WHERE id=1').first();
 return c.json({items,held,fetchedAt:cached?.fetched_at});
});
adminApi.post('/releases/hold',async c=>{
 const h=holdSchema.parse(await c.req.json());if(!(await releases(c.env.DB)).some(r=>r.tag_name===h.tag))return c.json({error:'Unknown release.'},400);const db=c.env.DB;
 await db.batch([h.held?db.prepare('INSERT OR IGNORE INTO held_releases(tag,held_at) VALUES(?,?)').bind(h.tag,Date.now()):db.prepare('DELETE FROM held_releases WHERE tag=?').bind(h.tag),audit(db,c.get('actor'),h.held?'release.hold':'release.resume',h.tag)]);return c.json({ok:true});
});
adminApi.put('/controls',async c=>{
 const s=controlsSchema.parse(await c.req.json());const db=c.env.DB;
 await db.batch([db.prepare('UPDATE controls SET analytics_enabled=?,announcements_enabled=?,reports_enabled=? WHERE id=1').bind(Number(s.analyticsEnabled),Number(s.announcementsEnabled),Number(s.reportsEnabled)),audit(db,c.get('actor'),'controls.update',JSON.stringify(s))]);return c.json({ok:true});
});
adminApi.get('/audit',async c=>c.json((await c.env.DB.prepare('SELECT * FROM audit_log ORDER BY at DESC LIMIT 200').all()).results));
