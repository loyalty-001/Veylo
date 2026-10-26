import {env} from 'cloudflare:test';
import {beforeAll,beforeEach,describe,expect,it,vi} from 'vitest';
import {createLocalJWKSet,exportJWK,generateKeyPair,SignJWT} from 'jose';
import migration from '../migrations/0001_operations.sql?raw';
import {app} from '../src/application';
import {cleanup} from '../src/public';
const holder=vi.hoisted(()=>({resolver:null as ReturnType<typeof createLocalJWKSet>|null}));
vi.mock('jose',async original=>{const real=await original<typeof import('jose')>();return {...real,createRemoteJWKSet:()=> (...args:Parameters<ReturnType<typeof createLocalJWKSet>>)=>holder.resolver!(...args)};});
let ownerToken:string;let otherToken:string;let wrongAud:string;
const origin='https://veylo-operations.scrde.workers.dev';
const bindings=()=>({...env,ADMIN_READY:'true',ACCESS_AUD:'test-audience',OWNER_EMAIL:'goodlucknosakhare9@gmail.com',ADMIN_ORIGIN:origin});
const auth=(token=ownerToken)=>({'Cf-Access-Jwt-Assertion':token,Origin:origin,'Content-Type':'application/json'});
const req=(path:string,method='GET',data?:unknown,headers?:Record<string,string>)=>app.request(origin+path,{method,headers:headers??auth(),body:data===undefined?undefined:JSON.stringify(data)},bindings());
beforeAll(async()=>{
 await env.DB.exec(migration);
 const {privateKey,publicKey}=await generateKeyPair('RS256');
 holder.resolver=createLocalJWKSet({keys:[{...await exportJWK(publicKey),kid:'test'}]});
 const sign=(email:string,aud='test-audience')=>new SignJWT({email,type:'app'}).setProtectedHeader({alg:'RS256',kid:'test'}).setSubject('test-owner').setIssuer('https://loyaltyz.cloudflareaccess.com').setAudience(aud).setIssuedAt().setExpirationTime('1h').sign(privateKey);
 ownerToken=await sign('goodlucknosakhare9@gmail.com');otherToken=await sign('someone@example.com');wrongAud=await sign('goodlucknosakhare9@gmail.com','another-app');
});
beforeEach(async()=>{await env.DB.exec('DELETE FROM announcements;DELETE FROM daily_installs;DELETE FROM first_launches;DELETE FROM health_counts;DELETE FROM reports;DELETE FROM audit_log;DELETE FROM held_releases;DELETE FROM download_clicks;DELETE FROM release_cache;UPDATE controls SET analytics_enabled=1,announcements_enabled=1,reports_enabled=1;');});
const note=()=>({title:'Service notice',body:'Playback improvements are available.',kind:'popup',status:'published',startsAt:Date.now()-1000,endsAt:Date.now()+3600000,minCode:22,maxCode:50,actionLabel:'',actionUrl:'',onceOnly:true});
describe('owner authorization and request safety',()=>{
 it('keeps admin locked before MFA setup even with a valid token',async()=>{const r=await app.request(origin+'/admin/api/overview',{headers:auth()},{...bindings(),ADMIN_READY:'false'});expect(r.status).toBe(503);});
 it('rejects anonymous, forged, wrong-audience and other-user requests',async()=>{for(const token of ['', 'forged',wrongAud,otherToken]){const r=await req('/admin/api/overview','GET',undefined,auth(token));expect([401,403]).toContain(r.status);}});
 it('permits the signed owner token and makes admin responses private',async()=>{const r=await req('/admin/api/overview');expect(r.status).toBe(200);expect(r.headers.get('cache-control')).toBe('no-store');expect(r.headers.get('content-security-policy')).toContain("frame-ancestors 'none'");});
 it('rejects cross-origin writes and unsupported payload keys',async()=>{expect((await req('/admin/api/announcements','POST',note(),{...auth(),Origin:'https://attacker.example'})).status).toBe(403);expect((await req('/v1/events','POST',{installId:crypto.randomUUID(),version:'3.0.3',code:22,platform:'android',category:'launch',videoUrl:'private'})).status).toBe(400);});
 it('limits request bodies before parsing',async()=>{expect((await req('/v1/reports','POST',{message:'x'.repeat(10000)})).status).toBe(413);});
});
describe('announcement delivery',()=>{
 it('delivers only published, active, version-matched messages and audits changes',async()=>{
  expect((await req('/admin/api/announcements','POST',note())).status).toBe(201);
  await req('/admin/api/announcements','POST',{...note(),status:'draft'});
  await req('/admin/api/announcements','POST',{...note(),startsAt:Date.now()+10000});
  const active=await (await req('/v1/config?code=22')).json() as {announcements:unknown[]};expect(active.announcements).toHaveLength(1);
  expect(((await (await req('/v1/config?code=21')).json()) as {announcements:unknown[]}).announcements).toHaveLength(0);
  expect((await env.DB.prepare('SELECT COUNT(*) n FROM audit_log').first<{n:number}>())?.n).toBe(3);
 });
 it('unpublishes immediately and increments revision on editing',async()=>{
  const {id}=await (await req('/admin/api/announcements','POST',note())).json() as {id:string};
  await req('/admin/api/announcements/'+id,'PUT',{...note(),title:'Revised'});
  expect((await env.DB.prepare('SELECT revision FROM announcements WHERE id=?').bind(id).first<{revision:number}>())?.revision).toBe(2);
  await req('/admin/api/announcements/'+id+'/unpublish','POST',{});
  expect(((await (await req('/v1/config?code=22')).json()) as {announcements:unknown[]}).announcements).toHaveLength(0);
 });
 it('rejects unsafe links, inconsistent targets and invalid dates',async()=>{for(const data of [{actionLabel:'Open',actionUrl:'javascript:alert(1)'},{actionLabel:'Open',actionUrl:'https://github.com/attacker/other'},{maxCode:10},{endsAt:0}])expect((await req('/admin/api/announcements','POST',{...note(),...data})).status).toBe(400);});
});
describe('privacy, reports and controls',()=>{
 it('deduplicates activity by installation/day and forgets opted-out identifiers',async()=>{const id=crypto.randomUUID();const data={installId:id,version:'3.0.3',code:22,platform:'android',category:'launch'};await req('/v1/events','POST',data);await req('/v1/events','POST',data);expect((await env.DB.prepare('SELECT COUNT(*) n FROM daily_installs').first<{n:number}>())?.n).toBe(1);await req('/v1/forget','POST',{installId:id});expect((await env.DB.prepare('SELECT COUNT(*) n FROM daily_installs').first<{n:number}>())?.n).toBe(0);expect((await env.DB.prepare('SELECT COUNT(*) n FROM first_launches').first<{n:number}>())?.n).toBe(0);});
 it('stores reports only when explicitly submitted, supports resolution, and honors pause controls',async()=>{const response=await req('/v1/reports','POST',{category:'casting',message:'The TV appeared but playback did not begin.',version:'3.0.3',code:22,platform:'android'});expect(response.status).toBe(201);const {id}=await response.json() as {id:string};await req('/admin/api/reports/'+id+'/resolve','POST',{});expect((await env.DB.prepare('SELECT status FROM reports WHERE id=?').bind(id).first<{status:string}>())?.status).toBe('resolved');await req('/admin/api/controls','PUT',{analyticsEnabled:false,announcementsEnabled:false,reportsEnabled:false});expect((await req('/v1/reports','POST',{category:'other',message:'A report submitted during a pause.',version:'3.0.3',code:22,platform:'android'})).status).toBe(503);});
 it('tracks download-link requests independently and redirects only to fixed assets',async()=>{const r=await req('/download/universal');expect(r.status).toBe(302);expect(r.headers.get('location')).toBe('https://github.com/loyalty-001/Veylo/releases/latest/download/Veylo.apk');expect((await req('/download/arbitrary')).status).toBe(404);expect((await env.DB.prepare('SELECT count FROM download_clicks').first<{count:number}>())?.count).toBe(1);});
});

function seedReleaseCache(db:D1Database,items:unknown[]){return db.prepare('INSERT INTO release_cache(id,fetched_at,body) VALUES(1,?,?)').bind(Date.now(),JSON.stringify(items)).run();}
it('release holds are audited and exposed only as update-safety tags',async()=>{
 await seedReleaseCache(env.DB,[{tag_name:'v3.0.3',name:'Veylo 3.0.3',published_at:new Date().toISOString(),html_url:'https://github.com/loyalty-001/Veylo/releases/tag/v3.0.3',prerelease:false,draft:false,assets:[]}]);
 expect((await req('/admin/api/releases/hold','POST',{tag:'v3.0.3',held:true})).status).toBe(200);
 const config=await (await req('/v1/config?code=22')).json() as {heldReleases:string[]};expect(config.heldReleases).toEqual(['v3.0.3']);
 expect((await req('/admin/api/releases/hold','POST',{tag:'v9.9.9',held:true})).status).toBe(400);
 await req('/admin/api/releases/hold','POST',{tag:'v3.0.3',held:false});expect((await env.DB.prepare('SELECT COUNT(*) n FROM held_releases').first<{n:number}>())?.n).toBe(0);
});
it('scheduled retention removes old identifiers and private reports while retaining anonymous aggregates',async()=>{
 await seedReleaseCache(env.DB,[]);
 await env.DB.exec("INSERT INTO daily_installs VALUES('2000-01-01','old-id','3.0.3',22,'android');INSERT INTO first_launches VALUES('old-id','2000-01-01','3.0.3');INSERT INTO health_counts VALUES('2000-01-01','3.0.3','cast_error',1);INSERT INTO reports(id,category,message,version,code,platform,created_at) VALUES('old-report','other','Old report text','3.0.3',22,'android',0);INSERT INTO audit_log VALUES('old-audit',0,'owner','test','test');");
 await cleanup(env.DB);
 for(const name of ['daily_installs','first_launches','reports','audit_log'])expect((await env.DB.prepare('SELECT COUNT(*) n FROM '+name).first<{n:number}>())?.n).toBe(0);
 expect((await env.DB.prepare('SELECT count FROM health_counts').first<{count:number}>())?.count).toBe(1);
});
