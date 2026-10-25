// PALA Play verifier v1. No credentials belong in this file.
const PACKAGE = 'com.optima.gametranslator';
const SUB = 'translator_pro';
const LIFE = 'translator_pro_lifetime';
const PLAN_IDS = new Set(['monthly', 'yearly', 'yearly-auto']);
const enc = new TextEncoder();
const jwksCache = new Map();
const accessCache = new Map();
class Fault extends Error {
  constructor(status, code) { super(code); this.status = status; this.code = code; }
}
const answer = (data, status = 200) => new Response(JSON.stringify(data), {
  status, headers: {'Content-Type':'application/json; charset=utf-8', 'Cache-Control':'no-store', 'X-Content-Type-Options':'nosniff'}
});
const b64url = bytes => btoa(String.fromCharCode(...new Uint8Array(bytes))).replace(/=/g,'').replace(/\+/g,'-').replace(/\//g,'_');
function unb64(value) {
  if (typeof value !== 'string' || !/^[A-Za-z0-9_-]+$/.test(value)) throw new Fault(401,'invalid_identity');
  const s=value.replace(/-/g,'+').replace(/_/g,'/');
  return Uint8Array.from(atob(s+'='.repeat((4-s.length%4)%4)), c=>c.charCodeAt(0));
}
export async function sha(value) {
  return [...new Uint8Array(await crypto.subtle.digest('SHA-256',enc.encode(value)))].map(b=>b.toString(16).padStart(2,'0')).join('');
}
async function jsonFetch(url, options, net) {
  const r=await net(url,options);
  if (!r.ok) throw new Fault(503,'google_service_unavailable');
  return r.json();
}
export async function identity(token, audience, net=fetch, now=Date.now(), signal) {
  try {
    if (!token || token.length>8192) throw new Fault(401,'invalid_identity');
    const parts=token.split('.');
    if(parts.length!==3) throw new Fault(401,'invalid_identity');
    const head=JSON.parse(new TextDecoder().decode(unb64(parts[0])));
    const claims=JSON.parse(new TextDecoder().decode(unb64(parts[1])));
    if(head.alg!=='RS256'||typeof head.kid!=='string'||head.kid.length>256) throw new Fault(401,'invalid_identity');
    const sec=now/1000;
    if(!['accounts.google.com','https://accounts.google.com'].includes(claims.iss) || claims.aud!==audience ||
       !Number.isFinite(claims.exp)||claims.exp<=sec || !Number.isFinite(claims.iat)||claims.iat>sec+60 ||
       typeof claims.sub!=='string'||!claims.sub||claims.sub.length>255) throw new Fault(401,'invalid_identity');
    let cached=jwksCache.get(net);
    if(!cached||cached.until<now) {
      const data=await jsonFetch('https://www.googleapis.com/oauth2/v3/certs',{signal},net);
      if(!Array.isArray(data.keys)) throw new Fault(503,'identity_keys_unavailable');
      cached={keys:data.keys,until:now+300000};
      jwksCache.set(net,cached);
    }
    // Unknown keys are rejected; cache naturally refreshes within five minutes.
    const jwk=cached.keys.find(k=>k.kid===head.kid&&k.kty==='RSA'&&(!k.alg||k.alg==='RS256')&&(!k.use||k.use==='sig'));
    if(!jwk) throw new Fault(401,'invalid_identity');
    const key=await crypto.subtle.importKey('jwk',jwk,{name:'RSASSA-PKCS1-v1_5',hash:'SHA-256'},false,['verify']);
    if(!await crypto.subtle.verify('RSASSA-PKCS1-v1_5',key,unb64(parts[2]),enc.encode(parts[0]+'.'+parts[1]))) throw new Fault(401,'invalid_identity');
    return sha(claims.sub);
  } catch(e) {
    if(e instanceof Fault) throw e;
    throw new Fault(401,'invalid_identity');
  }
}
async function accessToken(raw,net,now,signal) {
  let account;
  try { account=JSON.parse(raw); } catch { throw new Fault(503,'invalid_server_configuration'); }
  if(account.type!=='service_account'||account.client_email!=='pala-play-verifier@pala-game-translator.iam.gserviceaccount.com'||typeof account.private_key!=='string') throw new Fault(503,'invalid_server_configuration');
  const cacheKey=account.client_email+':'+account.private_key_id;
  const old=accessCache.get(cacheKey);
  if(old&&old.until>now+60000) return old.token;
  const sec=Math.floor(now/1000);
  const header=b64url(enc.encode(JSON.stringify({alg:'RS256',typ:'JWT'})));
  const body=b64url(enc.encode(JSON.stringify({iss:account.client_email,scope:'https://www.googleapis.com/auth/androidpublisher',aud:'https://oauth2.googleapis.com/token',iat:sec,exp:sec+3600})));
  const pem=account.private_key.replace(/-----BEGIN PRIVATE KEY-----|-----END PRIVATE KEY-----|\s/g,'');
  const key=await crypto.subtle.importKey('pkcs8',Uint8Array.from(atob(pem),c=>c.charCodeAt(0)),{name:'RSASSA-PKCS1-v1_5',hash:'SHA-256'},false,['sign']);
  const sig=await crypto.subtle.sign('RSASSA-PKCS1-v1_5',key,enc.encode(header+'.'+body));
  const result=await jsonFetch('https://oauth2.googleapis.com/token',{method:'POST',signal,headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams({grant_type:'urn:ietf:params:oauth:grant-type:jwt-bearer',assertion:header+'.'+body+'.'+b64url(sig)})},net);
  if(typeof result.access_token!=='string'||!Number.isFinite(result.expires_in)) throw new Fault(503,'invalid_google_response');
  accessCache.set(cacheKey,{token:result.access_token,until:now+Math.min(result.expires_in,3600)*1000});
  return result.access_token;
}
export function decide(product,data,now) {
  if(product===SUB) {
    const items=(Array.isArray(data.lineItems)?data.lineItems:[]).filter(i=>i.productId===SUB&&PLAN_IDS.has(i.offerDetails?.basePlanId));
    const ends=items.map(i=>Date.parse(i.expiryTime)).filter(Number.isFinite);
    const end=ends.length?Math.max(...ends):0;
    return {eligible:['SUBSCRIPTION_STATE_ACTIVE','SUBSCRIPTION_STATE_IN_GRACE_PERIOD','SUBSCRIPTION_STATE_CANCELED'].includes(data.subscriptionState)&&end>now,
      until:Math.min(end,now+60000), expiry:end?new Date(end).toISOString():null,
      state:data.subscriptionState||'UNKNOWN', bound:data.externalAccountIdentifiers?.obfuscatedExternalAccountId,
      acknowledged:data.acknowledgementState==='ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED'};
  }
  if(product===LIFE) return {eligible:data.purchaseState===0&&data.consumptionState===0,until:now+60000,expiry:null,
    state:'ONE_TIME_STATE_'+String(data.purchaseState),bound:data.obfuscatedExternalAccountId,acknowledged:data.acknowledgementState===1};
  throw new Fault(400,'unknown_product');
}
export function validatePurchases(purchases) {
  if(!Array.isArray(purchases)||purchases.length>16) throw new Fault(400,'invalid_purchases');
  const found=new Map();
  for(const p of purchases) {
    if(!p||typeof p!=='object'||![SUB,LIFE].includes(p.product)||typeof p.token!=='string'||!p.token||p.token.length>4096) throw new Fault(400,'invalid_purchase');
    if(found.has(p.token)&&found.get(p.token).product!==p.product) throw new Fault(400,'conflicting_product');
    found.set(p.token,{product:p.product,token:p.token});
  }
  if(found.size>4) throw new Fault(400,'too_many_purchases');
  return [...found.values()];
}
async function playGet(product,token,bearer,net,signal) {
  const root=`https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${PACKAGE}/purchases/`;
  const path=product===SUB?`subscriptionsv2/tokens/${encodeURIComponent(token)}`:`products/${product}/tokens/${encodeURIComponent(token)}`;
  const r=await net(root+path,{signal,headers:{Authorization:'Bearer '+bearer}});
  if([404,410].includes(r.status)) return null;
  if(!r.ok) throw new Fault(503,r.status===403?'play_permission_denied':'play_verification_unavailable');
  return r.json();
}
async function acknowledge(product,token,bearer,net,signal) {
  const kind=product===SUB?'subscriptions':'products';
  const url=`https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${PACKAGE}/purchases/${kind}/${product}/tokens/${encodeURIComponent(token)}:acknowledge`;
  const r=await net(url,{method:'POST',signal,headers:{Authorization:'Bearer '+bearer,'Content-Type':'application/json'},body:'{}'});
  if(!r.ok) throw new Fault(503,'acknowledgement_failed');
}
export async function state(account,purchases,env,net=fetch,now=Date.now(),signal) {
  const valid=validatePurchases(purchases);
  let bearer;
  if(valid.length) bearer=await accessToken(env.GOOGLE_SERVICE_ACCOUNT_JSON,net,now,signal);
  let until=now;
  const records=[];
  for(const p of valid) {
    const data=await playGet(p.product,p.token,bearer,net,signal);
    if(!data) { records.push({product:p.product,result:'not_owned'}); continue; }
    const decision=decide(p.product,data,now);
    const hash=await sha(p.token);
    const record={product:p.product,state:decision.state,expiresAt:decision.expiry,purchaseRef:hash.slice(0,16)};
    // Never let a supplied purchase token override a Google account binding.
    if(decision.bound&&decision.bound!==account) {records.push({...record,result:'account_mismatch'});continue;}
    if(!decision.eligible) {records.push({...record,result:'inactive'});continue;}
    if(!decision.bound) {
      const approval=await env.DB.prepare('SELECT account_hash, product FROM approved_migrations WHERE token_hash = ?').bind(hash).first();
      if(!approval||approval.account_hash!==account||approval.product!==p.product) {
        records.push({...record,result:'migration_required'});continue;
      }
    }
    await env.DB.prepare('INSERT OR IGNORE INTO purchase_bindings(token_hash,account_hash,product,created_at) VALUES(?,?,?,?)').bind(hash,account,p.product,Math.floor(now/1000)).run();
    const owner=await env.DB.prepare('SELECT account_hash,product FROM purchase_bindings WHERE token_hash = ?').bind(hash).first();
    if(!owner||owner.account_hash!==account||owner.product!==p.product) throw new Fault(403,'purchase_account_mismatch');
    if(!decision.acknowledged) await acknowledge(p.product,p.token,bearer,net,signal);
    until=Math.max(until,decision.until);
    records.push({...record,result:'verified'});
  }
  // Recompute after Google round trips, so the lease never extends past real expiry.
  const finished=Math.max(now,Date.now());
  const remaining=Math.max(0,Math.min(60000,until-finished));
  return {pro:remaining>0,validForMillis:remaining,trialUsedMillis:0,records,
    migrationRequired:records.some(r=>r.result==='migration_required'),checkedAt:new Date(finished).toISOString()};
}

function reviewConfigured(env) {
  return /^[a-f0-9]{64}$/.test(env.REVIEW_PASSWORD_SHA256 || '') &&
    typeof env.REVIEW_SESSION_SECRET === 'string' && env.REVIEW_SESSION_SECRET.length >= 32;
}
function constantEqual(a,b) {
  if(a.length!==b.length) return false;
  let diff=0; for(let i=0;i<a.length;i++) diff|=a.charCodeAt(i)^b.charCodeAt(i);
  return diff===0;
}
async function reviewKey(env,usage) {
  if(!reviewConfigured(env)) throw new Fault(503,'review_unavailable');
  return crypto.subtle.importKey('raw',enc.encode(env.REVIEW_SESSION_SECRET),{name:'HMAC',hash:'SHA-256'},false,[usage]);
}
async function bodyJson(request,maximum) {
  const reader=request.body?.getReader();
  if(!reader) throw new Fault(400,'invalid_body');
  const chunks=[];let size=0;
  while(true) {
    const {done,value}=await reader.read();if(done)break;
    size+=value.length;
    if(size>maximum){await reader.cancel();throw new Fault(413,'body_too_large');}
    chunks.push(value);
  }
  const data=new Uint8Array(size);let position=0;
  for(const c of chunks){data.set(c,position);position+=c.length;}
  try { return JSON.parse(new TextDecoder('utf-8',{fatal:true}).decode(data)); }
  catch {throw new Fault(400,'invalid_json');}
}
export async function reviewLogin(request,env,now=Date.now()) {
  if(!reviewConfigured(env)||!env.DB) throw new Fault(503,'review_unavailable');
  if(!request.headers.get('Content-Type')?.toLowerCase().startsWith('application/json')) throw new Fault(415,'json_required');
  const body=await bodyJson(request,4096);
  if(!body||typeof body.username!=='string'||typeof body.password!=='string'||body.username.length>100||body.password.length>256) throw new Fault(400,'invalid_body');
  // Only the trusted Cloudflare connection IP is used, never X-Forwarded-For.
  const ipHash=await sha(request.headers.get('CF-Connecting-IP')||'unknown');
  const window=Math.floor(now/900000);
  const row=await env.DB.prepare(`INSERT INTO review_attempts(ip_hash,window,attempts) VALUES(?,?,1)
    ON CONFLICT(ip_hash) DO UPDATE SET window=excluded.window,
    attempts=CASE WHEN review_attempts.window=excluded.window THEN review_attempts.attempts+1 ELSE 1 END
    RETURNING attempts`).bind(ipHash,window).first();
  if(!row||row.attempts>20) throw new Fault(429,'review_rate_limited');
  await env.DB.prepare('DELETE FROM review_attempts WHERE window < ?').bind(window-1).run();
  const passwordHash=await sha(body.password);
  if(body.username!=='play-review'||!constantEqual(passwordHash,env.REVIEW_PASSWORD_SHA256)) throw new Fault(401,'invalid_review_credentials');
  const sec=Math.floor(now/1000);
  const payload=b64url(enc.encode(JSON.stringify({scope:'play-review',iat:sec,exp:sec+3600})));
  const mac=await crypto.subtle.sign('HMAC',await reviewKey(env,'sign'),enc.encode('pr1.'+payload));
  return {token:'pr1.'+payload+'.'+b64url(mac),expiresInMillis:3600000};
}
export async function reviewState(token,env,now=Date.now()) {
  if(!reviewConfigured(env)) throw new Fault(401,'invalid_identity');
  try {
    const parts=token.split('.');
    if(parts.length!==3||parts[0]!=='pr1'||token.length>2048) throw new Error();
    const valid=await crypto.subtle.verify('HMAC',await reviewKey(env,'verify'),unb64(parts[2]),enc.encode(parts[0]+'.'+parts[1]));
    if(!valid) throw new Error();
    const claims=JSON.parse(new TextDecoder().decode(unb64(parts[1])));
    const sec=now/1000;
    if(claims.scope!=='play-review'||!Number.isFinite(claims.exp)||claims.exp<=sec||
      !Number.isFinite(claims.iat)||claims.iat>sec+60||claims.exp-claims.iat!==3600) throw new Error();
    return {pro:true,validForMillis:Math.max(0,Math.min(60000,Math.floor(claims.exp*1000-now))),trialUsedMillis:0,
      reviewSession:true,migrationRequired:false,records:[{product:'review-access',state:'REVIEW_ACCESS',result:'verified'}],checkedAt:new Date(now).toISOString()};
  } catch { throw new Fault(401,'invalid_identity'); }
}

export async function handle(request,env,net=fetch) {
  const path=new URL(request.url).pathname;
  try {
    if(path==='/health'&&request.method==='GET') {
      const config=Boolean(env.GOOGLE_WEB_CLIENT_ID?.endsWith('.apps.googleusercontent.com')&&env.GOOGLE_SERVICE_ACCOUNT_JSON&&env.DB);
      if(!config) return answer({ok:false,version:'pala-play-worker-2',error:'configuration_incomplete'},503);
      await env.DB.prepare('SELECT token_hash FROM purchase_bindings LIMIT 1').all();
      await env.DB.prepare('SELECT token_hash FROM approved_migrations LIMIT 1').all();
      if(reviewConfigured(env)) await env.DB.prepare('SELECT ip_hash FROM review_attempts LIMIT 1').all();
      return answer({ok:true,version:'pala-play-worker-2',database:true,googleApiVerified:false,reviewConfigured:reviewConfigured(env)});
    }
    if(path==='/v1/review-sign-in') {
      if(request.method!=='POST') return answer({error:'method_not_allowed'},405);
      return answer(await reviewLogin(request,env));
    }
    if(path!=='/v1/state') return answer({error:'not_found'},404);
    if(request.method!=='POST') return answer({error:'method_not_allowed'},405);
    if(!env.DB||!env.GOOGLE_WEB_CLIENT_ID||!env.GOOGLE_SERVICE_ACCOUNT_JSON) throw new Fault(503,'configuration_incomplete');
    if(!request.headers.get('Content-Type')?.toLowerCase().startsWith('application/json')) throw new Fault(415,'json_required');
    const auth=request.headers.get('Authorization')||'';
    if(!auth.startsWith('Bearer ')||auth.length>8200) throw new Fault(401,'sign_in_required');
    if(auth.slice(7).startsWith('pr1.')) return answer(await reviewState(auth.slice(7),env));
    const signal=AbortSignal.timeout(15000);
    const account=await identity(auth.slice(7),env.GOOGLE_WEB_CLIENT_ID,net,Date.now(),signal);
    const reader=request.body?.getReader();
    if(!reader) throw new Fault(400,'invalid_body');
    const chunks=[];let size=0;
    while(true) { const {done,value}=await reader.read();if(done)break;size+=value.length;if(size>65536){await reader.cancel();throw new Fault(413,'body_too_large');}chunks.push(value); }
    const bytes=new Uint8Array(size);let offset=0;for(const c of chunks){bytes.set(c,offset);offset+=c.length;}
    let body;
    try { body=JSON.parse(new TextDecoder('utf-8',{fatal:true}).decode(bytes)); } catch {throw new Fault(400,'invalid_json');}
    if(!body||Array.isArray(body)||typeof body!=='object'||!('purchases' in body)) throw new Fault(400,'invalid_body');
    return answer(await state(account,body.purchases,env,net,Date.now(),signal));
  } catch(e) {
    // Never return or log Google responses, tokens, keys or exception text.
    return answer({error:e instanceof Fault?e.code:'verification_unavailable'},e instanceof Fault?e.status:503);
  }
}
export default {
  async fetch(request, env, ctx) {
    return handle(request, env);
  }
};
