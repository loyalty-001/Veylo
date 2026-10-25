import {z} from 'zod';
const asset=z.object({name:z.string(),size:z.number(),download_count:z.number(),browser_download_url:z.url()});
const release=z.object({tag_name:z.string(),name:z.string().nullable(),published_at:z.string().nullable(),html_url:z.url(),prerelease:z.boolean(),draft:z.boolean(),assets:z.array(asset)});
export type Release=z.infer<typeof release>;
export async function releases(db:D1Database,refresh=false):Promise<Release[]>{
 const cache=await db.prepare('SELECT fetched_at,body FROM release_cache WHERE id=1').first<{fetched_at:number;body:string}>();
 if(cache&&!refresh&&Date.now()-cache.fetched_at<15*60000)return z.array(release).parse(JSON.parse(cache.body));
 try{const r=await fetch('https://api.github.com/repos/loyalty-001/Veylo/releases?per_page=30',{headers:{Accept:'application/vnd.github+json','User-Agent':'Veylo-Operations'},signal:AbortSignal.timeout(8000)});if(!r.ok)throw new Error('Release source unavailable');if(Number(r.headers.get('Content-Length'))>1000000)throw new Error('Release response too large');const parsed=z.array(release).parse(await r.json()).filter(x=>!x.draft&&!x.prerelease&&/^v\d+\.\d+\.\d+$/.test(x.tag_name));await db.prepare('INSERT INTO release_cache(id,fetched_at,body) VALUES(1,?,?) ON CONFLICT(id) DO UPDATE SET fetched_at=excluded.fetched_at,body=excluded.body').bind(Date.now(),JSON.stringify(parsed)).run();return parsed;}catch(error){if(cache)return z.array(release).parse(JSON.parse(cache.body));throw error;}
}
