import {test} from 'node:test';
import assert from 'node:assert/strict';
import {decide,validatePurchases,identity,state,handle,sha} from './worker.mjs';
const SUB='translator_pro', LIFE='translator_pro_lifetime';
const now=Date.now(), future=new Date(now+3600000).toISOString(), past=new Date(now-60000).toISOString();
const sub=(status='SUBSCRIPTION_STATE_ACTIVE',expiry=future,plan='yearly-auto')=>({subscriptionState:status,acknowledgementState:'ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED',lineItems:[{productId:SUB,expiryTime:expiry,offerDetails:{basePlanId:plan}}]});
for(const status of ['ACTIVE','IN_GRACE_PERIOD','CANCELED']) test('valid '+status,()=>assert.equal(decide(SUB,sub('SUBSCRIPTION_STATE_'+status),now).eligible,true));
for(const status of ['PENDING','ON_HOLD','PAUSED','EXPIRED','PENDING_PURCHASE_CANCELED']) test('deny '+status,()=>assert.equal(decide(SUB,sub('SUBSCRIPTION_STATE_'+status),now).eligible,false));
test('expired canceled denies',()=>assert.equal(decide(SUB,sub('SUBSCRIPTION_STATE_CANCELED',past),now).eligible,false));
test('unknown plan denies',()=>assert.equal(decide(SUB,sub(undefined,future,'unknown'),now).eligible,false));
test('previous prepaid plan remains recognized',()=>assert.equal(decide(SUB,sub(undefined,future,'yearly'),now).eligible,true));
test('malformed expiry denies',()=>assert.equal(decide(SUB,sub(undefined,'not a date'),now).eligible,false));
test('lease capped at exact expiry',()=>assert.equal(decide(SUB,sub(undefined,new Date(now+5000).toISOString()),now).until,now+5000));
test('lifetime refunded pending consumed deny',()=>{
 assert.equal(decide(LIFE,{purchaseState:0,consumptionState:0},now).eligible,true);
 for(const data of [{purchaseState:1,consumptionState:0},{purchaseState:2,consumptionState:0},{purchaseState:0,consumptionState:1}]) assert.equal(decide(LIFE,data,now).eligible,false);
});
test('input validation and deduplication',()=>{
 assert.equal(validatePurchases([{product:SUB,token:'t'},{product:SUB,token:'t'}]).length,1);
 for(const p of [null,{},[{product:'bad',token:'t'}],[{product:SUB,token:''}],Array.from({length:5},(_,i)=>({product:SUB,token:String(i)}))]) assert.throws(()=>validatePurchases(p));
});
const keys=await crypto.subtle.generateKey({name:'RSASSA-PKCS1-v1_5',modulusLength:2048,publicExponent:new Uint8Array([1,0,1]),hash:'SHA-256'},true,['sign','verify']);
const jwk=await crypto.subtle.exportKey('jwk',keys.publicKey);jwk.kid='test-key';jwk.alg='RS256';jwk.use='sig';
const b64=v=>Buffer.from(v).toString('base64url');
async function jwt(overrides={}) {
 const head=b64(JSON.stringify({alg:'RS256',kid:'test-key'}));
 const claims=b64(JSON.stringify({sub:'user-123',aud:'web.apps.googleusercontent.com',iss:'https://accounts.google.com',iat:Math.floor(Date.now()/1000),exp:Math.floor(Date.now()/1000)+3600,...overrides}));
 const payload=head+'.'+claims;
 return payload+'.'+b64(await crypto.subtle.sign('RSASSA-PKCS1-v1_5',keys.privateKey,new TextEncoder().encode(payload)));
}
const certNet=async url=>{assert.equal(url,'https://www.googleapis.com/oauth2/v3/certs');return Response.json({keys:[jwk]});};
test('real RSA identity signature verifies',async()=>assert.equal(await identity(await jwt(), 'web.apps.googleusercontent.com',certNet),await sha('user-123')));
test('wrong audience issuer expired and malformed tokens reject',async()=>{
 for(const change of [{aud:'wrong'},{iss:'evil'},{exp:0},{iat:Date.now()/1000+500},{sub:''}]) await assert.rejects(identity(await jwt(change),'web.apps.googleusercontent.com',certNet));
 await assert.rejects(identity('broken','web.apps.googleusercontent.com',certNet));
});
test('forged signature rejects',async()=>{const t=await jwt();await assert.rejects(identity(t.slice(0,t.lastIndexOf('.')+1)+'AAAA','web.apps.googleusercontent.com',certNet));});
const der=await crypto.subtle.exportKey('pkcs8',keys.privateKey);
const service=JSON.stringify({type:'service_account',client_email:'pala-play-verifier@pala-game-translator.iam.gserviceaccount.com',private_key_id:'test',private_key:'-----BEGIN PRIVATE KEY-----\n'+Buffer.from(der).toString('base64')+'\n-----END PRIVATE KEY-----'});
class DB {
 constructor(approval=null){this.approval=approval;this.owners=new Map();}
 prepare(sql){const db=this;let args=[];return {bind(...a){args=a;return this;},async first(){return sql.includes('approved_migrations')?db.approval:db.owners.get(args[0]);},async run(){if(sql.startsWith('INSERT')&&!db.owners.has(args[0]))db.owners.set(args[0],{account_hash:args[1],product:args[2]});return {success:true};},async all(){return {results:[]};}};}
}
function netFor(data,{ackFail=false,playStatus=200}={}) {
 return async(url,options)=>{
  if(url==='https://oauth2.googleapis.com/token')return Response.json({access_token:'mock-access',expires_in:3600});
  assert.ok(url.startsWith('https://androidpublisher.googleapis.com/'));
  assert.equal(options.headers.Authorization,'Bearer mock-access');
  if(url.endsWith(':acknowledge'))return new Response('',{status:ackFail?500:200});
  return Response.json(data,{status:playStatus});
 };
}
const env=db=>({DB:db,GOOGLE_SERVICE_ACCOUNT_JSON:service,GOOGLE_WEB_CLIENT_ID:'web.apps.googleusercontent.com'});
test('bound active purchase grants short lease',async()=>{
 const d={...sub(),externalAccountIdentifiers:{obfuscatedExternalAccountId:'owner'}};
 const r=await state('owner',[{product:SUB,token:'bound'}],env(new DB()),netFor(d));
 assert.equal(r.pro,true);assert.ok(r.validForMillis>0&&r.validForMillis<=60000);assert.equal(r.records[0].result,'verified');
});
test('refunded/expired stale client token does not grant',async()=>{
 const r=await state('owner',[{product:SUB,token:'stale'}],env(new DB()),netFor(sub('SUBSCRIPTION_STATE_EXPIRED',past)));
 assert.equal(r.pro,false);assert.equal(r.records[0].result,'inactive');
});
test('unbound active purchase requires explicit migration approval',async()=>{
 const r=await state('owner',[{product:SUB,token:'legacy'}],env(new DB()),netFor(sub()));
 assert.equal(r.pro,false);assert.equal(r.migrationRequired,true);
});
test('reviewed migration grants only approved account',async()=>{
 const r=await state('owner',[{product:SUB,token:'legacy'}],env(new DB({account_hash:'owner',product:SUB})),netFor(sub()));
 assert.equal(r.pro,true);
});
test('Google-bound other account cannot be bypassed by migration',async()=>{
 const d={...sub(),externalAccountIdentifiers:{obfuscatedExternalAccountId:'other'}};
 const r=await state('owner',[{product:SUB,token:'other-token'}],env(new DB({account_hash:'owner',product:SUB})),netFor(d));
 assert.equal(r.pro,false);assert.equal(r.records[0].result,'account_mismatch');
});
test('acknowledgement failure never grants',async()=>{
 const d={...sub(),acknowledgementState:'ACKNOWLEDGEMENT_STATE_PENDING',externalAccountIdentifiers:{obfuscatedExternalAccountId:'owner'}};
 await assert.rejects(state('owner',[{product:SUB,token:'ack'}],env(new DB()),netFor(d,{ackFail:true})));
});
test('Google permission failure is an error, not inactive entitlement',async()=>await assert.rejects(state('owner',[{product:SUB,token:'p'}],env(new DB()),netFor({}, {playStatus:403}))));
test('Google 410 stale token no grant',async()=>assert.equal((await state('owner',[{product:SUB,token:'gone'}],env(new DB()),netFor({}, {playStatus:410}))).pro,false));
test('empty ownership does not call Google',async()=>assert.equal((await state('owner',[],env(new DB()),async()=>{throw Error('unexpected');})).pro,false));
test('health verifies schema, not Google permissions',async()=>{
 const r=await handle(new Request('https://worker/health'),env(new DB()));assert.equal(r.status,200);assert.equal((await r.json()).googleApiVerified,false);
});
test('unauthenticated request denied',async()=>{
 const r=await handle(new Request('https://worker/v1/state',{method:'POST',headers:{'Content-Type':'application/json'},body:'{}'}),env(new DB()));assert.equal(r.status,401);
});
