import {test} from 'node:test';
import assert from 'node:assert/strict';
import {DatabaseSync} from 'node:sqlite';
import {readFileSync} from 'node:fs';
import worker,{reviewLogin,reviewState,handle,sha} from './worker.mjs';
const password='unit-test-long-random-password-not-a-deployment-secret';
const hash=await sha(password);
const time=Date.now();
function makeEnv(){
 const db=new DatabaseSync(':memory:');
 db.exec(readFileSync(new URL('./schema.sql',import.meta.url),'utf8'));
 const binding={prepare(sql){let args=[];return {bind(...values){args=values;return this;},async first(){return db.prepare(sql).get(...args)||null;},async run(){return db.prepare(sql).run(...args);},async all(){return {results:db.prepare(sql).all(...args)};}};}};
 return {DB:binding,REVIEW_PASSWORD_SHA256:hash,REVIEW_SESSION_SECRET:'test-session-secret-with-at-least-32-characters',GOOGLE_WEB_CLIENT_ID:'test.apps.googleusercontent.com',GOOGLE_SERVICE_ACCOUNT_JSON:'{}',sql:db};
}
function request(body={username:'play-review',password}){return new Request('https://worker/v1/review-sign-in',{method:'POST',headers:{'Content-Type':'application/json','CF-Connecting-IP':'192.0.2.1'},body:JSON.stringify(body)});}
test('review login grants reusable real Pro session without Google/Play calls',async()=>{
 const env=makeEnv();const login=await reviewLogin(request(),env,time);
 const state=await reviewState(login.token,env,time);
 assert.equal(state.pro,true);assert.equal(state.reviewSession,true);assert.equal(state.validForMillis,60000);assert.equal(login.expiresInMillis,3600000);
 const again=await reviewLogin(request(),env,time);assert.equal((await reviewState(again.token,env,time)).pro,true);
 const stored=env.sql.prepare('SELECT * FROM review_attempts').get();assert.equal(stored.ip_hash,await sha('192.0.2.1'));assert.ok(!JSON.stringify(stored).includes(password));
});
test('review token expiry is capped and expired token rejected',async()=>{
 const env=makeEnv();const {token}=await reviewLogin(request(),env,time);
 const exp=(Math.floor(time/1000)+3600)*1000;
 assert.equal((await reviewState(token,env,exp-1000)).validForMillis,1000);
 await assert.rejects(reviewState(token,env,exp));
});
test('wrong password and unknown username rejected',async()=>{
 const env=makeEnv();for(const body of [{username:'play-review',password:'bad'},{username:'other',password}])await assert.rejects(reviewLogin(request(body),env,time));
});
test('tampered token and wrong signing key rejected',async()=>{
 const env=makeEnv();const {token}=await reviewLogin(request(),env,time);
 await assert.rejects(reviewState(token.slice(0,-8)+'AAAAAAAA',env,time));
 await assert.rejects(reviewState(token,{...env,REVIEW_SESSION_SECRET:'different-session-key-at-least-thirty-two'},time));
 await assert.rejects(reviewState('pr1.invalid.invalid',env,time));
});
test('review disabled if secrets absent',async()=>{
 await assert.rejects(reviewLogin(request(),{DB:makeEnv().DB},time));
 await assert.rejects(reviewState('pr1.foo.bar',{},time));
});
test('login rate limit is atomic in actual SQLite and resets next window',async()=>{
 const env=makeEnv();for(let i=0;i<20;i++)await reviewLogin(request(),env,time);
 await assert.rejects(reviewLogin(request(),env,time),e=>e.status===429);
 assert.ok((await reviewLogin(request(),env,time+900000)).token);
});
test('oversized body is rejected',async()=>{
 await assert.rejects(reviewLogin(request({username:'play-review',password:'x'.repeat(5000)}),makeEnv(),time),e=>e.status===413);
});
test('HTTP login then state has no purchase or Google dependency',async()=>{
 const env=makeEnv();const login=await handle(request(),env);assert.equal(login.status,200);
 const {token}=await login.json();
 const state=await handle(new Request('https://worker/v1/state',{method:'POST',headers:{Authorization:'Bearer '+token,'Content-Type':'application/json'},body:'{"purchases":[]}'}),env,async()=>{throw Error('must not call Google');});
 assert.equal(state.status,200);assert.equal((await state.json()).pro,true);
});
test('review password is never returned in an error',async()=>{
 const r=await handle(request({username:'play-review',password:'wrong'}),makeEnv());
 assert.equal(r.status,401);assert.deepEqual(await r.json(),{error:'invalid_review_credentials'});
});
test('Cloudflare default export ignores ExecutionContext as network dependency',async()=>{
 const env=makeEnv();const original=globalThis.fetch;
 let called=false;
 globalThis.fetch=async()=>{called=true;return Response.json({keys:[]});};
 const b64=v=>Buffer.from(JSON.stringify(v)).toString('base64url');
 const token=b64({alg:'RS256',kid:'unknown'})+'.'+b64({sub:'user',aud:env.GOOGLE_WEB_CLIENT_ID,iss:'https://accounts.google.com',iat:Math.floor(Date.now()/1000),exp:Math.floor(Date.now()/1000)+60})+'.AAAA';
 try {
  const r=await worker.fetch(new Request('https://worker/v1/state',{method:'POST',headers:{Authorization:'Bearer '+token,'Content-Type':'application/json'},body:'{"purchases":[]}'}),env,{waitUntil(){}});
  assert.equal(r.status,401);assert.equal(called,true);
 }finally{globalThis.fetch=original;}
});
